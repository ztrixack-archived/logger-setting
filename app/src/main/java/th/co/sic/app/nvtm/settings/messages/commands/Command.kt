package th.co.sic.app.nvtm.settings.messages.commands

import android.os.Parcel
import android.os.Parcelable
import th.co.sic.app.nvtm.settings.messages.Message.Id
import th.co.sic.app.nvtm.settings.messages.Message.Id.Companion.GetEnum
import th.co.sic.app.nvtm.settings.messages.Message.Id.Companion.GetInt

abstract class Command : Parcelable {
    lateinit var data: ByteArray

    @JvmOverloads
    internal constructor(id: Id?, length: Int = 2) {
        data = ByteArray(length)
        data[0] = GetInt(id!!).toByte()
        data[1] = commandTag
    }

    internal constructor(`in`: Parcel) {
        readFromParcel(`in`)
    }

    fun encode(): ByteArray {
        return data
    }

    val id: Id
        get() = GetEnum(data[0].toInt())

    val tag: Int
        get() = data[1].toInt() and 0x000000FF

    override fun toString(): String {
        return "C." + id.toString() + "." + data[1].toInt().toString() + " - " + data.contentToString()
    }

    // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //
    // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //
    // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //
    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        out.writeByteArray(data)
    }

    private fun readFromParcel(`in`: Parcel) {
        data = `in`.createByteArray()
    }

    companion object {
        private const val commandTag: Byte = 0
    }
}