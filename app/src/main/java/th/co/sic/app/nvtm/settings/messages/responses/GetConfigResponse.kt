package th.co.sic.app.nvtm.settings.messages.responses

import android.os.Parcel
import android.os.Parcelable
import th.co.sic.app.nvtm.settings.utils.Util
import kotlin.math.abs

open class GetConfigResponse : Response {
    private var currentTime = 0 // 'current' as in: when the response class was created = 0
    var configTime = 0
        protected set
    var interval = 0
        protected set
    var startDelay = 0
        protected set
    var runningTime = 0
        protected set
    var validMinimum = 0
        protected set
    var validMaximum = 0
        protected set
    /* -------------------------------------------------------------------------------- */
    var attainedMinimum = 0
        protected set
    var attainedMaximum = 0
        protected set
    var count = 0
        protected set
    private var status = 0

    // status is not returned in a getter field.
    private var startTime = 0
    private var islandTime = 0

    constructor() : super() {
        LENGTH = 42
    }

    protected constructor(`in`: Parcel?) : super(`in`)

    @Throws(ArrayIndexOutOfBoundsException::class)
    override fun setData(data: ByteArray) {
        super.setData(data)
        currentTime = (System.currentTimeMillis() / 1000).toInt()

        /* -------------------------------------------------------------------------------- */
        configTime = Util.bytesToInt(byteArrayOf(data[6], data[7], data[8], data[9]))
        interval = Util.bytesToInt(byteArrayOf(data[10], data[11]))
        startDelay = Util.bytesToInt(byteArrayOf(data[12], data[13], data[14], data[15]))
        runningTime = Util.bytesToInt(byteArrayOf(data[16], data[17], data[18], data[19]))
        var t = Util.bytesToInt(byteArrayOf(data[20], data[21]))
        if (t >= 0x00008000) {
            t -= 0x10000
        }
        validMinimum = t
        t = Util.bytesToInt(byteArrayOf(data[22], data[23]))
        if (t >= 0x00008000) {
            t -= 0x10000
        }
        validMaximum = t
        /* -------------------------------------------------------------------------------- */
        t = Util.bytesToInt(byteArrayOf(data[24], data[25]))
        if (t >= 0x00008000) {
            t -= 0x10000
        }
        attainedMinimum = t
        t = Util.bytesToInt(byteArrayOf(data[26], data[27]))
        if (t >= 0x00008000) {
            t -= 0x10000
        }
        attainedMaximum = t
        count = Util.bytesToInt(byteArrayOf(data[28], data[29]))
        status = Util.bytesToInt(byteArrayOf(data[30], data[31], data[32], data[33]))
        startTime = Util.bytesToInt(byteArrayOf(data[34], data[35], data[36], data[37]))
        islandTime = Util.bytesToInt(byteArrayOf(data[38], data[39], data[40], data[41]))
    }

    // islandTime is not returned in a getter field.
    /* -------------------------------------------------------------------------------- */
    fun memoryIsFull(): Boolean {
        return status and MSG_EVENT_FULL != 0
    }

    fun countIsLimited(): Boolean {
        return runningTime > 0
    }

    fun countLimitIsReached(): Boolean {
        return status and MSG_EVENT_EXPIRED != 0
    }

    val isPristine: Boolean
        get() = status and MSG_EVENT_PRISTINE != 0 && status and (MSG_EVENT_CONFIGURED or MSG_EVENT_STARTING or MSG_EVENT_LOGGING or MSG_EVENT_STOPPED) == 0

    val isConfigured: Boolean
        get() = status and MSG_EVENT_CONFIGURED != 0 && status and (MSG_EVENT_STARTING or MSG_EVENT_LOGGING or MSG_EVENT_STOPPED) == 0

    val isStarting: Boolean
        get() = status and MSG_EVENT_STARTING != 0 && status and (MSG_EVENT_LOGGING or MSG_EVENT_STOPPED) == 0

    val isLogging: Boolean
        get() = status and MSG_EVENT_LOGGING != 0 && status and MSG_EVENT_STOPPED == 0

    val isStopped: Boolean
        get() = status and MSG_EVENT_STOPPED != 0

    val isValid: Boolean
        get() = status and (MSG_EVENT_STOPPED or MSG_EVENT_TEMPERATURE_TOO_HIGH or MSG_EVENT_TEMPERATURE_TOO_LOW or MSG_EVENT_BOD or MSG_EVENT_FULL or MSG_EVENT_EXPIRED) == 0
    /* -------------------------------------------------------------------------------- *//* Something went wrong. This deviation is simply not possible.
             * Likely a reset occurred, or memory got full and measurements stopped.
             */
    /**
     * Returns the factor with which to multiply the uncorrected timestamps as received from the NHS31xx:
     * With:
     * - t: uncorrected timestamp
     * - t': corrected timestamp
     * - s: start time
     * The correction can be retrieved using this formula:
     * t = s + (t' - s) * getCorrectionFactor()
     *
     * @return a strict positive number
     */
    val correctionFactor: Double
        get() {
            var correctionFactor = (currentTime - configTime).toDouble() / (islandTime - configTime)
            if (abs(1 - correctionFactor) > 0.10) {
                /* Something went wrong. This deviation is simply not possible.
             * Likely a reset occurred, or memory got full and measurements stopped.
             */
                correctionFactor = 1.0
            }
            return correctionFactor
        }

    companion object {
        val CREATOR: Parcelable.Creator<GetConfigResponse> = object : Parcelable.Creator<GetConfigResponse> {
            override fun createFromParcel(`in`: Parcel): GetConfigResponse? {
                return GetConfigResponse(`in`)
            }

            override fun newArray(size: Int): Array<GetConfigResponse?> {
                return arrayOfNulls(size)
            }
        }
        protected const val MSG_EVENT_PRISTINE = 1

        /**
         * State change: the IC no longer has a configuration and contains no data.
         */
        protected const val MSG_EVENT_CONFIGURED = 1 shl 1

        /**
         * State change: the IC is configured, but requires a #APP_MSG_ID_START command to start.
         */
        protected const val MSG_EVENT_STARTING = 1 shl 2

        /**
         * State change: the IC is configured and will make a first measurement after a delay.
         */
        protected const val MSG_EVENT_LOGGING = 1 shl 3

        /**
         * State change: the IC is configured and is logging. At least one sample is available.
         */
        protected const val MSG_EVENT_STOPPED = 1 shl 4

        /**
         * State change: the IC is configured and has been logging. Now it has stopped logging.
         */
        protected const val MSG_EVENT_TEMPERATURE_TOO_HIGH = 1 shl 5

        /**
         * < Failure: at least one temperature was strictly higher than the valid maximum value.
         */
        protected const val MSG_EVENT_TEMPERATURE_TOO_LOW = 1 shl 6

        /**
         * < Failure: At least one temperature was strictly lower than the valid minimum value.
         */
        protected const val MSG_EVENT_BOD = 1 shl 7

        /**
         * < Failure: A brown-out is about to occur or has occurred. Battery is (nearly) depleted.
         */
        protected const val MSG_EVENT_FULL = 1 shl 8

        /**
         * < Failure: Logging has stopped because no more free space is available to store samples.
         */
        protected const val MSG_EVENT_EXPIRED = 1 shl 9
    }
}