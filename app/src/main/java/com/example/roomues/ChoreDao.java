package com.example.roomues;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Delete;
import java.util.List;
import androidx.lifecycle.LiveData;

@Dao
public interface ChoreDao {

    @Insert void insert(ChoreEntity chore); // add new chore
    @Delete void delete(ChoreEntity chore); // delete chore

    @Query("SELECT * FROM chores")
    List<ChoreEntity> getAll(); // get all chores

    @Query("SELECT * FROM chores")
    LiveData<List<ChoreEntity>> getAllLive(); // gives live-updating version

    @Query("SELECT * FROM chores WHERE id = :id LIMIT 1")
    ChoreEntity getById(int id);
}
