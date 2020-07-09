package th.co.sic.app.nvtm.settings.utils

import kotlin.experimental.and
import kotlin.math.roundToInt

object Util {
    /**
     * Converts an array of bytes back to a single number
     *
     * @param bytes Array of bytes, of any length. Maximally, only the first 4 bytes are looked at.
     * @return A number. The top bit of the meaningful part of the number is not propagated.
     */
    fun bytesToInt(bytes: ByteArray): Int {
        val parts = intArrayOf(0, 0, 0, 0)
        for (i in 0 until bytes.size.coerceAtMost(4)) {
            parts[i] = if (bytes[i] >= 0) bytes[i].toInt() else bytes[i] + 256
        }
        return parts[0] + (parts[1] shl 8) + (parts[2] shl 16) + (parts[3] shl 24)
    }

    @JvmStatic
    fun bytesToHexString(bytes: ByteArray?, separator: Char): String {
        var s = "0"
        val hexString = StringBuilder()
        if (bytes != null && bytes.isNotEmpty()) {
            for (b in bytes) {
                val n: Byte = b and 0xff.toByte()
                if (n < 0x10) {
                    hexString.append("0")
                }
                hexString.append(Integer.toHexString(n.toInt()))
                if (separator.toInt() != 0) {
                    hexString.append(separator)
                }
            }
            s = hexString.substring(0, hexString.length - 1)
        }
        return s
    }

    fun celsiusToFahrenheit(deciCelsius: Int): Int {
        return ((1.8 * (deciCelsius / 10.0) + 32.0) * 10.0).roundToInt()
    }

    fun celsiusToFahrenheit(celsius: Float): Float {
        return (((1.8 * celsius + 32.0) * 10.0).roundToInt() / 10.0).toFloat()
    }

    fun celsiusToFahrenheit(celsius: Double): Double {
        return ((1.8 * celsius + 32.0) * 10.0).roundToInt() / 10.0
    }

    fun fahrenheitToCelsius(deciFahrenheit: Int): Int {
        return ((deciFahrenheit / 10.0 - 32) / 1.8 * 10.0).roundToInt().toInt()
    }

    fun fahrenheitToCelsius(fahrenheit: Float): Float {
        return (((fahrenheit - 32) / 1.8 * 10.0).roundToInt() / 10.0).toFloat()
    }

    fun fahrenheitToCelsius(fahrenheit: Double): Double {
        return ((fahrenheit - 32) / 1.8 * 10.0).roundToInt() / 10.0
    }
}