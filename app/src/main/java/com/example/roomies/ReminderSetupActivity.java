package com.example.roomies;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import android.app.Activity;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

public class ReminderSetupActivity extends AppCompatActivity {

    private static final int REQ_CHANGE_HOUSEHOLD = 2001;
    private TextView tvRoomLinkValue;
    private EditText inputName;
    private Button confirmNameButton, continueButton, cancelButton, btnChangeHousehold;
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
        btnChangeHousehold = findViewById(R.id.btn_change_household);
        tvRoomLinkValue = findViewById(R.id.tv_room_link_value);

        // Show current link (Uri as string)
        String currentLink = SyncUtils.getRoomFileUri(this);
        if (currentLink == null || currentLink.isEmpty()) {
            tvRoomLinkValue.setText("(not linked)");
        } else {
            tvRoomLinkValue.setText(currentLink);
        }

        btnChangeHousehold.setOnClickListener(v -> onChangeHouseholdClicked());

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
                currentUserId = me.id;
                inputName.setText(me.name);
                inputName.setEnabled(false);

                nameConfirmed = true;
                setGroupEnabled(yesNoGroup, true);
                updateContinueButtonState();

                confirmNameButton.setText("Change name");
                confirmNameButton.setEnabled(true);
                confirmNameButton.setOnClickListener(v -> showChangeNameDialog());
            }
        }

        if (!nameConfirmed) {
            confirmNameButton.setOnClickListener(v -> handleNameConfirm());
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
                        long newId = db.reminderDao().insert(g);
                        g.id = (int) newId;
                        ReminderScheduler.scheduleReminder(this, g);
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
                currentUserId = me.id;
                inputName.setText(me.name);
                inputName.setEnabled(false);

                nameConfirmed = true;
                setGroupEnabled(yesNoGroup, true);
                updateContinueButtonState();

                confirmNameButton.setText("Change name");
                confirmNameButton.setEnabled(true);
                confirmNameButton.setOnClickListener(v -> showChangeNameDialog());
            }
        }

        if (!nameConfirmed) {
            confirmNameButton.setOnClickListener(v -> handleNameConfirm());
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK || data == null) {
            return;
        }

        if (requestCode == REQ_CHANGE_HOUSEHOLD) {
            Uri newUri = data.getData();
            if (newUri != null) {
                handleHouseholdSwitch(newUri);
            }
        }
    }

    private void handleHouseholdSwitch(Uri newUri) {
        RoomiesDatabase db = RoomiesDatabase.getDatabase(this);
        RoommateDao roommateDao = db.roommateDao();
        // 1) Remove myself from the current household list
        int currentUserId = UserManager.getCurrentUser(this);
        if (currentUserId != -1) {
            RoommateEntity me = roommateDao.getById(currentUserId);
            if (me != null) {
                // This is a deliberate leave, so we allow deletion even if owned
                roommateDao.delete(me);
            }

            // Clear local mapping
            UserManager.setCurrentUser(this, -1);
        }

        // 2) Push updated roommate list to the OLD household file
        //    (we have not changed the stored Uri yet)
        SyncUtils.pushIfRoomLinked(this);

        // 3) Now link to the new household file and pull its data
        SyncUtils.linkAndPullExistingRoomFile(this, newUri);

        // 4) Update the displayed link
        String newLinkString = SyncUtils.getRoomFileUri(this);
        if (newLinkString == null || newLinkString.isEmpty()) {
            tvRoomLinkValue.setText("(not linked)");
        } else {
            tvRoomLinkValue.setText(newLinkString);
        }

        // 5) Reset the UI so user can pick their name in the new household
        //    (this is simplest: restart this activity fresh)
        Intent intent = new Intent(this, ReminderSetupActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
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
        roommateDao.markOwned(currentUserId);

        UserManager.setCurrentUser(this, currentUserId);
        inputName.setEnabled(false);
        confirmNameButton.setEnabled(false);
        nameConfirmed = true;
        setGroupEnabled(yesNoGroup, true);
        updateContinueButtonState();
        SyncUtils.pushIfRoomLinked(this);

        confirmNameButton.setEnabled(false);
        confirmNameButton.setText("Saved");

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            confirmNameButton.setText("Change name");
            confirmNameButton.setEnabled(true);
            confirmNameButton.setOnClickListener(v -> showChangeNameDialog());
        }, 2000);
    }

    private void showChangeNameDialog() {
        if (currentUserId == -1) {
            Toast.makeText(this, "No current user set yet.", Toast.LENGTH_SHORT).show();
            return;
        }

        RoommateEntity current = roommateDao.getById(currentUserId);
        if (current == null) {
            Toast.makeText(this, "Current user not found in this household.", Toast.LENGTH_SHORT).show();
            return;
        }

        final EditText input = new EditText(this);
        input.setText(current.name);
        if (current.name != null) {
            input.setSelection(current.name.length());
        }

        new AlertDialog.Builder(this)
                .setTitle("Change name")
                .setMessage("Enter your new name. This may affect how chores are assigned.")
                .setView(input)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    handleNameChangeConfirmed(current, newName);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void handleNameChangeConfirmed(RoommateEntity current, String newName) {
        if (newName.isEmpty()) {
            Toast.makeText(this, "Name cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        String oldName = current.name == null ? "" : current.name.trim();
        if (oldName.equalsIgnoreCase(newName)) {
            Toast.makeText(this, "Name is unchanged.", Toast.LENGTH_SHORT).show();
            return;
        }

        RoommateEntity existing = roommateDao.getRoommateByName(newName);

        if (existing == null) {
            // --- Case (a): new name does NOT exist ---
            roommateDao.renameRoommate(current.id, newName);
            inputName.setText(newName);
            Toast.makeText(this, "Name updated to " + newName, Toast.LENGTH_SHORT).show();
            SyncUtils.pushIfRoomLinked(this);
            return;
        }

        // If we somehow hit same id, treat as unchanged
        if (existing.id == current.id) {
            Toast.makeText(this, "Name is unchanged.", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- Case (b): name already exists ---
        if (existing.owned) {
            // Existing user is protected
            Toast.makeText(
                    this,
                    "Cannot change name to \"" + newName +
                            "\" because that roommate is linked on a device.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        // Merge chores & switch device to that roommate
        new AlertDialog.Builder(this)
                .setTitle("Merge with " + newName + "?")
                .setMessage(
                        "The name \"" + newName + "\" already exists.\n\n" +
                                "If you continue:\n" +
                                "• All your chores will be moved to that roommate.\n" +
                                "• This device will use that roommate.\n" +
                                "• Your old roommate entry will be removed."
                )
                .setPositiveButton("Merge", (dialog, which) -> {
                    // Move chores
                    db.choreDao().reassignAllFromRoommate(current.id, existing.id);

                    // Mark target as owned / protected
                    roommateDao.markOwned(existing.id);

                    // Remove old roommate
                    RoommateEntity toDelete = roommateDao.getById(current.id);
                    if (toDelete != null) {
                        roommateDao.delete(toDelete);
                    }

                    // Point this device at the merged roommate
                    currentUserId = existing.id;
                    UserManager.setCurrentUser(this, existing.id);

                    inputName.setText(existing.name);
                    inputName.setEnabled(false);
                    nameConfirmed = true;
                    setGroupEnabled(yesNoGroup, true);
                    updateContinueButtonState();

                    Toast.makeText(
                            this,
                            "Switched to " + existing.name + " and merged chores.",
                            Toast.LENGTH_SHORT
                    ).show();

                    SyncUtils.pushIfRoomLinked(this);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void onChangeHouseholdClicked() {
        new AlertDialog.Builder(this)
                .setTitle("Change household")
                .setMessage("This will remove you from the current household and join a different one. Continue?")
                .setPositiveButton("Yes", (d, w) -> openNewHouseholdPicker())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openNewHouseholdPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        startActivityForResult(intent, REQ_CHANGE_HOUSEHOLD);
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