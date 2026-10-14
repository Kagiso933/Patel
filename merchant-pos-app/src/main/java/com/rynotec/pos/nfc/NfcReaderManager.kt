package com.rynotec.pos.nfc

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.util.Log
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * Drives the device NFC radio in reader mode so it acts as a Ryno-Tec
 * POS terminal, negotiating a SELECT AID APDU handshake with the Huawei
 * Wallet pass applet and extracting the pass serial number.
 */
class NfcReaderManager(
    private val activity: Activity,
    private val onResult: (NfcReadResult) -> Unit,
) : NfcAdapter.ReaderCallback {

    companion object {
        private const val TAG = "RynoTec.NfcReader"

        // Combine Type A polling + skip NDEF so onTagDiscovered fires
        // immediately without Android attempting a generic NDEF parse first.
        private val READER_FLAGS =
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
    }

    private val adapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)

    val isAvailable: Boolean get() = adapter != null
    val isEnabled: Boolean get() = adapter?.isEnabled == true

    fun startReading() {
        adapter?.enableReaderMode(activity, this, READER_FLAGS, Bundle())
            ?: onResult(NfcReadResult.Failure("NFC hardware not found"))
    }

    fun stopReading() {
        adapter?.disableReaderMode(activity)
    }

    override fun onTagDiscovered(tag: Tag) {
        val isoDep = IsoDep.get(tag)
        if (isoDep == null) {
            onResult(NfcReadResult.Failure("Tag does not support ISO-DEP"))
            return
        }

        try {
            isoDep.connect()
            isoDep.timeout = 3_000

            val response = isoDep.transceive(ApduCommands.selectAid())
            Log.d(TAG, "SELECT AID response: ${ApduCommands.statusWordHex(response)}")

            if (!ApduCommands.isSuccess(response)) {
                onResult(
                    NfcReadResult.Failure(
                        "SELECT AID rejected — SW ${ApduCommands.statusWordHex(response)}"
                    )
                )
                return
            }

            val payload = ApduCommands.extractPayload(response)
            if (payload == null || payload.isEmpty()) {
                onResult(NfcReadResult.Failure("Empty payload in SELECT AID response"))
                return
            }

            val serialNumber = String(payload, StandardCharsets.UTF_8).trim()
            Log.i(TAG, "Pass read: $serialNumber")
            onResult(NfcReadResult.Success(serialNumber))

        } catch (e: IOException) {
            onResult(NfcReadResult.Failure("NFC I/O error: ${e.message}"))
        } finally {
            runCatching { isoDep.close() }
        }
    }
}
