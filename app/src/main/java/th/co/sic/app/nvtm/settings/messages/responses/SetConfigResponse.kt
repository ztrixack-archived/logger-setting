package th.co.sic.app.nvtm.settings.messages.responses

import android.os.Parcel
import android.os.Parcelable

class SetConfigResponse : Response {
    // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //
    // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //
    // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //
    constructor() : super() {
        LENGTH = 6
    }

    constructor(data: ByteArray?) : super(data!!)
    private constructor(`in`: Parcel) : super(`in`)

    companion object {
        val CREATOR: Parcelable.Creator<SetConfigResponse> = object : Parcelable.Creator<SetConfigResponse> {
            override fun createFromParcel(`in`: Parcel): SetConfigResponse? {
                return SetConfigResponse(`in`)
            }

            override fun newArray(size: Int): Array<SetConfigResponse?> {
                return arrayOfNulls(size)
            }
        }
    }
}