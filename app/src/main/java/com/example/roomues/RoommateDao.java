package com.example.roomues;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.lifecycle.LiveData;
import java.util.List;

@Dao
public interface RoommateDao {
    @Insert
    void insert(RoommateEntity roommate);

    @Query("SELECT * FROM roommates")
    List<RoommateEntity> getAllRoommates();

    @Query("SELECT * FROM roommates")
    LiveData<List<RoommateEntity>> getAllRoommatesLive();
}
