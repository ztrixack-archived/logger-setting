package th.co.sic.app.nvtm.settings.messages.commands

import android.os.Parcel
import android.os.Parcelable
import th.co.sic.app.nvtm.settings.messages.Message
import kotlin.experimental.and

open class SetConfigCommand : Command {
    // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //
    // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //
    // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //
    constructor(enable: Boolean, datetime: Long, interval: Int, wakeupTime: Int, startDelay: Int, runningTime: Int, upperThreshold: Int, lowerThreshold: Int, validMinimum: Int, validMaximum: Int) : super(Message.Id.SET_CONFIG, LENGTH) {
        var index = 2

        // turn on/off
        val _enable: Byte = if (enable) 0x55.toByte() else 0x00.toByte()
        data[index++] = (_enable and 0x000000FF.toByte()).toByte()

        // current date time
        val _datetime = datetime / 1000
        data[index++] = (_datetime and 0x000000FF).toByte()
        data[index++] = (_datetime shr 8 and 0x000000FF).toByte()
        data[index++] = (_datetime shr 16 and 0x000000FF).toByte()
        data[index++] = (_datetime shr 24 and 0x000000FF).toByte()

        // interval
        data[index++] = (interval and 0x00FF).toByte()
        data[index++] = (interval shr 8 and 0x00FF).toByte()

        // wakeup times to log
        val _wakeupTime = wakeupTime / interval
        data[index++] = (_wakeupTime and 0x00FF).toByte()

        // start delay (IGNORED)
        data[index++] = (startDelay and 0x000000FF).toByte()
        data[index++] = (startDelay shr 8 and 0x000000FF).toByte()
        data[index++] = (startDelay shr 16 and 0x000000FF).toByte()
        data[index++] = (startDelay shr 24 and 0x000000FF).toByte()

        // running Time (IGNORED)
        data[index++] = (runningTime and 0x000000FF).toByte()
        data[index++] = (runningTime shr 8 and 0x000000FF).toByte()
        data[index++] = (runningTime shr 16 and 0x000000FF).toByte()
        data[index++] = (runningTime shr 24 and 0x000000FF).toByte()

        // upper Threshold
        val _upperThreshold = upperThreshold * 10
        data[index++] = (_upperThreshold and 0x00FF).toByte()
        data[index++] = (_upperThreshold shr 8 and 0x00FF).toByte()

        // lower Threshold
        val _lowerThreshold = lowerThreshold * 10
        data[index++] = (_lowerThreshold and 0x00FF).toByte()
        data[index++] = (_lowerThreshold shr 8 and 0x00FF).toByte()

        // valid Minimum (IGNORED)
        data[index++] = (validMinimum and 0x000000FF).toByte()
        data[index++] = (validMinimum shr 8 and 0x000000FF).toByte()

        // valid Maximum (IGNORED)
        data[index++] = (validMaximum and 0x000000FF).toByte()
        data[index] = (validMaximum shr 8 and 0x000000FF).toByte()
    }

    protected constructor(`in`: Parcel?) : super(`in`!!)

    companion object {
        val CREATOR: Parcelable.Creator<SetConfigCommand> = object : Parcelable.Creator<SetConfigCommand> {
            override fun createFromParcel(`in`: Parcel): SetConfigCommand? {
                return SetConfigCommand(`in`)
            }

            override fun newArray(size: Int): Array<SetConfigCommand?> {
                return arrayOfNulls(size)
            }
        }
        private const val LENGTH = 26
    }
}