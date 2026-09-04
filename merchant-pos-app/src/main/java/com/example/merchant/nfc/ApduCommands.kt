package com.example.merchant.nfc

/** Builds the raw APDU byte arrays exchanged with the pass applet. */
object ApduCommands {

    // Closed-loop simulation AID: F2 22 22 22 22
    private val TARGET_AID = byteArrayOf(0xF2.toByte(), 0x22, 0x22, 0x22, 0x22)

    /**
     * SELECT AID command (ISO 7816-4), full hex: 00 A4 04 00 05 F2 22 22 22 22 00
     *
     *   CLA = 0x00              - standard class, no secure messaging
     *   INS = 0xA4               - SELECT instruction
     *   P1  = 0x04               - select "by name" (i.e. by AID)
     *   P2  = 0x00               - first/only occurrence, return FCI
     *   Lc  = AID.size (0x05)    - number of bytes in the data field
     *   Data = TARGET_AID        - the 5-byte AID being selected
     *   Le  = 0x00               - expect a response of any length
     */
    fun buildSelectAidCommand(): ByteArray {
        val header = byteArrayOf(
            0x00,               // CLA
            0xA4.toByte(),      // INS - SELECT
            0x04,               // P1  - select by name
            0x00,               // P2  - first occurrence
            TARGET_AID.size.toByte() // Lc
        )
        val trailer = byteArrayOf(0x00) // Le
        return header + TARGET_AID + trailer
    }
}
