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
    private int id;

    private String name;
    private String frequency;
    private int roommateId; // links to RoommateEntity

    public ChoreEntity(String name, String frequency, int roommateId) {
        this.name = name;
        this.frequency = frequency;
        this.roommateId = roommateId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public int getRoommateId() { return roommateId; }
    public void setRoommateId(int roommateId) { this.roommateId = roommateId; }
}
