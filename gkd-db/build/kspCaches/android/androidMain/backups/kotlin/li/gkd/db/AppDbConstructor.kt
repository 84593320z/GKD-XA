package li.gkd.db

import androidx.room3.RoomDatabaseConstructor

internal actual object AppDbConstructor : RoomDatabaseConstructor<AppDb> {
  actual override fun initialize(): AppDb = li.gkd.db.AppDb_Impl()
}
