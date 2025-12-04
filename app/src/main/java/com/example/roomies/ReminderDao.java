package com.example.roomies;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface ReminderDao {
    @Insert long insert(ReminderEntity reminder);
    @Delete void delete(ReminderEntity reminder);

    @Query("SELECT * FROM reminders WHERE choreId = :choreId")
    List<ReminderEntity> getByChore(int choreId);

    @Query("SELECT * FROM reminders")
    List<ReminderEntity> getAll();

    @Update void update(ReminderEntity reminder);
}