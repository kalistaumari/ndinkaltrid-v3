package com.andinazn.sensordetectionv2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.opencsv.CSVWriter;
import com.andinazn.sensordetectionv2.database.DatabaseHandler;
import com.andinazn.sensordetectionv2.model.MonitorDataModel;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;


public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    private static RemoteSensorManager remoteSensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        remoteSensorManager = RemoteSensorManager.getInstance(this);
        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, PlaceholderFragment.newInstance(position + 1))
                .commit();
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        TextView hrTxt;
        TextView currentXTxt, currentYTxt, currentZTxt;
        TextView lastSyncTxt;
        Switch mSwitch;

        long lastMeasurementTime = 0L;
        boolean isRunning = false;
        boolean isStop = false;

        private float deltaX = 0;
        private float deltaY = 0;
        private float deltaZ = 0;
        private float lastX = 0, lastY = 0, lastZ = 0;
        private float tmpHR = 0;

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            mSwitch = (Switch) rootView.findViewById(R.id.hrSwitch);
            lastSyncTxt = (TextView) rootView.findViewById(R.id.lastSyncTxt);

            hrTxt = (TextView) rootView.findViewById(R.id.hrTxt);
            currentXTxt = (TextView) rootView.findViewById(R.id.currentXTxt);
            currentYTxt = (TextView) rootView.findViewById(R.id.currentYTxt);
            currentZTxt = (TextView) rootView.findViewById(R.id.currentZTxt);


            mSwitch.setOnCheckedChangeListener(checkBtnChange);
            getActivity().registerReceiver(mMessageReceiver, new IntentFilter("com.example.Broadcast"));
            Intent intent = new Intent();
            intent.setAction("com.example.Broadcast");
            intent.putExtra("START_TIME", 0L); // clear millisec time
            getActivity().sendBroadcast(intent);
            return rootView;
        }

        CompoundButton.OnCheckedChangeListener checkBtnChange = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    lastMeasurementTime = System.currentTimeMillis();
                    remoteSensorManager.startMeasurement();
                    Intent intent = new Intent();
                    intent.setAction("com.example.Broadcast1");
                    intent.putExtra("START_TIME", lastMeasurementTime); // get current millisec time
                    getActivity().sendBroadcast(intent);
                    lastSyncTxt.setText(String.valueOf(getDate(lastMeasurementTime)));
                    SharedPreferences pref = getActivity().getSharedPreferences("START_TIME", Activity.MODE_PRIVATE);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putLong("START_TIME", lastMeasurementTime);
                    editor.apply();

                } else {
                    Intent intent = new Intent();
                    intent.setAction("com.example.Broadcast1");
                    intent.putExtra("START_TIME", 0L); // clear millisec time
                    getActivity().sendBroadcast(intent);
                    lastSyncTxt.setText("");
                    remoteSensorManager.stopMeasurement();
                    measurementDataSaveConfirm(lastMeasurementTime);
                    SharedPreferences pref = getActivity().getSharedPreferences("START_TIME", Activity.MODE_PRIVATE);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putLong("START_TIME", 0L);
                    editor.apply();
                }
            }
        };

        private void measurementDataSaveConfirm(final long lastMeasurementTime) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
            alertDialogBuilder.setMessage("Do you want to save this measurement?");
            alertDialogBuilder.setPositiveButton("Save",
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            //DatabaseHandler db = new DatabaseHandler(getActivity());
                            //db.getAllUserMonitorDataByLastMeasurementTime(lastMeasurementTime);
                            saveMonitorDataToCsv(lastMeasurementTime);

                        }
                    });
            alertDialogBuilder.setNegativeButton("Not Save",
                    new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            DatabaseHandler db = new DatabaseHandler(getActivity());
                            db.deleteMeasurementDataByMeasurementTime(lastMeasurementTime);
                        }
                    });

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }

        private void saveMonitorDataToCsv(long lastMeasurementTime) {
            DatabaseHandler db = new DatabaseHandler(getActivity());
            List<MonitorDataModel> mm = db.getAllUserMonitorDataByLastMeasurementTime(lastMeasurementTime);
            CSVWriter writer = null;
            try
            {
                File direct = new File(Environment.getExternalStorageDirectory() + "/" + "non");
                if(!direct.exists())
                {
                    if(direct.mkdir())
                    {
                        //directory is created;
                    }
                }
                String fileName = getDate2(lastMeasurementTime).concat(".csv").toString();
                writer = new CSVWriter(new FileWriter(Environment.getExternalStorageDirectory().getPath().concat("/").concat("non").concat("/").concat(fileName)), ',');
                String[] header = {
                        "id",
                        "sensorUpdateTime",
                        "sensorId",
                        "userId",
                        "accuracy",
                        "measurementTime",
                        "sensorVal"};
                writer.writeNext(header);
                for (int i = 0; i < mm.size(); i++){
                    String[] entries = {
                            String.valueOf(mm.get(i).getId()),
                            mm.get(i).getSensorUpdateTime(),
                            mm.get(i).getSensorId(),
                            mm.get(i).getUserId(),
                            mm.get(i).getAccuracy(),
                            mm.get(i).getMeasurementTime(),
                            mm.get(i).getSensorVal()}; // array of packet values
                    writer.writeNext(entries);
                }
                writer.close();
                Toast.makeText(getActivity(), "Write Text File Complete.", Toast.LENGTH_SHORT).show();
            }
            catch (IOException e)
            {
                Toast.makeText(getActivity(), "Write test text file fail.", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }

        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            ((MainActivity) activity).onSectionAttached(
                    getArguments().getInt(ARG_SECTION_NUMBER));
        }

        // handler for received Intents for the "my-event" event
        private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Extract data included in the Intent
                try {

                    isRunning = intent.getBooleanExtra("IS_RUNNING",false);
                    if (!isRunning) {
                        //Thread.sleep(3000);
                        isStop = true;
                    } else {
                        isStop = false;
                    }
                    if (mSwitch != null) {
                        if (isStop) {
                            mSwitch.setChecked(isRunning);
                        }
                    }

                    int message2 = intent.getIntExtra("ACCR", 0);
                    int sensorType = intent.getIntExtra("SENSOR_TYPE", 0);

                    //Receive heartrate data

                    float[] message1 = intent.getFloatArrayExtra("HR");

                    if ((message1 != null ) && (sensorType == 21)) {
                        Log.d("Receiver", "Got HR: " + message1[0] + ". Got Accuracy: " + message2);
                        int tmpHr = (int)Math.ceil(message1[0] - 0.5f);
                        if (tmpHr > 0) {
                            //db.getAllUserMonitorDataByLastMeasurementTime(lastMeasurementTime);
                            hrTxt.setText(String.valueOf(tmpHr));
                        }
                        DatabaseHandler db = new DatabaseHandler(getActivity());
                        long timeStamp = intent.getLongExtra("TIME", 0)/1000000L;
                        lastSyncTxt.setText(String.valueOf(getDate(timeStamp)) + " / " + db.getAllUserMonitorDataByLastMeasurementTime(lastMeasurementTime).size() + " records");
                    }

                    //Receive accelerometer data
                    /*
                    float[] message3 = intent.getFloatArrayExtra("CURRENT");

                    if (sensorType == 1) {
                        float tmpX = (int)Math.ceil(message3[0]);
                        float tmpY = (int)Math.ceil(message3[1]);
                        float tmpZ = (int)Math.ceil(message3[2]);

                        deltaX = (lastX - tmpX);
                        deltaY = (lastY - tmpY);
                        deltaZ = (lastZ - tmpZ);

                        // if the change is below 2, it is just plain noise
                        if (-2 < deltaX && deltaX < 2)
                            deltaX = 0;
                        if (-2 < deltaY && deltaY < 2)
                            deltaY = 0;

                        float currentacc[] = {deltaX, deltaY, deltaZ};

                        Log.d("Receiver", "Got Accelerometer: " + Arrays.toString(currentacc) + ". Got Accuracy: " + message2);
                        currentXTxt.setText(String.valueOf((deltaX)));
                        currentYTxt.setText(String.valueOf((deltaY)));
                        currentZTxt.setText(String.valueOf((deltaZ)));

                        DatabaseHandler db = new DatabaseHandler(getActivity());
                        long timeStamp = intent.getLongExtra("TIME", 0)/1000000L;
                        lastSyncTxt.setText(String.valueOf(getDate(timeStamp)) + " / " + db.getAllUserMonitorDataByLastMeasurementTime(lastMeasurementTime).size() + " records");
                    }
                    */
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        private String getDate(long timestamp){
            try{
                DateFormat sdf = new SimpleDateFormat("dd cc HH:mm a");
                sdf.setTimeZone(TimeZone.getTimeZone("Asia/Jakarta"));
                Date netDate = (new Date(timestamp));
                return sdf.format(netDate);
            }
            catch(Exception ex){
                return "7:00";
            }
        }

        private String getDate2(long timestamp){
            try{
                DateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
                sdf.setTimeZone(TimeZone.getTimeZone("Asia/Jakarta"));
                Date netDate = (new Date(timestamp));
                return sdf.format(netDate);
            }
            catch(Exception ex){
                return "7:00";
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //BusProvider.getInstance().register(this);
        //List<Sensor> sensors = RemoteSensorManager.getInstance(this).getSensors();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //BusProvider.getInstance().unregister(this);
    }

}
