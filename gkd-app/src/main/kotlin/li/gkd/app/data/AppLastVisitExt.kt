package li.gkd.app.data

import li.gkd.app.META
import li.gkd.app.a11y.launcherAppId
import li.gkd.app.util.systemUiAppId
import li.gkd.db.AppLastVisit
import li.gkd.db.Db

private var appLogCount = 0

suspend fun AppLastVisit.AppLastVisitDao.insert(oldAppId: String, newAppId: String, lastVisitTime: Long) {
    Db.withTransaction {
        insert(
            AppLastVisit(oldAppId, fixAppVisitTime(oldAppId, lastVisitTime - 1)),
            AppLastVisit(newAppId, fixAppVisitTime(newAppId, lastVisitTime)),
        )
        if (appLogCount++ % 100 == 0) {
            deleteKeepLatest()
        }
    }
}

private fun fixAppVisitTime(appId: String, time: Long): Long = when (appId) {
    META.appId -> time - 120_000
    launcherAppId, systemUiAppId -> time - 60_000
    else -> time
}
