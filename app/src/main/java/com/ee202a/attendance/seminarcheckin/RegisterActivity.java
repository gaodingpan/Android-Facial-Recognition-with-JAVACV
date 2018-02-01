package com.ee202a.attendance.seminarcheckin;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

/**
 * Created by dingg on 12/8/2017.
 */

public class RegisterActivity extends AppCompatActivity{
    private SharedPreferences           sharedPreferences;
    private SharedPreferences.Editor    spEditor;
    private EditText                    studentName;
    private EditText                    studentNum;
    private LinearLayout                register_layout;
    private Boolean                     backTwice;

    private String inStudentName;
    private String inStudentNum;
    private int studentNumber = -1;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        sharedPreferences = getSharedPreferences("", Context.MODE_PRIVATE);
        spEditor = sharedPreferences.edit();
        studentName = (EditText) findViewById(R.id.et_StudentName);
        studentNum = (EditText) findViewById(R.id.et_StudentNumber);
        register_layout = (LinearLayout) findViewById(R.id.register_layout);
        spEditor.putBoolean("ifRegistered", false);
        spEditor.commit();

        AWSConnection.setRegisterActivity(this);

        findViewById(R.id.b_back2Main).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!sharedPreferences.getBoolean("ifRegistered", false)) {
                    Snackbar.make(register_layout, "Registration not Complete, please hit the Register button", Snackbar.LENGTH_SHORT).show();
                } else {
//                    Intent back2Main = new Intent(RegisterActivity.this, MainActivity.class);
//                    startActivity(back2Main);
                    finish();
                }
            }
        });

        findViewById(R.id.b_trainRecognizer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Telling the OpenCV activity we need to train the model.
                spEditor.putBoolean("Training", true);
                spEditor.commit();
                Intent trainRecognizer = new Intent(RegisterActivity.this, OpenCvRecognizeActivity.class);
                startActivity(trainRecognizer);
            }
        });

        findViewById(R.id.b_register2Server).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(){
                    @Override
                    public void run() {
                        inStudentName = studentName.getText().toString();
                        inStudentNum = studentNum.getText().toString();
                        // Sanity check for if the name and student number are entered correctly;
                        if(TextUtils.isEmpty(inStudentName)) {
                            Snackbar.make(register_layout, "Please enter your Name!", Snackbar.LENGTH_SHORT).show();
                        } else if (TextUtils.isEmpty(inStudentNum)) {
                            Snackbar.make(register_layout, "Please enter your Student Number!", Snackbar.LENGTH_SHORT).show();
                        } else if (!sharedPreferences.getBoolean("faceRegistered", false)) {
                            Snackbar.make(register_layout, "Please Register your Face!", Snackbar.LENGTH_SHORT).show();
                        } else {
                            //sent information to server
                            studentNumber = Integer.parseInt(inStudentNum);
                            //API Method call goes here.
                            AWSConnection.signUp(studentNumber);
                        }
                    }
                }.start();
            }
        });
    }

    protected void isSignUp(boolean ifSignup){
        if(ifSignup /*Server Registration Okay*/) {
            spEditor.putBoolean("ifRegistered", true);
            spEditor.putBoolean("Training", false);
            spEditor.putString("StudentName", inStudentName);
            spEditor.putInt("StudentNumber", studentNumber);
            spEditor.commit();
            Snackbar.make(register_layout, "Register Successful! If you need assist, Please contact instructors.", Snackbar.LENGTH_SHORT).show();
        } else {
            Snackbar.make(register_layout, "Register unSuccessful! Please contact instructors.", Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(sharedPreferences.getBoolean("faceRegistered", false)) {
            findViewById(R.id.b_trainRecognizer).setEnabled(false);
        }
    }

//    @Override
//    public void onBackPressed() {
//        //Preventing the user from giving up sign in
//    }
}
