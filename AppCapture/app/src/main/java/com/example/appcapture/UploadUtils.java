package com.example.appcapture;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class UploadUtils {

    // ✅ Interface đặt ở đây
    public interface UploadCallback {
        void onSuccess(String sbd);
        void onError(String error);
    }

    public static void uploadImage(String filePath, String serverUrl, UploadCallback callback) {
        try {
            File file = new File(filePath);
            String boundary = "*****" + System.currentTimeMillis() + "*****";
            HttpURLConnection conn = (HttpURLConnection) new URL(serverUrl).openConnection();

            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
            dos.writeBytes("--" + boundary + "\r\n");
            dos.writeBytes("Content-Disposition: form-data; name=\"image\"; filename=\"" + file.getName() + "\"\r\n");
            dos.writeBytes("Content-Type: image/jpeg\r\n\r\n");

            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, read);
            }
            fis.close();

            dos.writeBytes("\r\n--" + boundary + "--\r\n");
            dos.flush();
            dos.close();

            int responseCode = conn.getResponseCode();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    responseCode == 200 ? conn.getInputStream() : conn.getErrorStream()
            ));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            String response = sb.toString();

            if (responseCode == 200) {
                try {
                    org.json.JSONObject json = new org.json.JSONObject(response);
                    String sbd = json.getString("sbd");
                    callback.onSuccess(sbd);
                } catch (Exception parseErr) {
                    callback.onError("Lỗi phân tích JSON: " + parseErr.getMessage());
                }
            } else {
                callback.onError("Lỗi từ server: " + response);
            }


        } catch (Exception e) {
            callback.onError("Lỗi kết nối: " + e.getMessage());
        }
    }

}
