package com.load.big.bitmap;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private InputStream is = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BigImageView bigView = findViewById(R.id.big_image_view);
        try {
            is = getAssets().open("single.jpg");
            bigView.setImage(is);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
