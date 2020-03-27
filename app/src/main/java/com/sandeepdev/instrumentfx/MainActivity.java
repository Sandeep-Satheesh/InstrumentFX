package com.sandeepdev.instrumentfx;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    AudioEngineInterface engineInterface;
    Switch swMasterOutput, swTranspose, swDualVoice;
    AppCompatSeekBar sbTranspose;
    AppCompatRadioButton rbHigherOctave, rbLowerOctave;
    private int samplerate;
    private int buffersize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Checking permissions.
        String[] permissions = {
                Manifest.permission.RECORD_AUDIO
        };
        for (String s : permissions) {
            if (ContextCompat.checkSelfPermission(this, s) != PackageManager.PERMISSION_GRANTED) {
                // Some permissions are not granted, ask the user.
                ActivityCompat.requestPermissions(this, permissions, 0);
                return;
            }
        }

        // Got all permissions, initialize.
        initialize();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // Called when the user answers to the permission dialogs.
        if ((requestCode != 0) || (grantResults.length < 1) || (grantResults.length != permissions.length))
            return;
        boolean hasAllPermissions = true;

        for (int grantResult : grantResults)
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                hasAllPermissions = false;
                Toast.makeText(getApplicationContext(), "Please allow all permissions for the app.", Toast.LENGTH_LONG).show();
            }

        if (hasAllPermissions) initialize();
    }

    private void initialize() {

        // Get the device's sample rate and buffer size to enable
        // low-latency Android audio output, if available.
        String samplerateString = null, buffersizeString = null;
        AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            samplerateString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
            buffersizeString = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
        }
        if (samplerateString == null) samplerateString = "48000";
        if (buffersizeString == null) buffersizeString = "480";
        samplerate = Integer.parseInt(samplerateString);
        buffersize = Integer.parseInt(buffersizeString);

        engineInterface = new AudioEngineInterface(buffersize, samplerate);

        swMasterOutput = findViewById(R.id.sw_masteroutput);
        swTranspose = findViewById(R.id.sw_transpose);
        sbTranspose = findViewById(R.id.sb_transpose);
        swDualVoice = findViewById(R.id.sw_dualvoice);
        rbHigherOctave = findViewById(R.id.rb_higheroctave);
        rbLowerOctave = findViewById(R.id.rb_loweroctave);

        disableAllControls();

        swMasterOutput.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    engineInterface.startAudioOutput();
                    enableAllControls();
                } else {
                    engineInterface.stopAudioOutput();
                    disableAllControls();
                }
            }
        });

        swTranspose.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    engineInterface.transpose(sbTranspose.getProgress() - 1200);
                    enableAllControls();
                } else {
                    sbTranspose.setProgress(1200);
                    engineInterface.transpose(0);
                    disableAllControls();
                }
            }
        });

        sbTranspose.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                engineInterface.transpose(progress - 1200);
                swTranspose.setText("Transpose Audio To: " + (progress - 1200) / 100.0f);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        swDualVoice.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    rbHigherOctave.setEnabled(true);
                    rbLowerOctave.setEnabled(true);
                    if (rbLowerOctave.isChecked()) engineInterface.setDualToneMode(-1);
                    else engineInterface.setDualToneMode(1);
                } else {
                    rbHigherOctave.setEnabled(false);
                    rbLowerOctave.setEnabled(false);
                    engineInterface.setDualToneMode(0);
                }
            }
        });

        rbHigherOctave.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) engineInterface.setDualToneMode(1);
            }
        });
        rbLowerOctave.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) engineInterface.setDualToneMode(-1);
            }
        });
    }

    private void enableAllControls() {
        swTranspose.setEnabled(true);
        if (swTranspose.isChecked())
            sbTranspose.setEnabled(true);
        else sbTranspose.setEnabled(false);

        swDualVoice.setEnabled(true);
        if (swDualVoice.isChecked()) {
            rbHigherOctave.setEnabled(true);
            rbLowerOctave.setEnabled(true);
            if (rbLowerOctave.isChecked())
                engineInterface.setDualToneMode(-1);
            else if (rbHigherOctave.isChecked())
                engineInterface.setDualToneMode(1);
            else engineInterface.setDualToneMode(0);
        }
    }

    private void disableAllControls() {
        sbTranspose.setEnabled(false);
        swTranspose.setEnabled(false);
        engineInterface.transpose(0);

        swDualVoice.setEnabled(false);
        rbLowerOctave.setEnabled(false);
        rbHigherOctave.setEnabled(false);
        engineInterface.setDualToneMode(0);
    }

    @Override
    protected void onPause() {
        super.onPause();
        engineInterface.runInBackground();
    }

    @Override
    protected void onResume() {
        super.onResume();
        engineInterface.runInForeground();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        engineInterface.cleanup();
    }
}