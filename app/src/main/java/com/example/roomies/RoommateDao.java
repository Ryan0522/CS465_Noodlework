package com.example.roomies;

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

    @Query("SELECT * FROM roommates WHERE name = :name LIMIT 1")
    RoommateEntity getRoommateByName(String name);

    // New: rename a roommate
    @Query("UPDATE roommates SET name = :newName WHERE id = :id")
    void renameRoommate(int id, String newName);

    // Mark a roommate as owned (linked on at least one device)
    @Query("UPDATE roommates SET owned = 1 WHERE id = :id")
    void markOwned(int id);

    // Clear owned flag if needed (we’ll likely use this in step 6)
    @Query("UPDATE roommates SET owned = 0 WHERE id = :id")
    void clearOwned(int id);

    // For debugging or display: get all owned roommates
    @Query("SELECT * FROM roommates WHERE owned = 1")
    List<RoommateEntity> getOwnedRoommates();
}