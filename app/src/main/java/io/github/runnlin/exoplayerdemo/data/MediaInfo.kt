package io.github.runnlin.exoplayerdemo.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "MediaInfo")
data class MediaInfo(
    @PrimaryKey @ColumnInfo(name = "UUID") val uuid: String,
    @ColumnInfo(name = "TYPE") val type: String,
    @ColumnInfo(name = "SIZE") val size: Int,
    @ColumnInfo(name = "TITLE") var title: String,
    @ColumnInfo(name = "DURATION") val duration: Int,
    @ColumnInfo(name = "PATH") val path: String
)
