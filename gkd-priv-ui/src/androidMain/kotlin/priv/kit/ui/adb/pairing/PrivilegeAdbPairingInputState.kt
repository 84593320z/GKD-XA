package priv.kit.ui.adb.pairing

import priv.kit.shared.PRIVILEGE_INTERNAL_ADB_PAIRING_CODE_LENGTH as PAIRING_CODE_LENGTH
import priv.kit.shared.isPrivilegeAdbPairingCode

internal data class PrivilegeAdbPairingInputState(
    val code: String = DEFAULT_PAIRING_INPUT_CODE,
    val selectedIndex: Int = 0,
) {
    init {
        require(code.isPrivilegeAdbPairingCode()) {
            "Pairing input code must contain exactly $PAIRING_CODE_LENGTH ASCII digits."
        }
        require(selectedIndex in 0 until PAIRING_CODE_LENGTH) {
            "Selected pairing input index must be in 0..${PAIRING_CODE_LENGTH - 1}."
        }
    }

    val displayText: String
        get() = code.mapIndexed { index, digit ->
            if (index == selectedIndex) "[$digit]" else digit.toString()
        }.joinToString(" ")

    fun moveLeft(): PrivilegeAdbPairingInputState =
        copy(selectedIndex = (selectedIndex + PAIRING_CODE_LENGTH - 1) % PAIRING_CODE_LENGTH)

    fun moveRight(): PrivilegeAdbPairingInputState =
        copy(selectedIndex = (selectedIndex + 1) % PAIRING_CODE_LENGTH)

    fun incrementDigit(): PrivilegeAdbPairingInputState =
        copy(code = code.replaceDigitAt(selectedIndex) { (it + 1) % 10 })

    fun decrementDigit(): PrivilegeAdbPairingInputState =
        copy(code = code.replaceDigitAt(selectedIndex) { (it + 9) % 10 })
}

private fun String.replaceDigitAt(
    index: Int,
    transform: (Int) -> Int,
): String =
    replaceRange(index, index + 1, transform(this[index].digitToInt()).toString())

private val DEFAULT_PAIRING_INPUT_CODE = "0".repeat(PAIRING_CODE_LENGTH)
