package com.example.roomies;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {
        RoommateEntity.class,
        ChoreEntity.class,
        ChoreSwapEntity.class,
        ReminderEntity.class
}, version = 4)
public abstract class RoomiesDatabase extends RoomDatabase {

    public abstract RoommateDao roommateDao();
    public abstract ChoreDao choreDao();
    public abstract ChoreSwapDao choreSwapDao();
    public abstract  ReminderDao reminderDao();

    private static volatile RoomiesDatabase INSTANCE;

//    // ✅ Add this executor definition
//    private static final int NUMBER_OF_THREADS = 4;
//    public static final ExecutorService databaseWriteExecutor =
//            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static RoomiesDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (RoomiesDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    RoomiesDatabase.class,
                                    "roomies_db"
                            )
                            .fallbackToDestructiveMigration()
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
