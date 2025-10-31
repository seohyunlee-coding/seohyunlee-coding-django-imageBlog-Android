package com.example.imageblog;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.imageblog.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PostDetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        TextView headerTitle = findViewById(R.id.headerTitle);
        ImageButton btnClose = findViewById(R.id.btnClose);
        TextView bodyView = findViewById(R.id.detailBody);
        TextView dateView = findViewById(R.id.detailDate);
        ImageView imageView = findViewById(R.id.detailImage);
        TextView labelBody = findViewById(R.id.labelBody);

        btnClose.setOnClickListener(v -> finish());

        Intent intent = getIntent();
        if (intent != null) {
            String title = intent.getStringExtra("title");
            String text = intent.getStringExtra("text");
            String published = intent.getStringExtra("published");
            String image = intent.getStringExtra("image");

            headerTitle.setText(title == null ? "" : title);
            bodyView.setText(text == null ? "" : text);

            // 날짜를 한국어 포맷으로 변환
            if (published != null && !published.isEmpty()) {
                dateView.setText(formatDateString(published));
            } else {
                dateView.setText("");
            }

            // 이미지 로딩 - 둥근 모서리 적용
            if (image != null && !image.isEmpty()) {
                int radiusDp = 12;
                int radiusPx = (int) (radiusDp * getResources().getDisplayMetrics().density + 0.5f);
                Glide.with(this)
                        .load(image)
                        .apply(RequestOptions.bitmapTransform(new RoundedCorners(radiusPx))
                                .placeholder(android.R.drawable.ic_menu_report_image))
                        .into(imageView);
            } else {
                imageView.setImageDrawable(null);
            }

            // 본문이 비어있으면 레이블 숨기기
            if (text == null || text.trim().isEmpty()) {
                labelBody.setVisibility(View.GONE);
                bodyView.setVisibility(View.GONE);
            } else {
                labelBody.setVisibility(View.VISIBLE);
                bodyView.setVisibility(View.VISIBLE);
            }
        }
    }

    // 날짜 문자열을 "2025년 10월 9일 9:31 오전" 형식으로 변환
    private String formatDateString(String rawDate) {
        // 입력 예: 2025-10-09T09:31:00 또는 2025-10-09 09:31:00
        String patternIn = rawDate.contains("T") ? "yyyy-MM-dd'T'HH:mm:ss" : "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat inputFormat = new SimpleDateFormat(patternIn, Locale.getDefault());
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy년 M월 d일 h:mm a", Locale.KOREAN);

        try {
            Date date = inputFormat.parse(rawDate);
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return rawDate;
        }
    }
}
