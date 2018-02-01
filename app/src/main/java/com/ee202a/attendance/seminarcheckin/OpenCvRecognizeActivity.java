package com.ee202a.attendance.seminarcheckin;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.opencv_face;
import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_core.Point;
import org.bytedeco.javacpp.opencv_core.RectVector;
import org.bytedeco.javacpp.opencv_core.Size;
import java.io.File;

import static com.ee202a.attendance.seminarcheckin.TrainHelper.isTrained;
import static org.bytedeco.javacpp.opencv_core.FONT_HERSHEY_PLAIN;
import static org.bytedeco.javacpp.opencv_core.LINE_8;
import static org.bytedeco.javacpp.opencv_core.Mat;
import static org.bytedeco.javacpp.opencv_face.createEigenFaceRecognizer;
import static org.bytedeco.javacpp.opencv_face.createLBPHFaceRecognizer;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.cvtColor;
import static org.bytedeco.javacpp.opencv_imgproc.putText;
import static org.bytedeco.javacpp.opencv_imgproc.rectangle;
import static org.bytedeco.javacpp.opencv_imgproc.resize;
import static org.bytedeco.javacpp.opencv_objdetect.CascadeClassifier;
import static com.ee202a.attendance.seminarcheckin.TrainHelper.ACCEPT_LEVEL;

/**
 * Created by djalmaafilho.
 * Adapted by dinggp
 */
public class OpenCvRecognizeActivity extends Activity implements CvCameraPreview.CvCameraViewListener {
    public static final String          TAG = "OpenCvRecognizeActivity";

    private int                         absoluteFaceSize = 0;
    private int                         photoTaken;
    private int                         framesPassed;
    private boolean                     backTwice;
//    private boolean                     loginSuccessful;
    private boolean                     recognized;
    private CascadeClassifier           faceDetector;
    private String[]                    nomes = {"", "Y Know You"};
    private CvCameraPreview             cameraView;
    private SharedPreferences           sharedPreferences;
    private SharedPreferences.Editor    spEditor;
    private FrameLayout                 CV_layout;
    private Button                      login;
    private Button                      back2main;


    boolean                             takePhoto;
    opencv_face.FaceRecognizer          faceRecognizer = createLBPHFaceRecognizer();
    boolean                             trained;


    private boolean hasPermissions(Context context, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    public void login() {
//        boolean result = false;
        int studentNum = sharedPreferences.getInt("StudentNumber", -1);
        if (studentNum == -1) {
            Snackbar.make(CV_layout, "No Registered User Found", Snackbar.LENGTH_SHORT).show();
        } else if(recognized) {//search for bluetooth beacon
            //send info to server
            AWSConnection.signIn(studentNum, this);
        } else {
            Snackbar.make(CV_layout, "Your face is not Recognized!", Snackbar.LENGTH_SHORT).show();
        }
//        return result;
    }

    protected void loginResponse() {
        Log.d(TAG, "now we are log in successful!!! ");
        spEditor.putBoolean("Loggedin", true);
        spEditor.commit();
        Snackbar.make(CV_layout, "Sign in Successful, Please keep this program alive and go back to main page!!!", Snackbar.LENGTH_SHORT).show();
//        finish();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_opencv);
        recognized = false;
//        loginSuccessful = false;
        photoTaken = 0;
        framesPassed = 0;
        CV_layout = (FrameLayout) findViewById(R.id.cv_layout);
        login = (Button) findViewById(R.id.btPhoto);
        back2main = (Button) findViewById(R.id.btReset);

//        AWSConnection.setOpenCvRecognizeActivity(this);

        if (Build.VERSION.SDK_INT >= 23) {
            String[] PERMISSIONS = {android.Manifest.permission.READ_EXTERNAL_STORAGE,android.Manifest.permission.WRITE_EXTERNAL_STORAGE};
            if (!hasPermissions(this, PERMISSIONS)) {
                ActivityCompat.requestPermissions(this, PERMISSIONS, 1 );
            }
        }
        sharedPreferences = getSharedPreferences("", Context.MODE_PRIVATE);
        spEditor = sharedPreferences.edit();
        if(TrainHelper.isTrained(getBaseContext())) {
            spEditor.putBoolean("Training", false);
            spEditor.commit();
        }
        if(!sharedPreferences.getBoolean("Training", false)) {
            findViewById(R.id.btTrain).setEnabled(false);

//            findViewById(R.id.btReset).setEnabled(false);
            back2main.setText("Back");
        }
        cameraView = (CvCameraPreview) findViewById(R.id.camera_view);
        cameraView.setCvCameraViewListener(this);


        new AsyncTask<Void,Void,Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    faceDetector = TrainHelper.loadClassifierCascade(OpenCvRecognizeActivity.this, R.raw.frontalface);
                    if(TrainHelper.isTrained(getBaseContext())) {
                        File folder = new File(getFilesDir(), TrainHelper.TRAIN_FOLDER);
//                        File f = new File(folder, TrainHelper.EIGEN_FACES_CLASSIFIER);
                        File f = new File(folder, TrainHelper.LBPH_CLASSIFIER);
                        faceRecognizer.load(f.getAbsolutePath());
                        trained = true;
                        spEditor.putBoolean("faceRegistered", true);
                        spEditor.commit();
                    }
                }catch (Exception e) {
                    Log.d(TAG, e.getLocalizedMessage(), e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                if(sharedPreferences.getBoolean("Training", false)){
                    findViewById(R.id.btPhoto).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            takePhoto = true;
                        }
                    });
                } else {
                    login.setText("Sign In");
                    login.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            login();
                        }
                    });
                }
                findViewById(R.id.btTrain).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        train();
                    }
                });
                findViewById(R.id.btReset).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(sharedPreferences.getBoolean("Training", false)) {
                            try {
                                TrainHelper.reset(getBaseContext());
                                Toast.makeText(getBaseContext(), "Reseted with sucess.", Toast.LENGTH_SHORT).show();
                                finish();
                            } catch (Exception e) {
                                Log.d(TAG, e.getLocalizedMessage(), e);
                            }
                        }
                        else{
                            finish();
                        }
                    }
                });
            }
        }.execute();
    }

    @Override
    public void onResume() {
        super.onResume();
        recognized = false;
        if(TrainHelper.isTrained(getBaseContext())) {
            spEditor.putBoolean("Training", false);
            spEditor.commit();
        }
        if(sharedPreferences.getBoolean("faceRegistered", false)) {
            findViewById(R.id.btTrain).setEnabled(false);
            back2main.setText("Back");
//            findViewById(R.id.btReset).setEnabled(false);
            login.setText("Sign in");
            login.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    login();
                }
            });
        }
    }

    void train() {
        int remainigPhotos = TrainHelper.PHOTOS_TRAIN_QTY - TrainHelper.qtdPhotos(getBaseContext());
        if(remainigPhotos > 0) {
            Toast.makeText(getBaseContext(), "You need more pictures taken to call train: "+ remainigPhotos, Toast.LENGTH_SHORT).show();
            return;
        }else if(TrainHelper.isTrained(getBaseContext())) {
            Toast.makeText(getBaseContext(), "Already trained", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getBaseContext(), "Start train: ", Toast.LENGTH_SHORT).show();
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                try{
                    if(!TrainHelper.isTrained(getBaseContext())) {
                        TrainHelper.train(getBaseContext());
                    }
                }catch (Exception e) {
                    Log.d(TAG, e.getLocalizedMessage(), e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                try {
                    Toast.makeText(getBaseContext(), "Reseting after train - Sucess : "+ TrainHelper.isTrained(getBaseContext()), Toast.LENGTH_SHORT).show();
                    spEditor.putBoolean("faceRegistered", true);
                    spEditor.commit();
                    finish();
                }catch (Exception e) {
                    Log.d(TAG, e.getLocalizedMessage(), e);
                }
            }
        }.execute();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        absoluteFaceSize = (int) (width * 0.32f);
    }

    @Override
    public void onCameraViewStopped() {

    }

    private void capturePhoto(Mat rgbaMat) {
        if (photoTaken >= TrainHelper.PHOTOS_TRAIN_QTY) {
            takePhoto = false;
        } else {
            photoTaken++;
            try {
                TrainHelper.takePhoto(getBaseContext(), 1, TrainHelper.qtdPhotos(getBaseContext()) + 1, rgbaMat.clone(), faceDetector);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void recognize(opencv_core.Rect dadosFace, Mat grayMat, Mat rgbaMat) {
        Mat detectedFace = new Mat(grayMat, dadosFace);
        resize(detectedFace, detectedFace, new Size(TrainHelper.IMG_SIZE,TrainHelper.IMG_SIZE));

        IntPointer label = new IntPointer(1);
        DoublePointer reliability = new DoublePointer(1);
        faceRecognizer.predict(detectedFace, label, reliability);
        int prediction = label.get(0);
        double acceptanceLevel = reliability.get(0);
        String name;
        if (prediction == -1 || acceptanceLevel >= ACCEPT_LEVEL) {
            name = getString(R.string.unknown);
            //recognized = false;
        } else {
//            name = nomes[prediction] + " - " + acceptanceLevel;
            name = sharedPreferences.getString("StudentName", "Please register your name!") + ", You can sign in now.";
            recognized = true;
        }
        int x = Math.max(dadosFace.tl().x() - 10, 0);
        int y = Math.max(dadosFace.tl().y() - 10, 0);
        putText(rgbaMat, name, new Point(x, y), FONT_HERSHEY_PLAIN, 1.2, new opencv_core.Scalar(0,255,0,0));
    }

    void showDetectedFace(RectVector faces, Mat rgbaMat) {
        int x = faces.get(0).x();
        int y = faces.get(0).y();
        int w = faces.get(0).width();
        int h = faces.get(0).height();

        rectangle(rgbaMat, new Point(x, y), new Point(x + w, y + h), opencv_core.Scalar.GREEN, 2, LINE_8, 0);
    }

    void noTrainedLabel(opencv_core.Rect face, Mat rgbaMat) {
        int x = Math.max(face.tl().x() - 10, 0);
        int y = Math.max(face.tl().y() - 10, 0);
        putText(rgbaMat, "No trained or train unavailable", new Point(x, y), FONT_HERSHEY_PLAIN, 1.4, new opencv_core.Scalar(0,255,0,0));
    }

    @Override
    public Mat onCameraFrame(Mat rgbaMat) {
        if (faceDetector != null) {
            Mat greyMat = new Mat(rgbaMat.rows(), rgbaMat.cols());
            cvtColor(rgbaMat, greyMat, CV_BGR2GRAY);
            RectVector faces = new RectVector();
            faceDetector.detectMultiScale(greyMat, faces, 1.25f, 3, 1,
                    new Size(absoluteFaceSize, absoluteFaceSize),
                    new Size(4 * absoluteFaceSize, 4 * absoluteFaceSize));

            if (faces.size() == 1) {
                framesPassed  = framesPassed + 1;
                showDetectedFace(faces, rgbaMat);
                if(takePhoto) {
                    if(framesPassed % 5 == 0) {
                        capturePhoto(rgbaMat);
                        alertRemainingPhotos();
                        framesPassed = 0;
                    }
                }
                if(trained) {
                    recognize(faces.get(0), greyMat, rgbaMat);
                }else{
                    noTrainedLabel(faces.get(0), rgbaMat);
                }
            }
            greyMat.release();
        }
        return rgbaMat;
    }

    void alertRemainingPhotos() {
        runOnUiThread(() -> {
            int remainingPhotos = TrainHelper.PHOTOS_TRAIN_QTY - TrainHelper.qtdPhotos(getBaseContext());
            Snackbar.make(CV_layout, "You need more pictures taken to call train: "+ remainingPhotos, Snackbar.LENGTH_SHORT).show();
        });
    }
}
