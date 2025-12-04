package com.example.roomies;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ChoreSwapDao {
    @Insert long insert(ChoreSwapEntity swap);

    @Query("SELECT * FROM chore_swaps WHERE weekOffset = :week")
    List<ChoreSwapEntity> getSwapsForWeek(int week);

    // For syncing: get all swaps, regardless of week
    @Query("SELECT * FROM chore_swaps")
    List<ChoreSwapEntity> getAll();

    // For syncing: delete ALL swaps
    @Query("DELETE FROM chore_swaps")
    void deleteAll();

    @Query("DELETE FROM chore_swaps WHERE weekOffset < :week")
    void deleteOldSwaps(int week);

    @Query("DELETE FROM chore_swaps WHERE id = :swapId")
    void deleteById(int swapId);
}