package com.example.roomies;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "reminders",
        foreignKeys = @ForeignKey(
                entity = ChoreEntity.class,
                parentColumns = "id",
                childColumns = "choreId",
                onDelete = ForeignKey.CASCADE
        )
)
public class ReminderEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public int choreId;
    public String timeText;  // e.g. "Saturday 6PM"
    public boolean isAuto;   // true = generated from settings

    public ReminderEntity(int choreId, String timeText, boolean isAuto) {
        this.choreId = choreId;
        this.timeText = timeText;
        this.isAuto = isAuto;
    }
}