package com.example.testapp;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_AUDIO_PERMISSION_CODE = 1;

    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;

    private Button startButton, stopButton, playButton;
    private EditText audioGain;
    private File file;



    private AudioTrack  audioTrack = null;
    private Thread playingThread = null;
    private static int bufferSize;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);

        startButton = findViewById(R.id.startRecording);
        stopButton = findViewById(R.id.stopRecording);
        playButton = findViewById(R.id.playRecorded);
        audioGain = findViewById(R.id.audioGain);

        stopButton.setEnabled(false);
        playButton.setEnabled(false);

        startButton.setOnClickListener((View v) -> {
                startRecording();
        });

        stopButton.setOnClickListener((View view) -> {
                stopRecording();

                stopButton.setEnabled(false);
                playButton.setEnabled(true);
                startButton.setEnabled(true);
                audioGain.setEnabled(true);

                Toast.makeText(MainActivity.this, "Recording Completed",
                        Toast.LENGTH_LONG).show();
        });

        playButton.setOnClickListener((View view) -> {
            stopButton.setEnabled(false);
            startButton.setEnabled(false);
            playButton.setEnabled(false);
            playingThread = new Thread(() -> playRecording(), "Playing Thread");
            playingThread.start();
            playButton.setEnabled(true);
        });
    }

    private void playRecording() {
        try {
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, RECORDER_SAMPLERATE, AudioFormat.CHANNEL_OUT_MONO,
                    RECORDER_AUDIO_ENCODING, bufferSize, AudioTrack.MODE_STREAM);
            if (audioTrack == null)
                return;

            int count = 512 * 1024;

            //Reading the file..
            byte[] byteData = null;
            File file = null;
            file = new File(getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/recording.pcm");

            byteData = new byte[(int) count];
            FileInputStream in = null;
            try {
                in = new FileInputStream(file);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            int bytesread = 0, ret = 0;
            int size = (int) file.length();
            audioTrack.play();
            while (bytesread < size) {
                ret = in.read(byteData, 0, count);
                if (ret != -1) {
                    // Write the byte array to the track
                    audioTrack.write(byteData, 0, ret);
                    bytesread += ret;
                } else
                    break;
            }
            in.close();
            audioTrack.stop();
            audioTrack.release();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void stopRecording() {
        if (null != recorder) {
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
        }
    }

    int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 2; // 2 bytes in 16bit format

    private void startRecording() {
        if(checkPermission()) {

            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                    RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);

            if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
                System.out.println("Error in recording audio");
                return;
            }
            recorder.startRecording();
            isRecording = true;

            recordingThread = new Thread(() -> writeDataToFile(), "AudioRecorder Thread");
            recordingThread.start();

            startButton.setEnabled(false);
            stopButton.setEnabled(true);
            audioGain.setEnabled(false);
        } else
            requestPermission();
    }

    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;
    }

    private void writeDataToFile() {
        String filePath = getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/recording.pcm";
        short sData[] = new short[BufferElements2Rec];
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        while (isRecording) {
            // gets the voice output from microphone to byte format

            int numRead = recorder.read(sData, 0, BufferElements2Rec);
            System.out.println("Short writing to file" + sData.toString());
            int audioGainValue = Integer.parseInt(audioGain.getText().toString());
            audioGainValue = (audioGainValue < 1)? 1 : audioGainValue;
            if (numRead > 0) {
                for (int i = 0; i < numRead; ++i) {
                    sData[i] = (short)Math.min((int)(sData[i] * audioGainValue), (int)Short.MAX_VALUE);
                }
            }
            try {
                // // writes the data to file from buffer
                // // stores the voice buffer
                byte bData[] = short2byte(sData);
                os.write(bData, 0, BufferElements2Rec * BytesPerElement);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(MainActivity.this, new
                String[]{WRITE_EXTERNAL_STORAGE, RECORD_AUDIO, READ_EXTERNAL_STORAGE}, REQUEST_AUDIO_PERMISSION_CODE );
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_AUDIO_PERMISSION_CODE:
                if (grantResults.length > 0) {
                    boolean StoragePermission = grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED;
                    boolean RecordPermission = grantResults[1] ==
                            PackageManager.PERMISSION_GRANTED;

                    if (StoragePermission && RecordPermission) {
                        Toast.makeText(MainActivity.this, "Permission Granted",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Permission Denied", Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }

    public boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(),
                WRITE_EXTERNAL_STORAGE);
        int result1 = ContextCompat.checkSelfPermission(getApplicationContext(),
                RECORD_AUDIO);
        int result2 = ContextCompat.checkSelfPermission(getApplicationContext(),
                READ_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED &&
                result1 == PackageManager.PERMISSION_GRANTED &&
                result2 == PackageManager.PERMISSION_GRANTED;
    }
}