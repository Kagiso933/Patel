package com.example.merchant.nfc

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * Drives the tablet/phone's NFC radio in reader mode so it can act as a
 * POS terminal, sending the SELECT AID APDU to any tapped pass and
 * extracting the serial number from the response.
 */
class NfcReaderManager(
    private val activity: Activity,
    private val onPassRead: (serialNumber: String) -> Unit,
    private val onError: (message: String) -> Unit
) : NfcAdapter.ReaderCallback {

    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)

    fun startReaderMode() {
        val adapter = nfcAdapter ?: run {
            onError("NFC not supported on this device")
            return
        }

        // FLAG_READER_NFC_A: only poll for Type A tags (what the pass applet
        // presents). FLAG_READER_SKIP_NDEF_CHECK: skip Android's default NDEF
        // parsing pass so onTagDiscovered fires immediately instead of the
        // OS trying (and failing) to read a generic NDEF message first.
        val flags = NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
        adapter.enableReaderMode(activity, this, flags, Bundle())
    }

    fun stopReaderMode() {
        nfcAdapter?.disableReaderMode(activity)
    }

    override fun onTagDiscovered(tag: Tag) {
        val isoDep = IsoDep.get(tag)
        if (isoDep == null) {
            onError("Tag does not support ISO-DEP (ISO 14443-4)")
            return
        }

        try {
            isoDep.connect()
            isoDep.timeout = 3000 // ms

            val response = isoDep.transceive(ApduCommands.buildSelectAidCommand())

            if (!endsInSuccess(response)) {
                onError("SELECT AID failed, status=${statusWordHex(response)}")
                return
            }

            val serialNumber = extractSerialNumber(response)
            if (serialNumber.isNullOrBlank()) {
                onError("Pass response contained no serial number")
                return
            }

            onPassRead(serialNumber)
        } catch (e: IOException) {
            onError("NFC I/O error: ${e.message}")
        } finally {
            try {
                isoDep.close()
            } catch (e: IOException) {
                // Tag already moved out of range; nothing to clean up.
            }
        }
    }

    /** Response is successful when the last two bytes (SW1 SW2) are 90 00. */
    private fun endsInSuccess(response: ByteArray): Boolean {
        if (response.size < 2) return false
        val sw1 = response[response.size - 2]
        val sw2 = response[response.size - 1]
        return sw1 == 0x90.toByte() && sw2 == 0x00.toByte()
    }

    private fun statusWordHex(response: ByteArray): String {
        if (response.size < 2) return "????"
        return "%02X%02X".format(response[response.size - 2], response[response.size - 1])
    }

    /** Strips the trailing 90 00 status word and decodes the remaining payload as UTF-8. */
    private fun extractSerialNumber(response: ByteArray): String? {
        if (response.size <= 2) return null
        val payload = response.copyOfRange(0, response.size - 2)
        return String(payload, StandardCharsets.UTF_8).takeIf { it.isNotBlank() }
    }
}
