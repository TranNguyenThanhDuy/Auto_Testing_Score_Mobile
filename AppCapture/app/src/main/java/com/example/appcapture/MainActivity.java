package com.example.appcapture;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.appcapture.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    Button btnCapture, btnSend;
    ImageView imageView;
    private EditText editIp;
    String currentPhotoPath;

    static final int REQUEST_IMAGE_CAPTURE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editIp = findViewById(R.id.editIp);
        btnCapture = findViewById(R.id.btnCapture);
        btnSend = findViewById(R.id.btnSend);
        imageView = findViewById(R.id.imageView);

        btnCapture.setOnClickListener(v -> dispatchTakePictureIntent());

        btnSend.setOnClickListener(v -> {
            String ip = editIp.getText().toString().trim();
            if (currentPhotoPath != null && !ip.isEmpty()) {
                String serverUrl = "http://" + ip + ":5000/upload";
                new Thread(() -> {
                    UploadUtils.uploadImage(currentPhotoPath, serverUrl);
                }).start();
            } else {
                Toast.makeText(this, "Vui lòng nhập IP và chụp ảnh trước!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) { }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        getPackageName() + ".provider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            imageView.setImageURI(Uri.fromFile(new File(currentPhotoPath)));
        }
    }
}
