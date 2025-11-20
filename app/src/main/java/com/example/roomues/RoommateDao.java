package com.example.roomues;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.lifecycle.LiveData;

import java.util.List;

@Dao
public interface RoommateDao {

    // INSERT
    @Insert
    void insert(RoommateEntity roommate);

    // UPDATE
    @Update
    void update(RoommateEntity roommate);

    // DELETE ONE
    @Delete
    void delete(RoommateEntity roommate);

    // DELETE ALL
    @Query("DELETE FROM roommates")
    void deleteAll();

    // GET ALL (LIST)
    @Query("SELECT * FROM roommates")
    List<RoommateEntity> getAllRoommates();

    // GET ALL LIVE DATA (for auto-updates)
    @Query("SELECT * FROM roommates")
    LiveData<List<RoommateEntity>> getAllRoommatesLive();
}
