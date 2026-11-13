package com.rescuevpn.app.data.model

import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Strict deterministic serializer for non-secret per-profile fixed-egress metadata.
 * This codec is not encryption or authentication. Store its output only inside
 * an authenticated encrypted record whose AAD binds the profile ID.
 */
internal object FixedEgressMetadataCodec {
    private const val HEADER = "RMA_FIXED_EGRESS_METADATA"
    private const val FORMAT_VERSION = "1"
    private const val MAX_RECORD_CHARS = 4096
    private const val MAX_PROFILE_ID_CHARS = 128
    private const val NULL_VALUE = "~"

    fun encode(profileId: String, metadata: FixedEgressMetadata): String {
        require(validProfileId(profileId)) { "Invalid profile ID." }
        require(metadata.isValid()) { metadata.validationErrors().joinToString(" ") }
        return listOf(
            HEADER,
            "format_version=$FORMAT_VERSION",
            "profile_id=" + encodeText(profileId),
            "schema_version=" + metadata.schemaVersion,
            "mode=" + metadata.mode.name,
            "gateway_ownership=" + metadata.gatewayOwnership.name,
            "expected_egress_ipv4=" + (metadata.expectedEgressIpv4?.let { encodeText(it) } ?: NULL_VALUE),
            "gateway_label=" + (metadata.gatewayLabel?.let { encodeText(it) } ?: NULL_VALUE)
        ).joinToString("\n")
    }

    fun decode(expectedProfileId: String, record: String): FixedEgressMetadata {
        require(validProfileId(expectedProfileId)) { "Invalid expected profile ID." }
        require(record.length <= MAX_RECORD_CHARS) { "Metadata record exceeds the size limit." }
        require('\r' !in record) { "Metadata record contains unsupported line endings." }
        val lines = record.split('\n')
        require(lines.size == 8 && lines[0] == HEADER) { "Invalid metadata record structure." }

        val values = linkedMapOf<String, String>()
        for (line in lines.drop(1)) {
            val separator = line.indexOf('=')
            require(separator > 0) { "Malformed metadata field." }
            val key = line.substring(0, separator)
            require(key !in values) { "Duplicate metadata field." }
            values[key] = line.substring(separator + 1)
        }

        val expectedKeys = setOf(
            "format_version", "profile_id", "schema_version", "mode",
            "gateway_ownership", "expected_egress_ipv4", "gateway_label"
        )
        require(values.keys == expectedKeys) { "Unknown or missing metadata fields." }
        require(values.getValue("format_version") == FORMAT_VERSION) {
            "Unsupported metadata record format version."
        }
        require(decodeText(values.getValue("profile_id")) == expectedProfileId) {
            "Metadata record does not belong to the requested profile."
        }

        val schemaVersion = values.getValue("schema_version").toIntOrNull()
            ?: throw IllegalArgumentException("Invalid metadata schema version.")
        val mode = enumValueOrThrow<EgressMode>(values.getValue("mode"))
        val ownership = enumValueOrThrow<GatewayOwnership>(values.getValue("gateway_ownership"))
        val metadata = FixedEgressMetadata(
            schemaVersion = schemaVersion,
            mode = mode,
            gatewayOwnership = ownership,
            expectedEgressIpv4 = decodeNullable(values.getValue("expected_egress_ipv4")),
            gatewayLabel = decodeNullable(values.getValue("gateway_label"))
        )
        require(metadata.isValid()) { metadata.validationErrors().joinToString(" ") }
        return metadata
    }

    private fun validProfileId(value: String): Boolean =
        value.isNotBlank() && value.length <= MAX_PROFILE_ID_CHARS &&
            value.none { it.isISOControl() }

    private fun encodeText(value: String): String =
        Base64.getUrlEncoder().withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodeNullable(value: String): String? =
        if (value == NULL_VALUE) null else decodeText(value)

    private fun decodeText(value: String): String {
        require(value.isNotEmpty()) { "Empty encoded metadata value." }
        val bytes = try {
            Base64.getUrlDecoder().decode(value)
        } catch (error: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid Base64 metadata value.", error)
        }
        val decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        val decoded = try {
            decoder.decode(ByteBuffer.wrap(bytes)).toString()
        } catch (error: Exception) {
            throw IllegalArgumentException("Metadata value is not valid UTF-8.", error)
        }
        require(encodeText(decoded) == value) { "Metadata value is not canonically encoded." }
        return decoded
    }

    private inline fun <reified T : Enum<T>> enumValueOrThrow(value: String): T =
        try {
            enumValueOf<T>(value)
        } catch (error: IllegalArgumentException) {
            throw IllegalArgumentException("Unsupported metadata enum value.", error)
        }
}
