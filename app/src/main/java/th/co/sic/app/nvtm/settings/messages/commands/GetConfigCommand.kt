package th.co.sic.app.nvtm.settings.messages.commands

import android.os.Parcel
import android.os.Parcelable
import th.co.sic.app.nvtm.settings.messages.Message

class GetConfigCommand : Command {
    // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //
    // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //
    // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //
    constructor() : super(Message.Id.GET_CONFIG)
    private constructor(`in`: Parcel) : super(`in`)

    companion object {
        const val LENGTH = 2
        val CREATOR: Parcelable.Creator<GetConfigCommand> = object : Parcelable.Creator<GetConfigCommand> {
            override fun createFromParcel(`in`: Parcel): GetConfigCommand? {
                return GetConfigCommand(`in`)
            }

            override fun newArray(size: Int): Array<GetConfigCommand?> {
                return arrayOfNulls(size)
            }
        }
    }
}