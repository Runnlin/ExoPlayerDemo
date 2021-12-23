package io.github.runnlin.exoplayerdemo.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "MediaInfo")
data class MediaInfo(
    @PrimaryKey @ColumnInfo(name = "UUID") val uuid: String,
    @ColumnInfo(name = "TYPE") val type: String = "media",
    @ColumnInfo(name = "SIZE") val size: Int = 10,
    @ColumnInfo(name = "TITLE") var title: String,
    @ColumnInfo(name = "DURATION") val duration: Int = 10,
    @ColumnInfo(name = "PATH") val path: String?,
    @ColumnInfo(name = "ABILITY") var isAbility: Int = 0// 0: ready for check,  1: can play, 2: can not play,  3: Playing
)
