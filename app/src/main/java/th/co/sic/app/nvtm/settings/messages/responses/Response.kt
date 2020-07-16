package th.co.sic.app.nvtm.settings.messages.responses

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import th.co.sic.app.nvtm.settings.utils.Util
import th.co.sic.app.nvtm.settings.messages.Message
import th.co.sic.app.nvtm.settings.messages.Message.Id
import th.co.sic.app.nvtm.settings.messages.Message.Id.Companion.GetEnum
import java.util.*

abstract class Response : Parcelable {
    @JvmField
    var LENGTH = 0
    private var data: ByteArray? = null

    internal constructor()
    internal constructor(data: ByteArray) : this() {
        setData(data)
    }

    internal constructor(`in`: Parcel?) {
        if (`in` != null) {
            readFromParcel(`in`)
        }
    }

    @Throws(ArrayIndexOutOfBoundsException::class)
    protected open fun setData(data: ByteArray) {
        if (LENGTH > 0) {
            if (data.size != LENGTH) {
                throw ArrayIndexOutOfBoundsException(String.format(Locale.getDefault(), "Response decoding error for Id %s, received length %d differs from expected length %d", GetEnum(data[0].toInt()).toString(), data.size, LENGTH))
            }
        } else {
            if (data.size < -LENGTH) {
                throw ArrayIndexOutOfBoundsException(String.format(Locale.getDefault(), "Response decoding error for Id %s, received length %d is less than expected lengths %d and up", GetEnum(data[0].toInt()).toString(), data.size, LENGTH))
            }
        }
        this.data = data
    }

    override fun toString(): String {
        return if (data == null) {
            "R.$id.$tag"
        } else {
            "R." + id.toString() + "." + tag + " - " + Arrays.toString(data)
        }
    }

    val id: Id
        get() {
            var id = Id.NONE
            if (data != null && data!!.isNotEmpty()) {
                id = GetEnum(data!![0].toInt())
            }
            return id
        }

    private val tag: Int
        get() {
            var tag = 0
            if (data != null && data!!.size > 1) {
                tag = data!![1].toInt() and 0x000000FF
            }
            return tag
        }

    val errorCode: Int
        get() = Util.bytesToInt(byteArrayOf(data!![2], data!![3], data!![4], data!![5]))

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
        @JvmStatic
        fun decode(payload: ByteArray?): Response? {
            var response: Response? = null
            val responseString: String?
            if (payload == null) {
                Log.d("No data", "payload == null")
            } else if (payload.size < 2) {
                Log.d("Not enough data", Arrays.toString(payload))
            } else {
                val idStringHashMap = Message.response
                responseString = idStringHashMap[GetEnum(payload[0].toInt())]
                if (responseString != null) {
                    try {
                        val c = Class.forName(responseString)
                        response = c.newInstance() as Response
                        response.setData(payload)
                    } catch (e: ClassNotFoundException) {
                        Log.d("Error parsing message", Arrays.toString(payload))
                        response = null
                    } catch (e: InstantiationException) {
                        Log.d("Error parsing message", Arrays.toString(payload))
                        response = null
                    } catch (e: IllegalAccessException) {
                        Log.d("Error parsing message", Arrays.toString(payload))
                        response = null
                    } catch (e: ArrayIndexOutOfBoundsException) {
                        Log.d("Error parsing message", Arrays.toString(payload))
                        response = null
                    }
                } else {
                    Log.d("Unknown message", Arrays.toString(payload))
                }
            }
            return response
        }
    }
}