package com.example.tripplanner.database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity(tableName = "trip_table", indices = [Index(value = ["email"])])
data class TripEntity(
    @PrimaryKey(autoGenerate = false)
    var tripId: String,
    @ColumnInfo(name = "trip_name")
    var tripName: String,
    @ColumnInfo(name = "start_point")
    var startPoint: String,
    @ColumnInfo(name = "end_point")
    var endPoint: String,
    @ColumnInfo(name = "note")
    var note: String,
    @ColumnInfo(name = "date")
    var date: String,
    @ColumnInfo(name = "time")
    var time: String,
    @ColumnInfo(name = "email")
    var email: String,
    @ColumnInfo(name = "trip_status")
    var tripStatus: String,
    @ColumnInfo(name = "start_point_lat_lng")
    var startPointLatLng: String,
    @ColumnInfo(name = "end_point_lat_lng")
    var endPointLatLng: String,
    @ColumnInfo(name = "insertion_time")
    var insertionTime: Long,
    @ColumnInfo(name = "trip_distance")
    var tripDistance: String,
    @ColumnInfo(name = "trip_duration")
    var tripDuration: String,
    @ColumnInfo(name = "start_time")
    var startTime: Long,
    @ColumnInfo(name = "work_request")
    var workRequest: Long,
    @ColumnInfo(name = "source")
    var source: String,
    @ColumnInfo(name = "trip_type")
    var tripType : String,
    @ColumnInfo(name = "rounded_id")
    var roundedId :String
) : Parcelable
