
package com.andinazn.sensordetectionv2;

import android.app.Activity;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

import org.w3c.dom.Text;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SensorService extends Service implements SensorEventListener {
    private static final String TAG = "SensorDashboard/SensorService";

    private final static int SENS_ACCELEROMETER = Sensor.TYPE_ACCELEROMETER;
    private final static int SENS_HEARTRATE = Sensor.TYPE_HEART_RATE;

    SensorManager mSensorManager;

    private Sensor mHeartrateSensor;
    ScheduledExecutorService hrScheduler;
    private DeviceClient client;
    Notification.Builder builder;

    private float deltaX = 0;
    private float deltaY = 0;
    private float deltaZ = 0;
    private float lastX = 0, lastY = 0, lastZ = 0;
    private float tmpHR = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        client = DeviceClient.getInstance(this);

        builder = new Notification.Builder(this);
        builder.setContentTitle("Fall Detection");
        builder.setContentText("Collecting heartrate and acceleration sensor data..");
        builder.setSmallIcon(R.drawable.ic_launcher);

        startForeground(1, builder.build());

        startMeasurement();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopMeasurement();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    protected void startMeasurement() {
        mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));

        if (BuildConfig.DEBUG) {
            logAvailableSensors();
        }

        Sensor accelerometerSensor = mSensorManager.getDefaultSensor(SENS_ACCELEROMETER);
        mHeartrateSensor = mSensorManager.getDefaultSensor(SENS_HEARTRATE);

        // Register the listener
        if (mSensorManager != null) {

            if (accelerometerSensor != null) {
                mSensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                Log.w(TAG, "No Accelerometer found");
            }

            if (mHeartrateSensor != null) {
                final int measurementDuration   = 30;   // Seconds
                final int measurementBreak      = 15;    // Seconds

                hrScheduler = Executors.newScheduledThreadPool(1);
                hrScheduler.scheduleAtFixedRate(
                        new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "register Heartrate Sensor");
                                mSensorManager.registerListener(SensorService.this, mHeartrateSensor, SensorManager.SENSOR_DELAY_NORMAL);

                                try {
                                    Thread.sleep(measurementDuration * 1000);
                                } catch (InterruptedException e) {
                                    Log.e(TAG, "Interrupted while waitting to unregister Heartrate Sensor");
                                }

                                Log.d(TAG, "unregister Heartrate Sensor");
                                mSensorManager.unregisterListener(SensorService.this, mHeartrateSensor);
                            }
                        }, 3, measurementDuration + measurementBreak, TimeUnit.SECONDS);
            } else {
                Log.d(TAG, "No Heartrate Sensor found");
            }

        }
    }

    private void stopMeasurement() {
        if (mSensorManager != null)
            mSensorManager.unregisterListener(this);
        if (hrScheduler != null)
            hrScheduler.shutdown();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //Log.d("Sensor", event.sensor.getType() + "," + event.accuracy + "," + event.timestamp + "," + Arrays.toString(event.values));

        if (event.sensor.getType() == SENS_HEARTRATE) {
            //client.sendSensorData(event.sensor.getType(), event.accuracy, event.timestamp, event.values);
            tmpHR = event.values[0];

            Log.d(TAG,"Broadcast HR.");
            Log.d("Sensor HR: ", event.accuracy + "," + event.timestamp + "," + String.valueOf(tmpHR));

            //Broadcasting heartrate data

            Intent intent = new Intent();
            intent.setAction("com.example.Broadcast");
            intent.putExtra("HR", event.values);
            intent.putExtra("ACCR", event.accuracy);
            intent.putExtra("TIME", event.timestamp);
            sendBroadcast(intent);

        }


        if (event.sensor.getType() == SENS_ACCELEROMETER) {
            //client.sendSensorData(event.sensor.getType(), event.accuracy, event.timestamp, event.values);

            deltaX = (lastX - event.values[0]);
            deltaY = (lastY - event.values[1]);
            deltaZ = (lastZ - event.values[2]);

            // if the change is below 2, it is just plain noise
            if (-2 < deltaX && deltaX < 2)
                deltaX = 0;
            if (-2 < deltaY && deltaY < 2)
                deltaY = 0;

            float currentacc[] = {deltaX, deltaY, deltaZ};

            Log.d(TAG,"Broadcast ACC.");
            Log.d("Sensor Acc: ", event.accuracy + "," + event.timestamp + "," + Arrays.toString(currentacc));
            //Broadcasting accelerometer data
            /*
            Intent intent = new Intent();
            intent.setAction("com.example.Broadcast");
            intent.putExtra("CURRENT", event.values);
            intent.putExtra("ACCR", event.accuracy);
            intent.putExtra("TIME", event.timestamp);
            sendBroadcast(intent);
            */
        }

        client.sendSensorData(event.sensor.getType(), event.accuracy, event.timestamp, event.values);
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /**
     * Log all available sensors to logcat
     */
    private void logAvailableSensors() {
        final List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        Log.d(TAG, "=== LIST AVAILABLE SENSORS ===");
        Log.d(TAG, String.format(Locale.getDefault(), "|%-35s|%-38s|%-6s|", "SensorName", "StringType", "Type"));
        for (Sensor sensor : sensors) {
            Log.v(TAG, String.format(Locale.getDefault(), "|%-35s|%-38s|%-6s|", sensor.getName(), sensor.getStringType(), sensor.getType()));
        }

        Log.d(TAG, "=== LIST AVAILABLE SENSORS ===");
    }
}
