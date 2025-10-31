package com.example.imageblog;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NewPostActivity extends AppCompatActivity {
    private static final String TAG = "NewPostActivity";
    private ImageView imagePreview;
    private EditText etTitle, etText;
    private ProgressBar progressBar;
    private Uri imageUri;
    private TextView newPostInfo;

    private ActivityResultLauncher<String> pickImageLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_post);

        imagePreview = findViewById(R.id.imagePreview);
        etTitle = findViewById(R.id.etTitle);
        etText = findViewById(R.id.etText);
        Button btnPick = findViewById(R.id.btnPickImage);
        Button btnSubmit = findViewById(R.id.btnSubmit);
        progressBar = findViewById(R.id.progressBar);
        newPostInfo = findViewById(R.id.newPostInfo);

        ImageButton btnClose = findViewById(R.id.newPostClose);
        btnClose.setOnClickListener(v -> finish());

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                imageUri = uri;
                // Glide로 이미지 로드하고 둥근 모서리 적용
                int radiusDp = 12;
                int radiusPx = (int) (radiusDp * getResources().getDisplayMetrics().density + 0.5f);
                Glide.with(this)
                        .load(uri)
                        .apply(RequestOptions.bitmapTransform(new RoundedCorners(radiusPx)))
                        .into(imagePreview);

                // 파일 이름 표시
                String name = getDisplayName(uri);
                if (name != null) newPostInfo.setText(name);
                else newPostInfo.setText("");
            }
        });

        btnPick.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        btnSubmit.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String text = etText.getText().toString().trim();
            if (title.isEmpty() && text.isEmpty() && imageUri == null) {
                Toast.makeText(this, "제목/본문/이미지 중 하나 이상 입력하세요", Toast.LENGTH_SHORT).show();
                return;
            }
            uploadPost(title, text, imageUri, btnSubmit);
        });
    }

    private String getDisplayName(Uri uri) {
        String displayName = null;
        android.database.Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                int idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (idx != -1 && cursor.moveToFirst()) {
                    displayName = cursor.getString(idx);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "getDisplayName failed", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return displayName;
    }

    private void uploadPost(String title, String text, Uri imageUri, Button btnSubmit) {
        progressBar.setVisibility(android.view.View.VISIBLE);
        btnSubmit.setEnabled(false);

        new Thread(() -> {
            OkHttpClient client = new OkHttpClient();
            String url = "https://cwijiq.pythonanywhere.com/api_root/Post/";

            try {
                MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM)
                        .addFormDataPart("author", "1")
                        .addFormDataPart("title", title)
                        .addFormDataPart("text", text);

                if (imageUri != null) {
                    InputStream is = null;
                    try {
                        is = getContentResolver().openInputStream(imageUri);
                        if (is != null) {
                            byte[] data = toByteArray(is);

                            String mime = getContentResolver().getType(imageUri);
                            if (mime == null) mime = "application/octet-stream";
                            MediaType mediaType = MediaType.parse(mime);

                            String filename = "upload_image";
                            android.database.Cursor cursor = null;
                            try {
                                cursor = getContentResolver().query(imageUri, null, null, null, null);
                                if (cursor != null) {
                                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                                    if (nameIndex != -1 && cursor.moveToFirst()) {
                                        filename = cursor.getString(nameIndex);
                                    }
                                }
                            } catch (Exception e) {
                                Log.w(TAG, "filename lookup failed", e);
                            } finally {
                                if (cursor != null) cursor.close();
                            }

                            RequestBody fileBody = RequestBody.create(data, mediaType);
                            builder.addFormDataPart("image", filename, fileBody);
                        } else {
                            Log.w(TAG, "InputStream is null for imageUri");
                        }
                    } finally {
                        if (is != null) {
                            try { is.close(); } catch (IOException ignored) {}
                        }
                    }
                }

                RequestBody requestBody = builder.build();
                Request request = new Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .build();

                Response response = client.newCall(request).execute();
                final boolean success = response.isSuccessful();
                final String respBody = response.body() != null ? response.body().string() : "";
                response.close();

                runOnUiThread(() -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    btnSubmit.setEnabled(true);
                    if (success) {
                        Toast.makeText(NewPostActivity.this, "게시 성공", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(NewPostActivity.this, "게시 실패: " + respBody, Toast.LENGTH_LONG).show();
                    }
                });

            } catch (IOException e) {
                Log.e(TAG, "upload failed", e);
                runOnUiThread(() -> {
                    progressBar.setVisibility(android.view.View.GONE);
                    btnSubmit.setEnabled(true);
                    Toast.makeText(NewPostActivity.this, "업로드 중 오류 발생: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[16384];
        while ((nRead = input.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }
}
