package th.co.sic.app.nvtm.settings;

import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import th.co.sic.app.nvtm.settings.messages.commands.Command;
import th.co.sic.app.nvtm.settings.messages.commands.GetConfigCommand;
import th.co.sic.app.nvtm.settings.messages.responses.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class CommunicationThread extends Thread {
    private static Handler myHandler = null;
    private static Handler guiHandler;
    private static Tag tag = null;
    private static Ndef ndef = null;

    CommunicationThread(Handler handler) {
        super();
        guiHandler = handler;
    }

    private static List<Response> writeReadNdef(byte[] payload, boolean cached) {
        List<Response> responses = null;
        try {
            ndef.connect();
            try {
                writeNdef(payload);
                int tries = 10;
                while ((tries > 0) && ((responses == null) || (responses.size() == 0))) {
                    try { /* Give the NHS31xx some time to respond. */
                        Thread.sleep(99);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                    tries--;
                    responses = readNdef(cached);
                    cached = false; /* When re-trying, do not check the same cached message again. */
                }
            } finally {
                ndef.close();
            }
        } catch (NullPointerException | IOException | IllegalStateException e) {
            // java.lang.NullPointerException can occasionally occur. From crash reports:
            // "Attempt to invoke virtual method 'boolean android.nfc.tech.Ndef.isConnected()' on a null object reference"
            // "Attempt to invoke virtual method 'void android.nfc.tech.Ndef.close()' on a null object reference"
            Log.e("writeReadNdef", e.toString());
        }
        return responses;
    }

    private static void writeNdef(byte[] payload) throws IOException {
        NdefRecord record = android.nfc.NdefRecord.createMime("n/p", payload);
        NdefMessage message = new NdefMessage(new NdefRecord[]{record});
        try {
            Log.d("writeNdef", message.toString());
            ndef.writeNdefMessage(message);
        } catch (FormatException e) {
            Log.e("writeNdef", e.toString());
        }
    }

    private static List<Response> readNdef(boolean cached) throws IOException {
        List<Response> responses = new ArrayList<>();
        try {
            NdefMessage ndefMessage;
            if (cached) {
                ndefMessage = ndef.getCachedNdefMessage();
            } else {
                ndefMessage = ndef.getNdefMessage();
            }
            if (ndefMessage != null) {
                for (NdefRecord r : ndefMessage.getRecords()) {
                    byte[] payload = r.getPayload();
                    if (r.getTnf() != 2) {
                        Log.d("readNdef", "Skipping a non-mime record");
                    } else if ((payload == null) || (payload.length < 2)) {
                        Log.i("readNdef", "Empty record or record too short. Ignored.");
                    } else if (payload[1] != 1) {
                        Log.d("readNdef", "Tag byte not 1. Did we read back our own command?");
                    } else {
                        Response response = Response.decode(payload);
                        if (response != null) {
                            Log.d("readNdef", response.toString());
                            responses.add(response);
                        }
                    }
                }
            }
        } catch (FormatException e) {
            Log.i("readNdef", e.toString());
        }
        return responses;
    }

    private static void report(Object obj) {
        Message response = Message.obtain();
        response.obj = obj;
        guiHandler.sendMessage(response);
    }

    @Override
    public void run() {
        Looper.prepare();
        myHandler = new WorkerThreadHandler();
        Looper.loop();
    }

    Handler getHandler() {
        return myHandler;
    }

    /**
     * Uses the low-level NXP proprietary GET_VERSION command to check which tag is tapped.
     *
     * @param mifareUltralight instance of an #MifareUltralight tag.
     * @return @c true when the tag is an NTAG SmartSensor.
     * @pre mifareUltralight.isConnected() returns @c true
     */
    private static boolean CheckType(MifareUltralight mifareUltralight) throws IOException {
        /* From the HW documentation:
         * The GET_VERSION command [60h] is used to retrieve information on the MIFARE family, product
         * version, storage size and other product data required to identify the product. This command is
         * available on other MIFARE products to have a common way of identifying products across platforms
         * and evolution steps. The GET_VERSION command has no arguments and replies the version
         * information for the specific type.
         * GET_VERSION response for NHS devices:
         * 0	Fixed header	00h
         * 1	Vendor ID	04h	NXP Semiconductors
         * 2	Product type	04h	NTAG
         * 3	Product subtype	06h	NHS
         * 4	Major product version	00h
         * 5	Minor product version	00h
         * 6	Size	13h
         * 7	Protocol type	03h	ISO/IEC 14443-3 compliant
         */
        final byte NFC_GET_VERSION = (byte) 0x60;
        byte[] bytes = mifareUltralight.transceive(new byte[]{NFC_GET_VERSION});
        return (bytes != null)
                && (bytes.length == 8)
                && (bytes[0] == 0x00)
                && (bytes[1] == 0x04)
                && (bytes[2] == 0x04)
                && (bytes[3] == 0x06)
                && (bytes[7] == 0x03);
    }

    /**
     * Checks whether pages in the 'static' region are locked for writing by the PCD.
     *
     * @param mifareUltralight instance of an #MifareUltralight tag.
     * @return @c true when no static lock bits have been set and when either the static block lock bits were already
     * set or could be set during this call.
     * @pre mifareUltralight.isConnected() returns @c true
     */
    private static boolean CheckStaticLockBits(MifareUltralight mifareUltralight) throws IOException {
        /* From the HW documentation:
         * The bits of byte 2 and byte 3 of page 02h represent the field programmable read-only locking
         * mechanism. Each page from 03h (Capability Container) to 0Fh can be individually locked by setting
         * the corresponding locking bit Lx to logic 1 to prevent further write access. After locking the
         * corresponding page becomes read-only memory. The three least significant bits of lock byte 0 are
         * the block-locking bits. Bit 2 deals with pages 0Ah to 0Fh, bit 1 deals with pages 04h to 09h and
         * bit 0 deals with page 03h (Capability Container). Once the block locking bits are set, the
         * locking configuration for the corresponding memory area is frozen.
         */
        final int STATIC_LOCK_BITS_PAGE = 0x02;
        boolean ok = false;
        byte[] rx = mifareUltralight.readPages(STATIC_LOCK_BITS_PAGE);
        if ((rx != null) && (rx.length == 4 * 4)) { /* MIFARE Ultralight compatible NFC tags are read 4 pages at a time. */
            if (((rx[2] & 0xF8) == 0x00) && (rx[3] == 0x00)) {
                /* All 'static' pages can be written to, check if the block lock bits are set. */
                if ((rx[2] & 0x07) != 0x07) {
                    byte[] tx = new byte[4];
                    System.arraycopy(rx, 0, tx, 0, 4);
                    tx[2] = 0x07;
                    mifareUltralight.writePage(STATIC_LOCK_BITS_PAGE, tx);
                }
                ok = true;
            }
        }
        return ok;
    }

    /**
     * Checks whether pages in the 'dynamic' region are locked for writing by the PCD.
     *
     * @param mifareUltralight instance of an #MifareUltralight tag.
     * @return @c true when no dynamic lock bits have been set and when either the dynamic block lock bits were already
     * set or could be set during this call.
     * @pre mifareUltralight.isConnected() returns @c true
     */
    private static boolean CheckDynamicLockBits(MifareUltralight mifareUltralight) throws IOException {
        /* From the HW documentation:
         * To lock the pages starting a page address 10h (16d) onwards, the lock bytes 2 to 4 located in
         * page E2h (226d) are used. [...] The granularity is 16 pages [...]. When a lock bit is set to one,
         * the corresponding page range is made read only. The block lock (BL) bits protect the lock bits.
         * Each block lock bit protects two lock bits. When the block lock bit is set, the corresponding
         * lock bits are read only. Block lock and lock bits are one time programmable (OTP). [...]
         * The dynamic lock page is protected against corruption by a valid flag. The valid flag has to
         * be set to 0xbd.
         */
        final int DYNAMIC_LOCK_BITS_PAGE = 0xE2;
        boolean ok = false;
        byte[] rx = mifareUltralight.readPages(DYNAMIC_LOCK_BITS_PAGE);
        if ((rx != null) && (rx.length == 4 * 4)) { /* MIFARE Ultralight compatible NFC tags are read 4 pages at a time. */
            if ((rx[0] == 0x00) && ((rx[1] & 0x3F) == 0x00)) {
                /* All 'dynamic' pages can be written to, check if the block lock bits are set. */
                if ((rx[2] & 0x7F) != 0x7F) {
                    byte[] tx = new byte[4];
                    System.arraycopy(rx, 0, tx, 0, 4);
                    tx[2] = 0x7F;
                    mifareUltralight.writePage(DYNAMIC_LOCK_BITS_PAGE, tx);
                }
                ok = true;
            }
        }
        return ok;
    }

    /**
     * Checks whether pages are locked. If no pages are locked, checks if the locking of pages is disabled. If this is
     * still enabled, writes the so called 'block lock bits' to disable this feature.
     * Thew report function will be used to signify when either a non - NVTM has been detected, or when at
     * least one page has been locked for writing by the PCD (Android phone).
     *
     * @return - @c true when communication can continue: the tag is an NVTM and all pages are available for
     * writing
     * - @c false when either the IC is not an NVTM, when some or all pages are locked for writing, or when
     * communication for some reason failed.
     * .
     */
    private static boolean BlockLockBits() {
        boolean ok = false;
        MifareUltralight mifareUltralight = MifareUltralight.get(tag);
        try {
            mifareUltralight.connect();
            if (mifareUltralight.isConnected()) {
                try {
                    ok = CheckType(mifareUltralight);
                    if (ok) {
                        ok = CheckDynamicLockBits(mifareUltralight) && CheckStaticLockBits(mifareUltralight);
                        if (!ok) {
                            report(R.string.lockBitsSet_message);
                        }
                    } else {
                        report(R.string.noNtagSmartSensor_message);
                    }
                } finally {
                    mifareUltralight.close();
                }
            }
        } catch (NullPointerException | IOException | IllegalStateException e) {
            Log.e("BlockLockBits", e.toString());
            ok = true; /* Avoid wrong notifications in case the NFC tag was badly placed or moved during this check. */
        }
        return ok;
    }

    private static class WorkerThreadHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            if (((ndef == null) || (message.obj == null)) && !(message.obj instanceof Tag)) {
                return; // Do nothing.
            }
            try {
                if (message.obj instanceof Tag) {
                    tag = (Tag) (message.obj);
                    ndef = Ndef.get(tag);
                    if (BlockLockBits()) {
                        if (ndef == null) {
                            report(R.string.noNdefFound_message);
                        } else {
                            List<Response> responses = writeReadNdef(null, true);
//                            if ((responses == null) || (responses.size() == 0)) {
//                                GetConfigCommand getConfigCommand = new GetConfigCommand();
//                                responses = writeReadNdef(getConfigCommand.encode(), true);
//                            }
//                            if ((responses == null) || (responses.size() == 0)) {
//                                report(R.string.noNdefFound_message);
//                            } else {
//                                for (Response r : responses) {
//                                    Log.d("response", r.getId().toString());
//                                    report(r);
//                                }
//                            }
                        }
                    }
                } else if (message.obj instanceof Command) {
                    Command command = (Command) message.obj;
                    Log.d("command", command.getId().toString());
                    for (Response r : writeReadNdef(command.encode(), false)) {
                        Log.d("response", r.getId().toString());
                        report(r);
                    }
                } else if (message.obj instanceof Boolean) {
                    if (!ndef.isConnected()) {
                        /* Check if still present. */
                        ndef.connect();
                        ndef.close();
                    }
                }
            } catch (NullPointerException | IOException e) {
                report(null);
                tag = null;
                ndef = null;
            }
        }
    }
}
