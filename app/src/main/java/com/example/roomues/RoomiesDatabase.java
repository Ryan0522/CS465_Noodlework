package com.example.roomues;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {RoommateEntity.class, ChoreEntity.class}, version = 1, exportSchema = false)
public abstract class RoomiesDatabase extends RoomDatabase {

    public abstract RoommateDao roommateDao();
    public abstract ChoreDao choreDao();

    private static volatile RoomiesDatabase INSTANCE;

    // ✅ Add this executor definition
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public static RoomiesDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (RoomiesDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    RoomiesDatabase.class,
                                    "roomies_db"
                            )
                            // temporary: allow main-thread access for testing
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
