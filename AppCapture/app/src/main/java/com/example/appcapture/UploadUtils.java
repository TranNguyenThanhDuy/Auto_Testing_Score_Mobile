package com.example.appcapture;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class UploadUtils {
    public static void uploadImage(String filePath, String serverUrl) {
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
            System.out.println("Response code: " + responseCode);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
