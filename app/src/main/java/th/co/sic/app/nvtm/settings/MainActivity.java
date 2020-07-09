package th.co.sic.app.nvtm.settings;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Vibrator;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;

import th.co.sic.app.nvtm.settings.fragments.ConfigFragment;
import th.co.sic.app.nvtm.settings.utils.MyPagerAdapter;
import th.co.sic.app.nvtm.settings.utils.Util;
import th.co.sic.app.nvtm.settings.messages.commands.Command;
import th.co.sic.app.nvtm.settings.messages.commands.GetConfigCommand;
import th.co.sic.app.nvtm.settings.messages.commands.SetConfigCommand;
import th.co.sic.app.nvtm.settings.messages.responses.GetConfigResponse;
import th.co.sic.app.nvtm.settings.messages.responses.Response;
import th.co.sic.app.nvtm.settings.messages.responses.SetConfigResponse;

public class MainActivity extends FragmentActivity {
    public static boolean isConnected = false;
    public String lastUsedTagId = null;
    public GetConfigResponse lastGetConfigResponse = null;
    private boolean goingToIdle = false;

    private NfcAdapter nfcAdapter = null;
    private PendingIntent pendingIntent = null;

    private CommunicationThread communicationThread = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MyPagerAdapter myPagerAdapter = new MyPagerAdapter(getSupportFragmentManager(), this);
        ViewPager viewPager = findViewById(R.id.pager);
        viewPager.setAdapter(myPagerAdapter);

        Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                if (message.obj == null) {
                    disconnect();
                    showMessage(R.string.connectionLost_message, Snackbar.LENGTH_LONG);
                } else if (message.obj instanceof Integer) {
                    disconnect();
                    showMessage(getResources().getString((Integer) message.obj), Snackbar.LENGTH_INDEFINITE);
                } else if (message.obj instanceof th.co.sic.app.nvtm.settings.messages.Message.Id) {
                    Message communicationMessage;
                    communicationMessage = Message.obtain();
                    communicationMessage.obj = createCommand((th.co.sic.app.nvtm.settings.messages.Message.Id) (message.obj));
                    (communicationThread.getHandler()).sendMessage(communicationMessage);
                } else if (message.obj instanceof Boolean) {
                    Message communicationMessage;
                    communicationMessage = Message.obtain();
                    communicationMessage.obj = message.obj;
                    (communicationThread.getHandler()).sendMessage(communicationMessage);
                } else if (message.obj instanceof Response) {
                    handleResponse((Response) message.obj);
                } else {
                    showMessage(message.obj.toString());
                }
            }
        };
        communicationThread = new CommunicationThread(handler);
        communicationThread.setPriority(Thread.NORM_PRIORITY - 1);
        communicationThread.start();

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        restoreInstanceState(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        nfcAdapter.disableForegroundDispatch(this);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //
    // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //
    // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);

        lastGetConfigResponse = null;

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null) {
            String tagId = Util.bytesToHexString(tag.getId(), ':');
            Message communicationMessage;
            communicationMessage = Message.obtain();
            communicationMessage.obj = tag;
            (communicationThread.getHandler()).sendMessage(communicationMessage);

            if ((lastUsedTagId == null) || (!lastUsedTagId.equals(tagId))) {
                lastUsedTagId = tagId;
            }

            Vibrator vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            long[] duration = {15, 30, 60, 90};
            assert vib != null;
            vib.vibrate(duration, -1);
            showMessage(String.format(getString(R.string.tagFound_message), lastUsedTagId));
            isConnected = true;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    assert imm != null;
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private void disconnect() {
        try {
            getConfigFragment().connected(false);
        } catch (IllegalArgumentException e) {
            // absorb
        }
        isConnected = false;
    }

    // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //
    // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //
    // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //

    public void showMessage(String message) {
        showMessage(message, Snackbar.LENGTH_SHORT);
    }

    private void showMessage(int resourceId) {
        showMessage(resourceId, Snackbar.LENGTH_SHORT);
    }

    private void showMessage(int resourceId, int displayLength) {
        showMessage(getResources().getString(resourceId), displayLength);
    }

    private void showMessage(String message, int displayLength) {
        View view = getWindow().getDecorView().getRootView();
        final Snackbar snackbar = Snackbar.make(view, message + "\n", displayLength);
        TextView textView = snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        textView.setBackgroundResource(R.color.deepBlue);
        textView.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
        textView.setLines(4);
        if (displayLength == Snackbar.LENGTH_INDEFINITE) {
            snackbar.setAction("x", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    snackbar.dismiss();
                }
            });
        }
        snackbar.show();
    }

    // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //
    // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //
    // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //

    private ConfigFragment getConfigFragment() throws IllegalArgumentException {
        ConfigFragment configFragment = null;
        FragmentManager fragmentManager = this.getSupportFragmentManager();
        List<Fragment> fragments = fragmentManager.getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                if ((fragment instanceof ConfigFragment)) {
                    configFragment = (ConfigFragment) fragment;
                }
            }
            if (configFragment == null) {
                FragmentTransaction transaction = this.getSupportFragmentManager().beginTransaction();
                configFragment = new ConfigFragment();
                if (fragments.size() == 0) {
                    transaction.add(0, configFragment);
                } else {
                    transaction.replace(0, configFragment);
                }
                transaction.addToBackStack(null);
                transaction.commit();
            }
        }
        return configFragment;
    }

    // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //
    // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //
    // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //

    public void onEnableConfigSwitchClicked(View view) {
        ConfigFragment configFragment = getConfigFragment();
        if (configFragment != null) {
            configFragment.update(view, ((Switch) view).isChecked());
        }
    }

    public void onDateConfigTextViewClicked(View view) {
        ConfigFragment configFragment = getConfigFragment();
        if (configFragment != null) {
            configFragment.datePickerPopup(view);
        }
    }

    public void onTimeConfigTextViewClicked(View view) {
        ConfigFragment configFragment = getConfigFragment();
        if (configFragment != null) {
            configFragment.timePickerPopup(view);
        }
    }

    public void onWakeupTimeConfigTextViewClicked(View view) {
        ConfigFragment configFragment = getConfigFragment();
        if (configFragment != null) {
            configFragment.numberPickerPopup(view);
        }
    }

    public void onUpperThresholdConfigTextViewClicked(View view) {
        ConfigFragment configFragment = getConfigFragment();
        if (configFragment != null) {
            configFragment.numberPickerPopup(view);
        }
    }

    public void onLowerThresholdConfigTextViewClicked(View view) {
        ConfigFragment configFragment = getConfigFragment();
        if (configFragment != null) {
            configFragment.numberPickerPopup(view);
        }
    }

    public void onApplyConfigButtonClicked(View view) {
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        final View currentFocus = getCurrentFocus();
        if (currentFocus != null) {
            final IBinder windowToken = currentFocus.getWindowToken();
            if (windowToken != null) {
                assert inputManager != null;
                inputManager.hideSoftInputFromWindow(windowToken, InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }

        try {
            boolean enable = ConfigFragment.getEnable();
            long dateTime = ConfigFragment.getDateTime();
            int interval = ConfigFragment.getInterval();
            int wakeupTime = ConfigFragment.getWakeupTime();
            int startDelay = ConfigFragment.getStartDelay();
            int runningTime = ConfigFragment.getRunningTime();
            int upperThreshold = ConfigFragment.getUpperThreshold();
            int lowerThreshold = ConfigFragment.getLowerThreshold();
            int validMinimum = ConfigFragment.getValidMinimum();
            int validMaximum = ConfigFragment.getValidMaximum();

            if (interval < 1) {
                String s = String.format(getResources().getString(R.string.configNotSent_message), 1, getResources().getString(R.string.seconds));
                showMessage(s, Snackbar.LENGTH_LONG);
                return;
            }
            goingToIdle = false;

            Message communicationMessage;
            communicationMessage = Message.obtain();
            communicationMessage.obj = new SetConfigCommand(enable, dateTime, interval, wakeupTime, startDelay, runningTime, upperThreshold, lowerThreshold, validMinimum, validMaximum);

            (communicationThread.getHandler()).sendMessage(communicationMessage);
            showMessage(R.string.configSent_message);
            communicationMessage = Message.obtain();
            communicationMessage.obj = new GetConfigCommand();
            (communicationThread.getHandler()).sendMessage(communicationMessage);
        } catch (IllegalArgumentException e) {
            // absorb
        }
    }

    // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //
    // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //
    // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //

    private Command createCommand(th.co.sic.app.nvtm.settings.messages.Message.Id commandId) {
        Command command = null;
        if (isConnected) {
            switch (commandId) {
                case GET_CONFIG:
                    command = new GetConfigCommand();
                    break;
                case SET_CONFIG: // Is only created on user request
                default:
                    command = null;
                    break;
            }
        }
        return command;
    }

    // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //
    // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //
    // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //

    private void handleResponse(Response response) {
        if (isConnected) {
            if (!(handleGetConfigResponse(response))) {
                if (!(handleSetConfigResponse(response))) {
                    Log.d("response", "Unknown response " + response.toString());
                }
            }
        }
    }

    private boolean handleGetConfigResponse(Response response) {
        if (response instanceof GetConfigResponse) {
            GetConfigResponse r = (GetConfigResponse) response;
            if (lastGetConfigResponse == null) {
                if (r.getCount() == 0) {
                    showMessage(R.string.emptyTag_message, Snackbar.LENGTH_LONG);
                } else {
                    showMessage(String.format(getString(R.string.tag_message), r.getCount()));
                }
                if (r.countLimitIsReached()) {
                    showMessage(getString(R.string.measurementsStopped_message));
                }
            }

            lastGetConfigResponse = r;
            try {
                getConfigFragment().connected(true);
            } catch (IllegalArgumentException e) {
                // absorb
            }
        }
        return (response instanceof GetConfigResponse);
    }

    private boolean handleSetConfigResponse(Response response) {
        if (response instanceof SetConfigResponse) {
            SetConfigResponse setConfigResponse = (SetConfigResponse) response;
            Vibrator vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            long[] duration = {15, 30, 60, 90};
            assert vib != null;
            vib.vibrate(duration, -1);

            boolean operatingUpToNow = (lastGetConfigResponse != null) && (lastGetConfigResponse.getInterval() > 0);
            if (setConfigResponse.getErrorCode() != 0) {
                showMessage(R.string.reconfigRejected_message, Snackbar.LENGTH_LONG);
            } else if (goingToIdle) {
                showMessage(R.string.pristineConfigConfirmed_message, Snackbar.LENGTH_LONG);
            } else if (operatingUpToNow) {
                showMessage(R.string.reconfigConfirmed_message, Snackbar.LENGTH_LONG);
            } else {
                int startDelay = ConfigFragment.getStartDelay();
                if (startDelay == 0) {
                    showMessage(R.string.firstConfigConfirmed_0_message, Snackbar.LENGTH_LONG);
                } else if (startDelay == -1) {
                    showMessage(R.string.firstConfigConfirmed_wait_message, Snackbar.LENGTH_LONG);
                } else {
                    String startDelayUnit = getResources().getString(R.string.seconds);
                    String s = String.format(getResources().getString(R.string.firstConfigConfirmed_message), startDelay, startDelayUnit);
                    showMessage(s, Snackbar.LENGTH_LONG);
                }
            }
        }
        return (response instanceof SetConfigResponse);
    }

    // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //
    // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //
    // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        saveInstanceState(outState);
    }

    private void saveInstanceState(Bundle outState) {
        outState.putParcelable("lastGetConfigResponse", lastGetConfigResponse);
        outState.putString("lastUsedTagId", lastUsedTagId);
    }

    private void restoreInstanceState(Bundle inState) {
        if (inState != null) {
            lastGetConfigResponse = inState.getParcelable("lastGetConfigResponse");
            lastUsedTagId = inState.getString("lastUsedTagId");
        }
    }
}
