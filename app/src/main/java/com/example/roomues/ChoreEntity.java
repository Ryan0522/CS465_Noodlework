package com.example.roomues;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "chores",
        foreignKeys = @ForeignKey(
                entity = RoommateEntity.class,
                parentColumns = "id",
                childColumns = "roommateId",
                onDelete = ForeignKey.CASCADE
        )
)
public class ChoreEntity {

    @PrimaryKey(autoGenerate = true)
    public int id; // Unique ID

    public String name; // e.g., "Take out trash"
    public String frequency; // e.g., "Weekly"
    public int roommateId; // which roommate is assigned

    // --- constructor ---
    public ChoreEntity(String name, String frequency, int roommateId) {
        this.name = name;
        this.frequency = frequency;
        this.roommateId = roommateId;
    }
}
