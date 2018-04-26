/*
*** Created by Andina Zahra Nabilla on 10 April 2018
*
* Active Sensor: Heartrate Monitor
* Build Gradle Version: 1.1.0
* Mobile SDK Version: 25.0.3
* Wear SDK Version: 21.1.2
*
* Done right:
* + Heartrate sensor kedetect
* + Flow berpikir harusnya udah bener
* + DeviceClient.java fixx
* + MessageReceiverService fixx
* + Nama intent jadi IntentHR
* + Data HR dan ACC muncul di logcat
* + Nama app sudah falldetection
* + Nama app sudah com.andinazn.sensordetectionv2
* + Layout mobile sesuai
* + Sending intent kayaknya udah bener (?)
* + Data accelerometer muncul di textview
* + Accelerometer berhasil dikirim dan ditampilkan di mobile
* 
* Need fixing:
* - Masih satu intent (receive multiple intent blm bisa)
* - Fragment / NavigationDrawer
* - Max 590 records (20 menit)
* - Sensor HR lebih lama dari ACC
* - Timestamp masih salah (data)
* - Timestamp masih salah (textview)
 */

package com.andinazn.sensordetectionv2;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class MainActivity extends Activity {

    private TextView timeStampTxt, hrTxt, currentXTxt, currentYTxt, currentZTxt;

    private float deltaX = 0;
    private float deltaY = 0;
    private float deltaZ = 0;
    private float lastX = 0, lastY = 0, lastZ = 0;

    private DecimalFormat _DF = new DecimalFormat();
    private int DECIMAL_PLACES = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //set the layout
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                timeStampTxt = (TextView) stub.findViewById(R.id.timeStampTxt);
                hrTxt = (TextView) stub.findViewById(R.id.hrTxt);
                currentXTxt = (TextView) stub.findViewById(R.id.currentXTxt);
                currentYTxt = (TextView) stub.findViewById(R.id.currentYTxt);
                currentZTxt = (TextView) stub.findViewById(R.id.currentZTxt);
            }
        });

        this.registerReceiver(mMessageReceiver, new IntentFilter("com.example.Broadcast"));

    }

    // handler for received Intents for the "my-event" event
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //Atur desimal
            //_DF.setMaximumFractionDigits(DECIMAL_PLACES);

            //Get timestamp
            long timeStamp = intent.getLongExtra("TIME", 0)/1000000L;
            timeStampTxt.setText(String.valueOf(getDate(timeStamp)));

            //Get accuracy
            int message2 = intent.getIntExtra("ACCR", 0);

            // Extract data included in the Intent
            //Receiving heartrate data from broadcast

            float[] message1 = intent.getFloatArrayExtra("HR");
            Log.d("Receiver", "Got message1: " + message1[0] + "Got message2: " + message2);
            int tmpHr = (int)Math.ceil(message1[0] - 0.5f);
            hrTxt.setText(String.valueOf(tmpHr));


            //Receiving accelerometer data from broadcast
            /*
            float[] message3 = intent.getFloatArrayExtra("CURRENT");

            Log.d("Receiver", "Got message3: " + message3[0] + "Got message2: " + message2);
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

            currentXTxt.setText(String.valueOf((deltaX)));
            currentYTxt.setText(String.valueOf((deltaY)));
            currentZTxt.setText(String.valueOf((deltaZ)));
            */

        }
    };


    @Override
    public void onResume() {
        super.onResume();
        // Register mMessageReceiver to receive messages.
        this.registerReceiver(mMessageReceiver, new IntentFilter("com.example.Broadcast"));

    }


    private String getDate(long timestamp){

        try{
            DateFormat sdf = new SimpleDateFormat("HH:mm");
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Jakarta"));
            Date netDate = (new Date(timestamp));
            return sdf.format(netDate);
        }
        catch(Exception ex){
            return "7:00";
        }
    }

}
