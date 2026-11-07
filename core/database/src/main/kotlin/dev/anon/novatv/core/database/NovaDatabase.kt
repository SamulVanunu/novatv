package dev.anon.novatv.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dev.anon.novatv.core.database.dao.ChannelDao
import dev.anon.novatv.core.database.dao.EpgDao
import dev.anon.novatv.core.database.dao.PlaylistDao
import dev.anon.novatv.core.database.dao.UserDataDao
import dev.anon.novatv.core.database.entity.ChannelEntity
import dev.anon.novatv.core.database.entity.EpgChannelEntity
import dev.anon.novatv.core.database.entity.EpgProgramEntity
import dev.anon.novatv.core.database.entity.EpisodeEntity
import dev.anon.novatv.core.database.entity.FavoriteEntity
import dev.anon.novatv.core.database.entity.HiddenChannelEntity
import dev.anon.novatv.core.database.entity.HiddenGroupEntity
import dev.anon.novatv.core.database.entity.MovieEntity
import dev.anon.novatv.core.database.entity.PlaylistEntity
import dev.anon.novatv.core.database.entity.ProfileEntity
import dev.anon.novatv.core.database.entity.SeasonEntity
import dev.anon.novatv.core.database.entity.SeriesEntity
import dev.anon.novatv.core.database.entity.WatchHistoryEntity

@Database(
    entities = [
        PlaylistEntity::class,
        ChannelEntity::class,
        EpgChannelEntity::class,
        EpgProgramEntity::class,
        MovieEntity::class,
        SeriesEntity::class,
        SeasonEntity::class,
        EpisodeEntity::class,
        ProfileEntity::class,
        FavoriteEntity::class,
        HiddenChannelEntity::class,
        HiddenGroupEntity::class,
        WatchHistoryEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class NovaDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun channelDao(): ChannelDao
    abstract fun epgDao(): EpgDao
    abstract fun userDataDao(): UserDataDao

    companion object {
        fun build(context: Context): NovaDatabase =
            Room.databaseBuilder(context.applicationContext, NovaDatabase::class.java, "novatv.db").build()
    }
}
