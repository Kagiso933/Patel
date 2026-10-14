package com.rynotec.pos.nfc

/**
 * Ryno-Tec APDU command builder.
 *
 * Constructs the ISO 7816-4 byte arrays used to communicate with the
 * Huawei Wallet pass applet over NFC (ISO 14443-4 / ISO-DEP).
 */
object ApduCommands {

    // Ryno-Tec closed-loop AID: F2 22 22 22 22
    val TARGET_AID: ByteArray = byteArrayOf(0xF2.toByte(), 0x22, 0x22, 0x22, 0x22)

    /**
     * SELECT AID — full command: 00 A4 04 00 05 F2 22 22 22 22 00
     *
     *   CLA  = 0x00       Standard ISO 7816 class byte (no secure messaging)
     *   INS  = 0xA4       SELECT instruction
     *   P1   = 0x04       Select "by name" (i.e. by AID)
     *   P2   = 0x00       First or only occurrence; return FCI template
     *   Lc   = 0x05       Length of the data field (5-byte AID)
     *   Data = F2 22 22 22 22   Ryno-Tec custom closed-loop AID
     *   Le   = 0x00       Accept response of any length
     */
    fun selectAid(): ByteArray =
        byteArrayOf(
            0x00,                        // CLA
            0xA4.toByte(),               // INS – SELECT
            0x04,                        // P1  – select by name
            0x00,                        // P2  – first occurrence
            TARGET_AID.size.toByte(),    // Lc
        ) + TARGET_AID + byteArrayOf(0x00)  // Le

    /** Returns true when the last two bytes of a response are 90 00 (success). */
    fun isSuccess(response: ByteArray): Boolean =
        response.size >= 2 &&
        response[response.size - 2] == 0x90.toByte() &&
        response[response.size - 1] == 0x00.toByte()

    /** Strips the SW1/SW2 status trailer and returns the payload bytes. */
    fun extractPayload(response: ByteArray): ByteArray? =
        if (response.size > 2) response.copyOfRange(0, response.size - 2) else null

    /** Formats the status word as hex string for logging ("9000", "6A82", …). */
    fun statusWordHex(response: ByteArray): String =
        if (response.size >= 2)
            "%02X%02X".format(response[response.size - 2], response[response.size - 1])
        else "????"
}
