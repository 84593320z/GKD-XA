package priv.kit.ui.adb.pairing

import kotlinx.coroutines.Job

/** Owns the resources and identity of one pairing flow, independently of its presentation. */
internal class PrivilegeUiPairingSession(
    private val acquirePermit: () -> AutoCloseable?,
) : AutoCloseable {
    private val permitLock = Any()
    private var permit: AutoCloseable? = null
    private var generation = 0
    private var job: Job? = null
    var port: Int? = null
        private set
    var deviceName: String? = null
        private set

    fun acquire(): Boolean = synchronized(permitLock) {
        if (permit != null) return@synchronized true
        permit = acquirePermit() ?: return@synchronized false
        true
    }

    fun begin(deviceName: String?): Int {
        invalidate()
        this.deviceName = deviceName
        return generation
    }

    fun nextOperation(): Int {
        job?.cancel()
        job = null
        return ++generation
    }

    fun isCurrent(id: Int): Boolean = generation == id

    fun discovered(port: Int, deviceName: String?) {
        this.port = port
        this.deviceName = deviceName
    }

    fun lostEndpoint() {
        port = null
    }

    fun attach(id: Int, nextJob: Job) {
        if (!isCurrent(id)) {
            nextJob.cancel()
            return
        }
        job = nextJob
        nextJob.start()
    }

    fun finishOperation() {
        // The completing coroutine must remain active for post-pairing TCP setup.
        job = null
        ++generation
        port = null
        deviceName = null
    }

    fun invalidate() {
        nextOperation()
        port = null
        deviceName = null
    }

    fun release() {
        val released = synchronized(permitLock) { permit.also { permit = null } }
        released?.close()
    }

    override fun close() {
        invalidate()
        release()
    }
}
