package com.ee202a.attendance.seminarcheckin;
/*
    List of Shared Preferences:
    Booleans:
    1. Training: the register is in progress, training the model
    2. ifRegistered; Already registered on Server
    3. faceRegistered; Model Trained
    4. Loggedin: if the user have signed in onto the server with their credentials
    Strings:
    1. Student Name
    2. Student Number
*/

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private boolean mPermissionReady;
    private boolean backTwice;
    private SharedPreferences sharedPreferences;
    private LinearLayout main_layout;
    protected String clientId;

    Button b_go2register;
    Button btnOpenCv;

    static final String LOG_TAG = MainActivity.class.getCanonicalName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//screen always on

        main_layout = (LinearLayout) findViewById(R.id.main_layout);
        Log.d(LOG_TAG, "main activity created!!!");
        sharedPreferences = getSharedPreferences("", Context.MODE_PRIVATE);
        SharedPreferences.Editor spEditor = sharedPreferences.edit();
        spEditor.putBoolean("Training", false);
        spEditor.putBoolean("Loggedin", false);
        spEditor.commit();

        b_go2register = (Button) findViewById(R.id.b_go2register);
        btnOpenCv = (Button) findViewById(R.id.btnOpenCv);

        clientId = UUID.randomUUID().toString();
        btnOpenCv.setEnabled(false);
        b_go2register.setEnabled(false);

        AWSConnection.setMainActivity(this);
        AWSConnection.onCreateCall();


        btnOpenCv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPermissionReady) {
                    startActivity(new Intent(MainActivity.this, OpenCvRecognizeActivity.class));
                }
            }
        });

        b_go2register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sharedPreferences.getBoolean("ifRegistered", false)) {
                    Snackbar.make(main_layout, "Access Denied: You have already Registered!", Snackbar.LENGTH_SHORT).show();
                } else {
                    Intent go2Register = new Intent(MainActivity.this, RegisterActivity.class);
                    startActivity(go2Register);
                }
            }
        });

        int cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int storagePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int coarseLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        int fineLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        mPermissionReady = cameraPermission == PackageManager.PERMISSION_GRANTED
                && storagePermission == PackageManager.PERMISSION_GRANTED
                && coarseLocationPermission == PackageManager.PERMISSION_GRANTED
                && fineLocationPermission == PackageManager.PERMISSION_GRANTED;
        if (!mPermissionReady)
            requirePermissions();
    }

    private void requirePermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION}, 11);
    }

    protected void setButtonEnable() {
        if (sharedPreferences.getBoolean("ifRegistered", false)) {
            b_go2register.setEnabled(false);
            btnOpenCv.setEnabled(true);
        } else {
            b_go2register.setEnabled(true);
            btnOpenCv.setEnabled(false);
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Map<String, Integer> perm = new HashMap<>();
        perm.put(Manifest.permission.CAMERA, PackageManager.PERMISSION_DENIED);
        perm.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_DENIED);
        perm.put(Manifest.permission.ACCESS_COARSE_LOCATION, PackageManager.PERMISSION_DENIED);
        perm.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_DENIED);
        for (int i = 0; i < permissions.length; i++) {
            perm.put(permissions[i], grantResults[i]);
        }
        if (perm.get(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                && perm.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && perm.get(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && perm.get(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mPermissionReady = true;
        } else {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)
                    || !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                new AlertDialog.Builder(this)
                        .setMessage(R.string.permission_warning)
                        .setPositiveButton(R.string.dismiss, null)
                        .show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "main activity resumed!!!");
        if(sharedPreferences.getBoolean("Loggedin", false)){
            AWSConnection.resumeConnection();
        }
        setButtonEnable();
    }

    @Override
    public void onBackPressed() {
        if (backTwice) {
            Intent quitIntent = new Intent(Intent.ACTION_MAIN);
            quitIntent.addCategory(Intent.CATEGORY_HOME);
            quitIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(quitIntent);
            finish();
            System.exit(0);
        }

        Toast.makeText(MainActivity.this, "Please press BACK again to exit.", Toast.LENGTH_SHORT).show();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                backTwice = false;
            }
        }, 3000);
        backTwice = true;
    }
}
