package com.example.icoper.myapplication;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Observable;
import java.util.Observer;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private BackgroundService backgroundService;

    // To keep track of activity's window focus
    private static boolean currentFocus;
    private static Object statusBarService;
    // To keep track of activity's foreground/background status
    private static boolean isStart = true;

    Button startLock;
    Button stopLock;
    Intent intentService;

    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startLock = (Button) findViewById(R.id.start_btn);
        startLock.setOnClickListener(this);
        stopLock = (Button) findViewById(R.id.stop_btn);
        stopLock.setOnClickListener(this);
        statusBarService =getSystemService("statusbar");
        backgroundService = new BackgroundService();
        if (intentService == null) {
            intentService = new Intent(MainActivity.this, BackgroundService.class);
        }

    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        ChangeListener changeListener = new ChangeListener();
        changeListener.addObserver(backgroundService.getBackgroundService());
        changeListener.setSomeVariable(hasFocus);
    }


    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.start_btn:
                isStart = false;
                startService(intentService);
                Toast.makeText(getApplicationContext(), "LockIsStart", Toast.LENGTH_SHORT).show();
                break;
            case R.id.stop_btn:
                stopService(intentService);
                isStart = true;
                Toast.makeText(getApplicationContext(), "LockIsStop", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    public static class BackgroundService extends IntentService implements Observer {

        private static final String LOG_TAG = "BackGroundService";

        private boolean focusChange;
        Handler collapseNotificationHandler;


        public BackgroundService getBackgroundService() {
            return this;
        }

        public BackgroundService() {
            super(LOG_TAG);
        }

        @Override
        protected void onHandleIntent(@Nullable Intent intent) {
            Log.d(LOG_TAG, "isStart");

        }

        private void collapse() {
            Log.d(LOG_TAG, "collapse is called");
            // Initialize 'collapseNotificationHandler'
            if (collapseNotificationHandler == null) {
                collapseNotificationHandler = new Handler();
            }

            if (!currentFocus && !isStart) {

                // Post a Runnable with some delay - currently set to 300 ms
                collapseNotificationHandler.postDelayed(new Runnable() {

                    @Override
                    public void run() {

                        // Use reflection to trigger a method from 'StatusBarManager'
                        Class<?> statusBarManager = null;

                        try {
                            statusBarManager = Class.forName("android.app.StatusBarManager");
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }

                        Method collapseStatusBar = null;

                        try {

                            // Prior to API 17, the method to call is 'collapse()'
                            // API 17 onwards, the method to call is `collapsePanels()`

                            if (Build.VERSION.SDK_INT > 16) {
                                collapseStatusBar = statusBarManager.getMethod("collapsePanels");
                            } else {
                                collapseStatusBar = statusBarManager.getMethod("collapse");
                            }
                        } catch (NoSuchMethodException e) {
                            e.printStackTrace();
                        }

                        collapseStatusBar.setAccessible(true);

                        try {
                            collapseStatusBar.invoke(statusBarService);
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        }

                        // Check if the window focus has been returned
                        // If it hasn't been returned, post this Runnable again
                        // Currently, the delay is 100 ms. You can change this
                        // value to suit your needs.
                        if (!currentFocus && !isStart) {
                            collapseNotificationHandler.postDelayed(this, 100L);
                        }

                    }
                }, 300L);
            }

        }

        @Override
        public void update(Observable observable, Object o) {
            Log.d(LOG_TAG, "update");
            focusChange = ((ChangeListener) observable).getSomeVariable();
            if (!focusChange) {
                collapse();
            }
        }
    }

}
