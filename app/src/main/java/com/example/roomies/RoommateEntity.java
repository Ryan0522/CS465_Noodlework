package com.example.roomies;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "roommates")
public class RoommateEntity {

    @PrimaryKey(autoGenerate = true)
    public int id; // Unique ID for each roommate

    public String name; // Roommate's name

    public boolean owned = false;

    // --- constructor ---
    public RoommateEntity(String name) {
        this.name = name;
        this.owned = false;
    }
}
