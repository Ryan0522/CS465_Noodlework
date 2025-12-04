package com.example.roomies;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Delete;
import androidx.room.Update;
import java.util.List;

@Dao
public interface ChoreDao {

    @Insert void insert(ChoreEntity chore); // add new chore
    @Update void update(ChoreEntity chore);
    @Delete void delete(ChoreEntity chore); // delete chore

    @Query("SELECT * FROM chores")
    List<ChoreEntity> getAll(); // get all chores

    @Query("SELECT * FROM chores")
    LiveData<List<ChoreEntity>> getAllLive(); // gives live-updating version

    @Query("SELECT * FROM chores WHERE roommateId = :roommateId")
    List<ChoreEntity> getByRoommate(int roommateId);

    @Query("SELECT * FROM chores WHERE id = :id")
    List<ChoreEntity> getById(int id);
}
