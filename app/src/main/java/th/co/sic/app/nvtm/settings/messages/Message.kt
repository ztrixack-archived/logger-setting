package th.co.sic.app.nvtm.settings.messages

import java.util.*

object Message {
    private val defaultCommand: HashMap<Id, String> = object : HashMap<Id, String>() {
        init {
            put(Id.GET_CONFIG, "GetConfigCommand")
            put(Id.SET_CONFIG, "SetConfigCommand")
        }
    }
    private val defaultResponse: HashMap<Id, String> = object : HashMap<Id, String>() {
        init {
            put(Id.GET_CONFIG, "GetConfigResponse")
            put(Id.SET_CONFIG, "SetConfigResponse")
        }
    }

    // ------------------------------------------------------------------------
    val command = defaultCommand

    // ------------------------------------------------------------------------
    @JvmField
    val response = defaultResponse

    // ------------------------------------------------------------------------
    enum class Id(private val id: Int) {
        NONE(0x00), GET_CONFIG(0x48), SET_CONFIG(0x49);

        fun Compare(id: Int): Boolean {
            return this.id == id
        }

        companion object {
            @JvmStatic
            fun GetEnum(id: Int): Id {
                val ids = values()
                for (x in ids) {
                    if (x.Compare(id)) {
                        return x
                    }
                }
                return NONE
            }

            @JvmStatic
            fun GetInt(id: Id): Int {
                return id.id
            }
        }

    }
}