package com.example.hackathon2024_ksu;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.media.Image;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity implements ImageAnalysis.Analyzer{

    private static final int REQUEST_PERMISSION_CODE = 200;
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private TextView textViewSpeech;
    private SpeechRecognizer speechRecognizer;
    private boolean isListening = false; // Tracks speech recognition state
    private ProcessCameraProvider cameraProvider; // Holds an instance of the camera provider
    private FaceDetector detector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        textViewSpeech = findViewById(R.id.textViewSpeech);
        Button btnCapture = findViewById(R.id.btnCapture);

        FaceDetectorOptions realTimeOpts =
                new FaceDetectorOptions.Builder()
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                        .build();
        detector = FaceDetection.getClient(realTimeOpts);

        initializeSpeechRecognizer();

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                // Initially, don't bind camera preview here
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Error starting camera " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));

        btnCapture.setOnClickListener(v -> {
            if (allPermissionsGranted()) {
                toggleListening(); // Only toggle speech recognition
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, REQUEST_PERMISSION_CODE);
            }
        });
    }

    private void initializeSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        speechRecognizer.setRecognitionListener(new RecognitionListenerAdapter() {
            @Override
            public void onError(int error) {
                // Don't display error messages
                // Continue listening for speech after an error occurs
                if (isListening) {
                    speechRecognizer.startListening(speechRecognizerIntent);
                }
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null) {
                    String text = matches.get(0);
                    textViewSpeech.setText(text);
                }
                // Continue listening for speech
                if (isListening) {
                    speechRecognizer.startListening(speechRecognizerIntent);
                }
            }

            @Override
            public void onEndOfSpeech() {
                // This method is called when speech input is complete
                // Start listening again
                if (isListening) {
                    speechRecognizer.startListening(speechRecognizerIntent);
                }
            }
        });
    }

    private void toggleListening() {
        if (!isListening) {
            if (speechRecognizer != null) {
                speechRecognizer.startListening(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH));
                isListening = true;
                bindCameraPreview(); // Bind camera preview when speech recognition starts
            }
        } else {
            if (speechRecognizer != null) {
                speechRecognizer.stopListening();
                isListening = false;
                unbindCameraPreview(); // Unbind camera preview when speech recognition stops
            }
        }
    }
    private void bindCameraPreview() {
        if (cameraProvider != null) {
            Preview preview = new Preview.Builder().build();
            ImageAnalysis.Builder builder = new ImageAnalysis.Builder();
            ImageAnalysis analysis = builder.setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
            analysis.setAnalyzer(ContextCompat.getMainExecutor(this), this);
            CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
            preview.setSurfaceProvider(previewView.getSurfaceProvider());
            cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, analysis);
        }
    }

    private void unbindCameraPreview() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll(); // Stop camera preview
        }
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE && allPermissionsGranted()) {
            toggleListening();
        } else {
            Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }


    // A simple adapter class to reduce boilerplate code in the main class
    private static abstract class RecognitionListenerAdapter implements RecognitionListener {
        @Override
        public void onReadyForSpeech(Bundle params) {}
        @Override
        public void onBeginningOfSpeech() {}
        @Override
        public void onRmsChanged(float rmsdB) {}
        @Override
        public void onBufferReceived(byte[] buffer) {}
        @Override
        public void onEndOfSpeech() {}
        @Override
        public void onPartialResults(Bundle partialResults) {}
        @Override
        public void onEvent(int eventType, Bundle params) {}
    }

    private void moveTextView(float x, float y){
        textViewSpeech.measure(0,0);
        textViewSpeech.setX((float) (x - (textViewSpeech.getMeasuredWidth() / 2.0) + 100));
        textViewSpeech.setY(y);
    }


    @OptIn(markerClass = ExperimentalGetImage.class)
    @Override
    public void analyze(ImageProxy imageProxy) {
        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
            detector.process(image)
                    .addOnSuccessListener(
                            faces -> {
                                for (Face face: faces){
                                    List <PointF> location = face.getContour(FaceContour.FACE).getPoints();
                                    moveTextView((location.get(0).x),location.get(0).y);
                                }
                            })
                    .addOnFailureListener(
                            e -> {
                                // Task failed with an exception
                                // ...
                            })
                    .addOnCompleteListener(results -> imageProxy.close());

        }
    }
}
