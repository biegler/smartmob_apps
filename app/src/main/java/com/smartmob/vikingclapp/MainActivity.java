package com.smartmob.vikingclapp;

import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.TextClock;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;

public class MainActivity extends Activity implements LocationListener {

    TimePicker alarmTime;
    Button activateButton;
    Button cancelButton;
    Timer t;
    boolean isNotTriggered = true;
    MediaPlayer mMediaPlayer;
    boolean hasFix = false;
    LocationManager lm;
    ReentrantLock mutex = new ReentrantLock();
    class FixInfo{
        long time_msec;
        long timestamp_msec;
    };
    FixInfo fixInfo = new FixInfo();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        lm = (LocationManager) getSystemService(LOCATION_SERVICE);


        playAssetSound("viking_clap");

        alarmTime = findViewById(R.id.timePicker1);
        alarmTime.setIs24HourView(true);
        alarmTime.setEnabled(false);

        activateButton = findViewById(R.id.buttonActivate1);
        activateButton.setEnabled(false);
        activateButton.setText("Wait for FIX");
        cancelButton = findViewById(R.id.buttonCancel1);
        cancelButton.setEnabled(false);

    }

    private void playAssetSound(String assetName) {
        try {
            AssetFileDescriptor afd = getAssets().openFd("sounds/" + assetName + ".mp3");
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mMediaPlayer.prepare();
        } catch (Exception ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10f, this);

        activateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                cancelButton.setEnabled(true);
                activateButton.setEnabled(false);

                final long setTime_msec = alarmTime.getCurrentHour()*3600000
                        + alarmTime.getCurrentMinute()*60000;

                t = new Timer();

                t.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        SystemClock systemClock;
                        if (hasFix && isNotTriggered) {
                            mutex.lock();
                            long waitTime = (setTime_msec - fixInfo.time_msec) + fixInfo.timestamp_msec;
                            mutex.unlock();
                            long  currentTimestamp = SystemClock.elapsedRealtimeNanos()/1000000;
                            if (currentTimestamp >= waitTime){
                                mMediaPlayer.start();
                                mMediaPlayer.setVolume(1,1;
                                isNotTriggered = false;
                            }
                        }

                    }
                },0,20);
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //time = SystemClock.elapsedRealtime();
                mMediaPlayer.stop();
                mMediaPlayer.release();
                playAssetSound("viking_clap");
                t.cancel();
                cancelButton.setEnabled(false);
                activateButton.setEnabled(true);
                isNotTriggered = true;
            }
        });
    }

    @Override
    public void onLocationChanged(Location location) {

        long timestampGPS = location.getTime();
        long timestampeReal = location.getElapsedRealtimeNanos()/1000000;
        Date date = new Date(timestampGPS);
        String dateString  = date.toString();
        boolean hasMESZ = dateString.contains("MESZ");
        long hour = Long.parseLong(dateString.substring(11,13),10);
        long minute = Long.parseLong(dateString.substring(14,16),10);
        long second = Long.parseLong(dateString.substring(17,19),10);
        if (hasMESZ){
            hour = hour - 1;
        }
        alarmTime.setEnabled(true);
        activateButton.setEnabled(true);
        activateButton.setText("ACTIVATE");
        mutex.lock();
        fixInfo.time_msec = hour*3600000 + minute*60000 + second*1000;
        fixInfo.timestamp_msec = timestampeReal;
        mutex.unlock();
        hasFix = true;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * android.location.LocationListener#onProviderDisabled(java.lang.String)
     */
    @Override
    public void onProviderDisabled(String provider) {

        /*
         * Diese Methode wird aufgerufen wenn/falls das GPS in den Einstellungen
         * deaktiviert ist.
         */

        /* Aufrufen der GPS-Einstellungen */
        Intent intent = new Intent(
                android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);

    }

}