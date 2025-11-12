package com.example.roomues;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Delete;
import androidx.lifecycle.LiveData;
import java.util.List;

@Dao
public interface RoommateDao {

    @Insert long insert(RoommateEntity roommate); // return rowId
    @Delete void delete(RoommateEntity roommate);

    @Query("SELECT * FROM roommates") List<RoommateEntity> getAll(); // returns all roommates
    @Query("SELECT * FROM roommates") LiveData<List<RoommateEntity>> getAllLive(); // give a live-updating version

    @Query("SELECT * FROM roommates WHERE id = :id LIMIT 1") RoommateEntity getById(int id);
    @Query("SELECT COUNT(*) FROM roommates WHERE LOWER(name) = LOWER(:name)") int countByName(String name);
}