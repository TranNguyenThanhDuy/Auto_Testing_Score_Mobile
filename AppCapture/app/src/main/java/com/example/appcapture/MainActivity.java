package com.example.appcapture;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

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

        Button btnLogout = findViewById(R.id.btnLogout);

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();  // ✅ Đăng xuất khỏi Firebase

            Intent intent = new Intent(MainActivity.this, FLoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);  // Xóa lịch sử activity
            startActivity(intent);
            finish();  // ✅ Đóng MainActivity hiện tại
        });

        btnSend.setOnClickListener(v -> {
            String ip = editIp.getText().toString().trim();
            if (currentPhotoPath != null && !ip.isEmpty()) {
                String serverUrl = "http://" + ip + ":5000/upload";
                btnSend.setEnabled(false); // 🔒 Khóa nút khi đang gửi

                new Thread(() -> {
                    UploadUtils.uploadImage(currentPhotoPath, serverUrl, new UploadUtils.UploadCallback() {
                        @Override
                        public void onSuccess(String sbd) {
                            runOnUiThread(() -> {
                                btnSend.setEnabled(true); // 🔓 Mở lại nút
                                if (sbd == null || sbd.isEmpty()) {
                                    Toast.makeText(MainActivity.this, "SBD rỗng hoặc không hợp lệ!", Toast.LENGTH_LONG).show();
                                    return;
                                }
                                Intent intent = new Intent(MainActivity.this, ResultActivity.class);
                                intent.putExtra("sbd", sbd);
                                startActivity(intent);
                            });
                        }

                        @Override
                        public void onError(String error) {
                            runOnUiThread(() -> {
                                btnSend.setEnabled(true); // 🔓 Mở lại nút
                                Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                            });
                        }
                    });
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
            } catch (IOException ex) {
                Toast.makeText(this, "Không thể tạo file ảnh!", Toast.LENGTH_SHORT).show();
                return;
            }
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
