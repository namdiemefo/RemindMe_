package com.reminders.reminder;

import android.app.ActionBar;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private ListView mListView;
    private RemindersDbAdapter mDbAdapter;
    private ReminderSimpleCursorAdapter mCursorAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setIcon(R.mipmap.ic_launcher);

        mListView = (ListView) findViewById(R.id.reminders_view);
        mListView.setDivider(null);
        mDbAdapter = new RemindersDbAdapter(this);
        mDbAdapter.open();


        if (savedInstanceState == null)
            //clear all data
            mDbAdapter.deleteAllReminders();
        //add some data
        insertSomeReminders();

        Cursor cursor = mDbAdapter.fetchAllReminders();

        //from columns in database
        String[] from = new String[]{
                RemindersDbAdapter.COL_CONTENT
        };

        //to the ids of views in the layout
        int[] to = new int[]{
                R.id.row_text
        };

        mCursorAdapter = new ReminderSimpleCursorAdapter(
                //CONTEXT
                MainActivity.this,
                //layout
                R.layout.reminder_row,
                //cursor
                cursor,
                //from columns defined in db
                from,
                //to the ids of views in layout
                to,
                //flag - not used
                0
        );

        //the cursor adapter (controller) is nowupdating the listview (view)
        //with data from the db (model)
        mListView.setAdapter(mCursorAdapter);
    }


        public void fireCustomDialog(final Reminder reminder){


            final Dialog dialog = new Dialog(this);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_custom);

            TextView titleView = (TextView) dialog.findViewById(R.id.custom_title);
            final EditText editText = (EditText) dialog.findViewById(R.id.editText);
            final CheckBox checkBox = (CheckBox) dialog.findViewById(R.id.checkBox);
            checkBox.setChecked(true);
            Button commitbutton = (Button) dialog.findViewById(R.id.button_commit);
            LinearLayout linearLayout = dialog.findViewById(R.id.linearlayout);
            final boolean isEditOperation = (reminder != null);

            //FOR AN EDIT
            if (isEditOperation){
                titleView.setText("EDIT REMINDER");
                checkBox.setChecked(reminder.getImportant() == 1);
                editText.setText(reminder.getContent());
                linearLayout.setBackgroundColor(getResources().getColor(R.color.blue));
            }

            commitbutton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String reminderText = editText.getText().toString();
                    if (isEditOperation){
                        Reminder reminderEdited = new Reminder(reminder.getId(), reminderText, checkBox.isChecked()? 1 : 0);
                        mDbAdapter.updateReminder(reminderEdited);
                        //FOR A NEW REMINDER
                    }else {
                        mDbAdapter.createReminder(reminderText, checkBox.isChecked());
                        mCursorAdapter.changeCursor(mDbAdapter.fetchAllReminders());
                        dialog.dismiss();
                    }
                }
            });
        Button buttoncancel = (Button) dialog.findViewById(R.id.button_cancel);
        buttoncancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });





        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int masterListPosition, long id) {

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                ListView modeListView = new ListView(MainActivity.this);
                String[] modes = new String[]{"EDIT REMINDER", "DELETE REMINDER", "SCHEDULE REMINDER"};

                final ArrayAdapter<String> modeAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, android.R.id.text1, modes);
                modeListView.setAdapter(modeAdapter);
                builder.setView(modeListView);
                final Dialog dialog = builder.create();
                dialog.show();
                modeListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                       if (position == 0) {
                           int nId = getIdFromPosition(masterListPosition);
                           Reminder reminder = mDbAdapter.fetchReminderById(nId);
                           fireCustomDialog(reminder);
                       } else if (position == 1){
                           mDbAdapter.deleteReminderById(getIdFromPosition(masterListPosition));
                           mCursorAdapter.changeCursor(mDbAdapter.fetchAllReminders());
                       } else {
                           final TimePickerDialog.OnTimeSetListener listener = new TimePickerDialog.OnTimeSetListener() {
                               @Override
                               public void onTimeSet(TimePicker timePicker, int hourOfDay, int minute) {
                                   final Calendar alarmTime = Calendar.getInstance();
                                   alarmTime.set(Calendar.HOUR, hourOfDay);
                                   alarmTime.set(Calendar.MINUTE, minute);
                                   scheduleReminder(alarmTime.getTimeInMillis(), reminder.getContent());
                                   
                               }
                           };
                           final Calendar today = Calendar.getInstance();
                           new TimePickerDialog(MainActivity.this, null, today.get(Calendar.HOUR), today.get(Calendar.MINUTE),false).show();

                       }
                       dialog.dismiss();
                    }
                });
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {}

            mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
            mListView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
                @Override
                public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {}

                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    MenuInflater inflater = getMenuInflater();
                    inflater.inflate(R.menu.menu_reminder, menu);
                    return true;
                }



                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    return false;
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.menu_item_delete_reminder:
                            for (int nC = mCursorAdapter.getCount() - 1; nC >= 0; nC--) {
                                if (mListView.isItemChecked(nC)) {
                                    mDbAdapter.deleteReminderById(getIdFromPosition(nC));

                                }
                            }
                            mode.finish();
                            mCursorAdapter.changeCursor(mDbAdapter.fetchAllReminders());
                            return true;
                    }
                    return false;

                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {
                }

                public boolean onOptionsItemSelected(MenuItem item){
                    switch (item.getItemId()){
                        case R.id.action_new:
                            fireCustomDialog(null);
                            return true;
                        case R.id.action_exit:
                            finish();
                            return true;
                            default:
                                return false;

                    }
                }
            });

    }

    private void scheduleReminder(long time, String content) {

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(this, ReminderAlarmReceiver.class);
        alarmIntent.putExtra(ReminderAlarmReceiver.REMINDER_TEXT, content);
        PendingIntent broadcast =  PendingIntent.getBroadcast(this, 0, alarmIntent, 0);
        alarmManager.set(AlarmManager.RTC_WAKEUP, time, broadcast);
    }

    private int getIdFromPosition ( int nC){
            return (int) mCursorAdapter.getItemId(nC);


        }










    private void insertSomeReminders() {
        mDbAdapter.createReminder("checkout new soundcloud music", true);
        mDbAdapter.createReminder("meet with tobe and bruno", false);
        mDbAdapter.createReminder("Dinner at Jayys on Friday", false);
        mDbAdapter.createReminder("EL CLASSICO DERBY", false);
        mDbAdapter.createReminder("call my woman", false);
        mDbAdapter.createReminder("check barca analysis", true);
        mDbAdapter.createReminder("work on proposal", false);
        mDbAdapter.createReminder("Research on African sports", false);
        mDbAdapter.createReminder("Renew membership to club", false);
        mDbAdapter.createReminder("CHECK ESPN", true);
        mDbAdapter.createReminder("CHECK TECHPOINT", false);
        mDbAdapter.createReminder("call tobe", false);
        mDbAdapter.createReminder("Call accountant about tax returns", false);
        mDbAdapter.createReminder("Buy 300,000 shares of Google", false);
        mDbAdapter.createReminder("Call the Dalai Lama back", true);
    }

}

