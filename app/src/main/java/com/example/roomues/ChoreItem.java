package com.example.roomues;

public class ChoreItem {
    private final String roommate;
    private final String chore;

    public ChoreItem(String roommate, String chore) {
        this.roommate = roommate;
        this.chore = chore;
    }

    public String getRoommate() {
        return roommate;
    }

    public String getChore() {
        return chore;
    }
}
