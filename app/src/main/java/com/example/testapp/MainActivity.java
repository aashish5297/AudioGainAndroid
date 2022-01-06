package com.example.testapp;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_AUDIO_PERMISSION_CODE = 1;

    private static final int SAMPLING_RATE_IN_HZ = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE_FACTOR = 2;

    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLING_RATE_IN_HZ,
            CHANNEL_CONFIG, AUDIO_FORMAT) * BUFFER_SIZE_FACTOR;
    private final AtomicBoolean recordingInProgress = new AtomicBoolean(false);

    private AudioRecord recorder = null;
    private AudioTrack audioTrack = null;
    private Thread recordingThread = null;
    private Button startButton, stopButton, playButton;
    private File file;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        startButton = findViewById(R.id.startRecording);
        stopButton = findViewById(R.id.stopRecording);
        playButton = findViewById(R.id.playRecorded);

        stopButton.setEnabled(false);
        playButton.setEnabled(false);

        startButton.setOnClickListener((View v) -> {
                startRecording();
        });

        stopButton.setOnClickListener((View view) -> {
                stopRecording();

                /*
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;

                 */
                stopButton.setEnabled(false);
                playButton.setEnabled(true);
                startButton.setEnabled(true);

                Toast.makeText(MainActivity.this, "Recording Completed",
                        Toast.LENGTH_LONG).show();
        });

        playButton.setOnClickListener((View view) -> {
            stopButton.setEnabled(false);
            startButton.setEnabled(false);
            playRecording();
            /*
            if(mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
                try {
                    mediaPlayer.setDataSource(audioSavePathInDevice);
                    mediaPlayer.prepare();
                    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mediaPlayer) {
                            buttonPlayLastRecordAudio.setText("PLAY");
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
                buttonPlayLastRecordAudio.setText("PAUSE");
                mediaPlayer.start();
                Toast.makeText(MainActivity.this, "Recording Playing", Toast.LENGTH_LONG).show();
            } else {
                if(buttonPlayLastRecordAudio.getText().equals("PLAY")) {
                    mediaPlayer.start();
                    buttonPlayLastRecordAudio.setText("PAUSE");
                } else {
                    mediaPlayer.pause();
                    buttonPlayLastRecordAudio.setText("PLAY");
                }
            }
                     */
        });

    }

    private void playRecording() {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                                .build();

        AudioFormat audioFormat = new AudioFormat.Builder()
                                        .setEncoding(AUDIO_FORMAT)
                                        .setSampleRate(SAMPLING_RATE_IN_HZ)
                                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                                        .build();

        audioTrack = new AudioTrack(audioAttributes, audioFormat, BUFFER_SIZE,
                                AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);

        Thread playingThread = new Thread(new PlayRecordingRunnable(), "Play Recording Thread");
        playingThread.run();
    }

    private void readAudioData() {
        final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE / 2);

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        int i = 0;
        while(i != -1) {
            try {
                i = fis.read(buffer.array());
                audioTrack.write(buffer, 0, i);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            fis.close();
        }
        catch (IOException e) {
            // handle exception
        }

        audioTrack.stop();
        audioTrack.release();
        audioTrack = null;
    }

    private void stopRecording() {
        if (null != recorder) {
            recordingInProgress.set(false);
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
        }
    }

    private void startRecording() {
        if(checkPermission()) {
            recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                    SAMPLING_RATE_IN_HZ, CHANNEL_CONFIG,
                    AUDIO_FORMAT, BUFFER_SIZE);

            if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
                System.out.println("Error in recording audio");
                return;
            }
            recorder.startRecording();

            recordingInProgress.set(true);

            recordingThread = new Thread(new RecordingRunnable(), "Recording Thread");
            recordingThread.start();

            /*
            audioSavePathInDevice = ""getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();""
            audioSavePathInDevice += "/AudioRecording.3gp";

            // below method is used to initialize
            // the media recorder class
            mediaRecorder = new MediaRecorder();

            // below method is used to set the audio
            // source which we are using a mic.
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);

            // below method is used to set
            // the output format of the audio.
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

            // below method is used to set the
            // audio encoder for our recorded audio.
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            // below method is used to set the
            // output file location for our recorded audio
            mediaRecorder.setOutputFile(audioSavePathInDevice);
            try {
                // below method will prepare
                // our audio recorder class
                mediaRecorder.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
            // start method will start
            // the audio recording.
            mediaRecorder.start();
            Toast.makeText(MainActivity.this,"Recording Started", Toast.LENGTH_LONG).show();
             */
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
        } else
            requestPermission();
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

    private class RecordingRunnable implements Runnable {

        @Override
        public void run() {
            file = new File(getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/recording.pcm");
            System.out.println("Creating File"+file.getAbsolutePath());
            final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

            try (final FileOutputStream outStream = new FileOutputStream(file)) {
                while (recordingInProgress.get()) {
                    int result = recorder.read(buffer, BUFFER_SIZE);
                    if (result < 0) {
                        throw new RuntimeException("Reading of audio buffer failed: " +
                                getBufferReadFailureReason(result));
                    }
                    outStream.write(buffer.array(), 0, BUFFER_SIZE);
                    buffer.clear();
                }
            } catch (IOException e) {
                System.out.println("Encountered some error, inside IOException");
                throw new RuntimeException("Writing of recorded audio failed", e);
            }
        }

        private String getBufferReadFailureReason(int errorCode) {
            switch (errorCode) {
                case AudioRecord.ERROR_INVALID_OPERATION:
                    return "ERROR_INVALID_OPERATION";
                case AudioRecord.ERROR_BAD_VALUE:
                    return "ERROR_BAD_VALUE";
                case AudioRecord.ERROR_DEAD_OBJECT:
                    return "ERROR_DEAD_OBJECT";
                case AudioRecord.ERROR:
                    return "ERROR";
                default:
                    return "Unknown (" + errorCode + ")";
            }
        }
    }

    private class PlayRecordingRunnable implements Runnable {
        @Override
        public void run() {
            readAudioData();
        }
    }
}