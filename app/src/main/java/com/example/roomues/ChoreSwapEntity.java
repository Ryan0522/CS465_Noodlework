package com.example.roomues;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ForeignKey;

@Entity(
        tableName = "chore_swaps",
        foreignKeys = {
                @ForeignKey(
                        entity = ChoreEntity.class,
                        parentColumns = "id",
                        childColumns = "chore1Id",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = ChoreEntity.class,
                        parentColumns = "id",
                        childColumns = "chore2Id",
                        onDelete = ForeignKey.CASCADE
                )
        }
)
public class ChoreSwapEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;           // unique ID for this swap record

    public int weekOffset;   // 0 = current week, +1 = next week, etc.
    public int chore1Id;     // first chore involved
    public int chore2Id;     // second chore involved

    // --- Constructor ---
    public ChoreSwapEntity(int weekOffset, int chore1Id, int chore2Id) {
        this.weekOffset = weekOffset;
        this.chore1Id = chore1Id;
        this.chore2Id = chore2Id;
    }

    public ChoreSwapEntity() {
    }
}
