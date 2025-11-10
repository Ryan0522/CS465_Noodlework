package com.example.roomues;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;
import androidx.lifecycle.LiveData;

@Dao
public interface ChoreDao {
    @Insert
    void insert(ChoreEntity chore);

    @Update
    void update(ChoreEntity chore);

    @Query("SELECT * FROM chores")
    List<ChoreEntity> getAllChores();

    @Query("SELECT * FROM chores")
    LiveData<List<ChoreEntity>> getAllChoresLive();
}
