package com.example.roomues;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;
import androidx.lifecycle.LiveData;

@Dao
public interface ChoreDao {
    @Insert
    void insert(ChoreEntity chore);

    @Query("SELECT * FROM chores")
    List<ChoreEntity> getAllChores();

    @Query("SELECT * FROM chores")
    LiveData<List<ChoreEntity>> getAllChoresLive();
}
