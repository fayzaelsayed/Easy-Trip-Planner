package com.example.tripplanner.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TripDatabaseDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertTrip(trip: TripEntity)

    @Update
    fun updateTrip(trip: TripEntity)

    @Query("UPDATE trip_table SET trip_status = :status WHERE tripId = :key ")
    fun updateTripStatus(key: String, status: String)

    @Query("UPDATE trip_table SET date = :date WHERE tripId = :key ")
    fun updateTripDate(key: String, date: String)

    @Query("SELECT * FROM trip_table WHERE email = :email AND trip_status = 'UPCOMING' ORDER BY insertion_time DESC")
    fun getAllUpcomingUserTrips(email: String): LiveData<List<TripEntity>>

    @Query("SELECT * FROM trip_table WHERE email = :email AND trip_status IN ('CANCELED', 'FINISHED') ORDER BY insertion_time DESC")
    fun getAllHistoryUserTrips(email: String): LiveData<List<TripEntity>>

    @Query("DELETE FROM trip_table")
    fun deleteAllTrips()

    @Delete
    fun deleteTripById(tripEntity: TripEntity)

    @Query("SELECT * FROM trip_table WHERE email=:email AND trip_status = 'FINISHED' ORDER BY start_time DESC")
    fun getAllFinishedTrips(email: String): LiveData<List<TripEntity>>

    @Query("SELECT * FROM trip_table WHERE email=:email AND trip_status = 'CANCELED' ORDER BY start_time DESC")
    fun getAllCanceledTrips(email: String): LiveData<List<TripEntity>>

    @Query("UPDATE trip_table SET start_time = :start WHERE tripId =:key")
    fun updateStartTime(key: String, start: Long)

    @Query("SELECT * FROM trip_table WHERE email=:email ORDER BY insertion_time DESC")
    fun getAllTrips(email: String): LiveData<List<TripEntity>>
}