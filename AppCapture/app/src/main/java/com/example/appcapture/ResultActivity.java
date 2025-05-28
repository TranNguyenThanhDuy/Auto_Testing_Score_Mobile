package com.example.appcapture;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.appcapture.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.google.firebase.database.ValueEventListener;

public class ResultActivity extends AppCompatActivity {

    private TextView nameView, ncodeView, ecodeView, scoreView, commentView;
    private Button btnUpdate;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.result_layout);  // đảm bảo file XML bạn vừa hoàn tất có tên này

        // Ánh xạ các thành phần từ XML
        nameView = findViewById(R.id.name);
        ncodeView = findViewById(R.id.ncode);
        ecodeView = findViewById(R.id.ecode);
        scoreView = findViewById(R.id.score);
        commentView = findViewById(R.id.comment);
        btnUpdate = findViewById(R.id.btnUpdate);

        btnUpdate.setOnClickListener(v -> {
            // Quay về MainActivity và xóa activity hiện tại
            Intent intent = new Intent(ResultActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish(); // Đóng ResultActivity
        });


        // Nhận mã số sinh viên từ Intent
        String sbd = getIntent().getStringExtra("sbd");

        // ✅ Thông báo nếu là SBD không hợp lệ
        if (sbd != null && sbd.startsWith("unknown_")) {
            Toast.makeText(this, "⚠️ SBD không hợp lệ hoặc không rõ ràng!", Toast.LENGTH_LONG).show();
        }

        if (sbd == null || sbd.isEmpty()) {
            Toast.makeText(this, "Không có mã số sinh viên!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        // Truy vấn Firebase Realtime Database
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("SinhVien").child(sbd);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("ho_ten").getValue(String.class);
                    String ma_de = snapshot.child("ma_de").getValue(String.class);
                    String diem = String.valueOf(snapshot.child("diem").getValue());
                    String nhan_xet = snapshot.child("nhan_xet").getValue(String.class);

                    nameView.setText(name != null ? name : "(Không có)");
                    ncodeView.setText(sbd);
                    ecodeView.setText(ma_de != null ? ma_de : "(Không có)");
                    scoreView.setText(diem);
                    commentView.setText(nhan_xet != null ? nhan_xet : "(Không có)");
                } else {
                    Toast.makeText(ResultActivity.this, "Không tìm thấy dữ liệu cho SBD: " + sbd, Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ResultActivity.this, "Lỗi truy vấn Firebase: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}