package com.example.hackathon2024_ksu;


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private final int SPLASH_DISPLAY_LENGTH = 3000; // 3000 milliseconds or 3 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splashactivity); // Make sure you have a layout file named splash_screen.xml

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Create an Intent that will start the main activity.
                Intent mainIntent = new Intent(SplashActivity.this, MainActivity.class);
                SplashActivity.this.startActivity(mainIntent);
                SplashActivity.this.finish(); // Destroy the splash activity so the user can't return to it
            }
        }, SPLASH_DISPLAY_LENGTH);
    }
}
