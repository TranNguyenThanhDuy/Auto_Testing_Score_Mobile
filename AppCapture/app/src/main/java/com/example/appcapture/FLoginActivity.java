package com.example.appcapture;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FLoginActivity extends AppCompatActivity {

    private EditText edEmail, edPassword;
    private Button btnLogin;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_dangnhap);

        mAuth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("allowed_users");

        edEmail = findViewById(R.id.edEmailDN);
        edPassword = findViewById(R.id.edPasswordDN);
        btnLogin = findViewById(R.id.button);

        btnLogin.setOnClickListener(v -> {
            String email = edEmail.getText().toString().trim();
            String password = edPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            // ✅ Chuyển email thành key hợp lệ (thay dấu '.' bằng ',')
            String emailKey = email.replace(".", ",");

            // 🔍 Kiểm tra xem email có trong whitelist không
            databaseRef.child(emailKey).get().addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    // ✅ Có trong danh sách → cho đăng nhập
                    mAuth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(FLoginActivity.this, MainActivity.class));
                                    finish();
                                } else {
                                    Toast.makeText(this, "Sai mật khẩu hoặc tài khoản", Toast.LENGTH_SHORT).show();
                                }
                            });
                } else {
                    // ❌ Không nằm trong allowed_users
                    Toast.makeText(this, "Tài khoản này không được phép đăng nhập", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Lỗi kết nối kiểm tra quyền truy cập", Toast.LENGTH_SHORT).show();
            });
        });
    }
}
