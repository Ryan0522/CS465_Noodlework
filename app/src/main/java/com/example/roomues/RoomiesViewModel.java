package com.example.roomues;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;

public class RoomiesViewModel extends AndroidViewModel {

    private final RoommateDao roommateDao;
    private final ChoreDao choreDao;
    public final LiveData<List<RoommateEntity>> roommates;
    public final LiveData<List<ChoreEntity>> chores;

    public RoomiesViewModel(Application app) {
        super(app);
        RoomiesDatabase db = RoomiesDatabase.getDatabase(app);
        roommateDao = db.roommateDao();
        choreDao = db.choreDao();

        roommates = roommateDao.getAllLive();
        chores = choreDao.getAllLive();
    }

    public void addRoommate(RoommateEntity r) { roommateDao.insert(r); }
    public void addChore(ChoreEntity c) { choreDao.insert(c); }
}
