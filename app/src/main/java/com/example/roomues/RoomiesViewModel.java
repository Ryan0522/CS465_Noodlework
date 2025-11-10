package com.example.roomues;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;

public class RoomiesViewModel extends AndroidViewModel {
    private final RoommateDao roommateDao;
    private final ChoreDao choreDao;
    private final LiveData<List<RoommateEntity>> allRoommates;
    private final LiveData<List<ChoreEntity>> allChores;

    public RoomiesViewModel(Application application) {
        super(application);
        RoomiesDatabase db = RoomiesDatabase.getDatabase(application);
        roommateDao = db.roommateDao();
        choreDao = db.choreDao();
        allRoommates = roommateDao.getAllRoommatesLive();
        allChores = choreDao.getAllChoresLive();
    }

    public LiveData<List<RoommateEntity>> getAllRoommates() { return allRoommates; }
    public LiveData<List<ChoreEntity>> getAllChores() { return allChores; }

    public void insertRoommate(RoommateEntity roommate) {
        RoomiesDatabase.databaseWriteExecutor.execute(() -> roommateDao.insert(roommate));
    }

    public void insertChore(ChoreEntity chore) {
        RoomiesDatabase.databaseWriteExecutor.execute(() -> choreDao.insert(chore));
    }
}
