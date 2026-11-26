package li.gkd.app.snapshot

/** Separates local capture, explicit generation, and globally gated automatic generation. */
internal enum class SnapshotCaptureMode {
    SNAPSHOT_ONLY,
    SNAPSHOT_AND_GENERATE,
    AUTOMATIC;

    suspend fun run(
        capture: suspend () -> Long,
        aiEnabled: Boolean = true,
        generate: suspend (Long, Boolean) -> Unit,
    ) {
        val snapshotId = capture()
        when (this) {
            SNAPSHOT_ONLY -> Unit
            SNAPSHOT_AND_GENERATE -> generate(snapshotId, true)
            AUTOMATIC -> if (aiEnabled) generate(snapshotId, false)
        }
    }
}
