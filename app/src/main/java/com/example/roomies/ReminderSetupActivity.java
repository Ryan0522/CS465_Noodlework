package com.example.roomies;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class ReminderSetupActivity extends AppCompatActivity {

    private EditText inputName;
    private Button confirmNameButton, continueButton, cancelButton;
    private RadioGroup yesNoGroup;
    private LinearLayout daysLayout, timesLayout;
    private boolean nameConfirmed = false;
    private RoomiesDatabase db;
    private RoommateDao roommateDao;
    private int currentUserId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder_screen);

        try {
            NavBarHelper.setupBottomNav(this, "settings");
        } catch (Throwable t) {
            Toast.makeText(this, "Error setting up bottom nav", Toast.LENGTH_SHORT).show();
        }

        // --- find all views ---
        inputName = findViewById(R.id.inputName);
        confirmNameButton = findViewById(R.id.confirmNameButton);
        yesNoGroup = findViewById(R.id.yesNoGroup);
        daysLayout = findViewById(R.id.daysLayout);
        timesLayout = findViewById(R.id.timesLayout);
        continueButton = findViewById(R.id.continueButton);
        cancelButton = findViewById(R.id.cancelButton);

        // --- access database ---
        db = RoomiesDatabase.getDatabase(this);
        roommateDao = db.roommateDao();

        // --- disable everything at start ---
        setGroupEnabled(yesNoGroup, false);
        setLayoutEnabled(daysLayout, false);
        setLayoutEnabled(timesLayout, false);
        confirmNameButton.setEnabled(false);
        continueButton.setEnabled(false);
        cancelButton.setEnabled(false);

        inputName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                confirmNameButton.setEnabled(s.toString().trim().length() > 0);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // --- handle saved user (prefill) ---
        int savedId = UserManager.getCurrentUser(this);
        if (savedId != -1) {
            RoommateEntity me = roommateDao.getById(savedId);
            if (me != null) {
                inputName.setText(me.name);
                inputName.setEnabled(false);
                confirmNameButton.setEnabled(false);
                confirmNameButton.setBackgroundColor(getResources().getColor(R.color.gray));
                nameConfirmed = true;
                setGroupEnabled(yesNoGroup, true);
            }
        }

        // --- handle name confirmation ---
        confirmNameButton.setOnClickListener(v -> handleNameConfirm());

        // --- radio group gating ---
        yesNoGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (!nameConfirmed) return;
            boolean yes = (checkedId == R.id.radioYes);
            setLayoutEnabled(daysLayout, yes);
            setLayoutEnabled(timesLayout, yes);
            updateContinueButtonState();
        });

        // --- when user checks/unchecks any day/time, re-evaluate Continue ---
        attachCheckListeners(daysLayout);
        attachCheckListeners(timesLayout);

        // --- continue button: save preference and lock UI ---
        continueButton.setOnClickListener(v -> {
            boolean autoReminders = (yesNoGroup.getCheckedRadioButtonId() == R.id.radioYes);
            UserManager.setAutoRemindersEnabled(this, autoReminders);

            if (autoReminders) {
                List<String> selectedDays = getCheckedTexts(daysLayout);
                List<String> selectedTimes = getCheckedTexts(timesLayout);

                if (selectedDays.isEmpty() || selectedTimes.isEmpty()) {
                    Toast.makeText(this, "Please select at least one day and one time.", Toast.LENGTH_SHORT).show();
                    return;
                }

                UserManager.setReminderDays(this, String.join(",", selectedDays));
                UserManager.setReminderTimes(this, String.join(",", selectedTimes));

                // Delete old auto reminders
                List<ReminderEntity> allRems = db.reminderDao().getAll();
                for (ReminderEntity r : allRems) {
                    if (r.isAuto) db.reminderDao().delete(r);
                }

                // Recreate auto reminders for each of user's chores
                int currentUser = UserManager.getCurrentUser(this);
                List<ChoreEntity> chores = db.choreDao().getAll();
                for (ChoreEntity c : chores) {
                    if (c.roommateId != currentUser) continue;
                    List<ReminderEntity> generated = ReminderAutoGenerator.buildAutoRemindersForChore(
                            c, selectedDays, selectedTimes
                    );
                    for (ReminderEntity g : generated) {
                        db.reminderDao().insert(g);
                    }
                }

                Toast.makeText(this, "Auto reminders updated.", Toast.LENGTH_SHORT).show();
            } else {
                // Auto reminders off → remove all previous ones
                List<ReminderEntity> allRems = db.reminderDao().getAll();
                for (ReminderEntity r : allRems) {
                    if (r.isAuto) db.reminderDao().delete(r);
                }
                Toast.makeText(this, "Auto reminders removed.", Toast.LENGTH_SHORT).show();
            }

            // Lock UI
            setGroupEnabled(yesNoGroup, false);
            setLayoutEnabled(daysLayout, false);
            setLayoutEnabled(timesLayout, false);
            continueButton.setEnabled(false);
            cancelButton.setEnabled(true);

            Toast.makeText(this, "Settings saved!", Toast.LENGTH_SHORT).show();

            startActivity(new Intent(this, ChoresListActivity.class));
            SyncUtils.pushIfRoomLinked(this);
            finish();
        });

        // --- cancel button ---
        cancelButton.setOnClickListener(v -> {
            startActivity(new Intent(this, ChoresListActivity.class));
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // --- Restore user name ---
        int savedId = UserManager.getCurrentUser(this);
        if (savedId != -1) {
            RoommateEntity me = roommateDao.getById(savedId);
            if (me == null) {
                UserManager.setCurrentUser(this, -1);
            } else {
                inputName.setText(me.name);
                inputName.setEnabled(false);
                confirmNameButton.setEnabled(false);
                confirmNameButton.setBackgroundColor(getResources().getColor(R.color.gray));
                nameConfirmed = true;
                setGroupEnabled(yesNoGroup, true);
            }
        }

        // --- Restore auto reminder setting ---
        boolean autoReminders = UserManager.getAutoRemindersEnabled(this);
        if (autoReminders) {
            yesNoGroup.check(R.id.radioYes);
            setLayoutEnabled(daysLayout, true);
            setLayoutEnabled(timesLayout, true);
        } else {
            yesNoGroup.check(R.id.radioNo);
            setLayoutEnabled(daysLayout, false);
            setLayoutEnabled(timesLayout, false);
        }

        // --- Restore selected days and times ---
        restoreCheckedBoxes(daysLayout, UserManager.getReminderDays(this));
        restoreCheckedBoxes(timesLayout, UserManager.getReminderTimes(this));

        setGroupEnabled(yesNoGroup, nameConfirmed);
        cancelButton.setEnabled(savedId != -1);
        updateContinueButtonState();
    }

    // --- confirm name logic ---
    private void handleNameConfirm() {
        String name = inputName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
            return;
        }

        RoommateEntity existing = roommateDao.getRoommateByName(name);
        if (existing != null) {
            currentUserId = existing.id;
            Toast.makeText(this, "Welcome back, " + name + "!", Toast.LENGTH_SHORT).show();
        } else {
            RoommateEntity newRoommate = new RoommateEntity(name);
            roommateDao.insert(newRoommate);
            RoommateEntity inserted = roommateDao.getRoommateByName(name);
            currentUserId = inserted.id;
            Toast.makeText(this, "New roommate added: " + name, Toast.LENGTH_SHORT).show();
        }

        UserManager.setCurrentUser(this, currentUserId);
        inputName.setEnabled(false);
        confirmNameButton.setEnabled(false);
        nameConfirmed = true;
        setGroupEnabled(yesNoGroup, true);
        updateContinueButtonState();
        SyncUtils.pushIfRoomLinked(this);
    }

    // --- helper: validate whether Continue should be enabled ---
    private void updateContinueButtonState() {
        boolean nameOk = nameConfirmed;
        boolean radioOk = yesNoGroup.getCheckedRadioButtonId() != -1;

        boolean allOk = nameOk && radioOk;

        if (radioOk && yesNoGroup.getCheckedRadioButtonId() == R.id.radioYes) {
            boolean dayChecked = false;
            boolean timeChecked = false;

            for (int i = 0; i < daysLayout.getChildCount(); i++) {
                View v = daysLayout.getChildAt(i);
                if (v instanceof CheckBox && ((CheckBox) v).isChecked()) {
                    dayChecked = true;
                    break;
                }
            }
            for (int i = 0; i < timesLayout.getChildCount(); i++) {
                View v = timesLayout.getChildAt(i);
                if (v instanceof CheckBox && ((CheckBox) v).isChecked()) {
                    timeChecked = true;
                    break;
                }
            }

            allOk = allOk && dayChecked && timeChecked;
        }

        continueButton.setEnabled(allOk);
    }

    // --- helper: enable/disable linear layouts ---
    private void setLayoutEnabled(LinearLayout layout, boolean enabled) {
        for (int i = 0; i < layout.getChildCount(); i++) {
            View v = layout.getChildAt(i);
            v.setEnabled(enabled);
        }
    }

    // --- helper: enable/disable radio buttons ---
    private void setGroupEnabled(RadioGroup group, boolean enabled) {
        for (int i = 0; i < group.getChildCount(); i++) {
            group.getChildAt(i).setEnabled(enabled);
        }
    }

    // --- helper: attach check change listeners ---
    private void attachCheckListeners(LinearLayout layout) {
        if (layout == null) return;
        for (int i = 0; i < layout.getChildCount(); i++) {
            View v = layout.getChildAt(i);
            if (v instanceof CheckBox) {
                ((CheckBox) v).setOnCheckedChangeListener((buttonView, isChecked) -> updateContinueButtonState());
            }
        }
    }

    // --- helper: get checked checkbox texts ---
    private List<String> getCheckedTexts(LinearLayout layout) {
        List<String> checked = new ArrayList<>();
        for (int i = 0; i < layout.getChildCount(); i++) {
            View v = layout.getChildAt(i);
            if (v instanceof CheckBox && ((CheckBox) v).isChecked()) {
                checked.add(((CheckBox) v).getText().toString());
            }
        }
        return checked;
    }

    // --- helper: restore checked boxes from CSV string ---
    private void restoreCheckedBoxes(LinearLayout layout, String csv) {
        if (csv == null || csv.isEmpty()) return;
        String[] parts = csv.split(",");
        for (int i = 0; i < layout.getChildCount(); i++) {
            View v = layout.getChildAt(i);
            if (v instanceof CheckBox) {
                CheckBox cb = (CheckBox) v;
                cb.setChecked(false);
                for (String p : parts) {
                    if (cb.getText().toString().equalsIgnoreCase(p.trim())) {
                        cb.setChecked(true);
                        break;
                    }
                }
            }
        }
    }
}