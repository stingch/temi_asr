package com.example.temi_asr;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.robotemi.sdk.NlpResult;
import com.robotemi.sdk.Robot;
import com.robotemi.sdk.TtsRequest;
import com.robotemi.sdk.constants.SdkConstants;
import com.robotemi.sdk.listeners.OnConversationStatusChangedListener;
import com.robotemi.sdk.listeners.OnRobotReadyListener;
import com.robotemi.sdk.listeners.OnTtsVisualizerFftDataChangedListener;
import com.robotemi.sdk.listeners.OnTtsVisualizerWaveFormDataChangedListener;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements OnRobotReadyListener, Robot.NlpListener, Robot.WakeupWordListener, Robot.TtsListener,
        Robot.AsrListener, OnConversationStatusChangedListener, OnTtsVisualizerWaveFormDataChangedListener, OnTtsVisualizerFftDataChangedListener{

    public static final String ACTION_HOME_WELCOME = "home.welcome", ACTION_HOME_DANCE = "home.dance", ACTION_HOME_SLEEP = "home.sleep";
    public static final String HOME_BASE_LOCATION = "home base";
    private TextView tvLog;
    private Robot robot;

    /**
     * Setting up all the event listeners
     */
    @Override
    protected void onStart() {
        super.onStart();
        robot.addOnRobotReadyListener(this);
        robot.addNlpListener(this);
        robot.addWakeupWordListener(this);
        robot.addTtsListener(this);
        robot.addAsrListener(this);
        robot.addOnConversationStatusChangedListener(this);
        robot.addOnTtsVisualizerWaveFormDataChangedListener(this);
        robot.addOnTtsVisualizerFftDataChangedListener(this);
    }

    /**
     * Places this application in the top bar for a quick access shortcut.
     */
    @Override
    public void onRobotReady(boolean isReady) {
        if (isReady) {
            try {
                final ActivityInfo activityInfo = getPackageManager().getActivityInfo(getComponentName(), PackageManager.GET_META_DATA);
                // Robot.getInstance().onStart() method may change the visibility of top bar.
                robot.onStart(activityInfo);
            } catch (PackageManager.NameNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Removing the event listeners upon leaving the app.
     */
    @Override
    protected void onStop() {
        super.onStop();
        robot.getInstance().removeTtsListener(this);
        robot.removeOnRobotReadyListener(this);
        //robot.removeNlpListener(this);
        //robot.removeWakeupWordListener(this);
        //robot.removeTtsListener(this);
        //robot.removeAsrListener(this);
        //robot.removeOnConversationStatusChangedListener(this);
        robot.removeOnTtsVisualizerWaveFormDataChangedListener(this);
        robot.removeOnTtsVisualizerFftDataChangedListener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        robot = Robot.getInstance(); // get an instance of the robot in order to begin using its features.
        tvLog = findViewById(R.id.tvLog);
        tvLog.setMovementMethod(new ScrollingMovementMethod());
    }


    /**
     * When adding the Nlp Listener to your project you need to implement this method
     * which will listen for specific intents and allow you to respond accordingly.
     * <p>
     * See AndroidManifest.xml for reference on adding each intent.
     */
    @Override
    public void onNlpCompleted(NlpResult nlpResult) {
        //do something with nlp result. Base the action specified in the AndroidManifest.xml
        Toast.makeText(MainActivity.this, nlpResult.action, Toast.LENGTH_SHORT).show();
        switch (nlpResult.action) {
            case ACTION_HOME_WELCOME:
                robot.tiltAngle(23);
                break;

            case ACTION_HOME_DANCE:
                long t = System.currentTimeMillis();
                long end = t + 5000;
                while (System.currentTimeMillis() < end) {
                    robot.skidJoy(0F, 1F);
                }
                break;

            case ACTION_HOME_SLEEP:
                robot.goTo(HOME_BASE_LOCATION);
                break;
        }
    }


    @Override
    public void onTtsStatusChanged(@NotNull TtsRequest ttsRequest) {
        // Do whatever you like upon the status changing. after the robot finishes speaking
    }

    @Override
    public void onWakeupWord(@NotNull String wakeupWord, int direction) {
        // Do anything on wakeup. Follow, go to location, or even try creating dance moves.
        printLog("onWakeupWord", wakeupWord + ", " + direction);
    }

    @Override
    public void onConversationStatusChanged(int status, @NotNull String text) {
        printLog("Conversation", "status=" + status + ", text=" + text);
    }

    @Override
    public void onTtsVisualizerWaveFormDataChanged(@NotNull byte[] waveForm) {
        //ttsVisualizerView.updateVisualizer(waveForm);
    }

    @Override
    public void onTtsVisualizerFftDataChanged(@NotNull byte[] fft) {
        Log.d("TtsVisualizer", Arrays.toString(fft));
//        ttsVisualizerView.updateVisualizer(fft);
    }

    /**
     * If you want to cover the voice flow in Launcher OS,
     * please add following meta-data to AndroidManifest.xml.
     * <pre>
     * <meta-data
     *     android:name="com.robotemi.sdk.metadata.KIOSK"
     *     android:value="true" />
     *
     * <meta-data
     *     android:name="com.robotemi.sdk.metadata.OVERRIDE_NLU"
     *     android:value="true" />
     * <pre>
     * And also need to select this App as the Kiosk Mode App in Settings > App > Kiosk.
     *
     * @param asrResult The result of the ASR after waking up temi.
     */
    @Override
    public void onAsrResult(final @NonNull String asrResult) {
        printLog("onAsrResult", "asrResult = " + asrResult);
        try {
            Bundle metadata = getPackageManager()
                    .getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA)
                    .metaData;
            if (metadata == null) return;
            if (!robot.isSelectedKioskApp()) return;
            if (!metadata.getBoolean(SdkConstants.METADATA_OVERRIDE_NLU)) return;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return;
        }

        if (asrResult.equalsIgnoreCase("哈囉")) {
            robot.askQuestion("嗨，我是temi，我能為你做什麼？");
        } else if (asrResult.equalsIgnoreCase("唱歌") || asrResult.equalsIgnoreCase("我想唱歌")) {
            robot.speak(TtsRequest.create("沒問題", true));
            robot.finishConversation();
            Singing();
        } else if (asrResult.equalsIgnoreCase("聽歌") || asrResult.equalsIgnoreCase("聽音樂") || asrResult.equalsIgnoreCase("打開YouTube")|| asrResult.equalsIgnoreCase("開YouTube") ){
            robot.speak(TtsRequest.create("好的，正在為你打開Youtube", true));
            robot.finishConversation();
            playMusic();
        } else if (asrResult.equalsIgnoreCase("播電影")) {
            robot.speak(TtsRequest.create("Okay, please enjoy.", true));
            robot.finishConversation();
            playMovie();
        } else if (asrResult.toLowerCase().contains("返回")) {
            robot.finishConversation();
            robot.speak(TtsRequest.create("好", true));
            Back();
            printLog("返回", "return");

        } else if (asrResult.toLowerCase().contains("跟隨")) {
            robot.finishConversation();
            robot.beWithMe();
        } else if (asrResult.toLowerCase().contains("回到充電座")) {
            robot.finishConversation();
            robot.goTo("home base");
        } else {
            robot.askQuestion("抱歉我聽不懂，可以再說一次嗎？");
        }
    }

    private void playMovie() {
        // Play movie...
        printLog("onAsrResult", "Play movie...");
    }
    private void Singing() {
        // Singing songs...
        printLog("onAsrResult", "Open Karaoke app...");

        Intent activityIntent = new Intent();
        activityIntent.setAction("com.example.karaoke");//自訂的 action
        try {
            startActivity(activityIntent);
        }catch (Exception e) {
            printLog("Karaoke app", "Error...");
        }
    }

    private void playMusic() {
        // Play music...
        printLog("onAsrResult", "Play music...");
        //com.google.android.youtube
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://www.youtube.com"));
        intent.setPackage("com.google.android.youtube");
        try {
            startActivity(intent);
        }catch (Exception e) {
            printLog("playMusic", "Error...");
        }

    }

    private void printLog(String msg) {
        printLog("", msg);
    }

    private void printLog(String tag, String msg) {
        Log.d(tag, msg);
        tvLog.setGravity(Gravity.BOTTOM);
        tvLog.append(String.format("%s %s\n", "· ", msg));
    }

    public void btnClearLog(View view) {
        tvLog.setText("");
    }

    private void Back() {
        // Back
        printLog("onAsrResult", "Back to app...");

        Intent activityIntent = new Intent();
        activityIntent.setAction("com.example.temi_asr");//自訂的 action
        try {
            startActivity(activityIntent);
        }catch (Exception e) {
            printLog("temi_asr app", "Error...");
        }
    }



}