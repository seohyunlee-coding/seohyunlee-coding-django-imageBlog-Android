package com.example.imageblog;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivate extends AppCompatActivity {
    private static final String TAG = "MainActivate";
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    TextView textView;
    RecyclerView recyclerView;
    String site_url = "https://cwijiq.pythonanywhere.com"; // 변경된 API 호스트
    JSONObject post_json;
    String imageUrl = null;
    CloadImage taskDownload;
    String lastRawJson = null; // 디버깅용으로 원시 JSON을 저장
    //PutPost taskUpload;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activate_main); // 레이아웃 이름 수정
        textView = findViewById(R.id.textView);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        textView.setText("초기 로딩 중...");
        Log.d(TAG, "onCreate: 시작, 자동으로 데이터 로드 시도합니다.");
        // 자동으로 데이터 로드 시도
        if (taskDownload != null && taskDownload.getStatus() == AsyncTask.Status.RUNNING) {
            taskDownload.cancel(true);
        }
        taskDownload = new CloadImage();
        taskDownload.execute(site_url + "/api/posts");
    }

    public void onClickDownload(View v) {
// 수동으로 버튼 눌렀을 때 재요청
        Log.d(TAG, "onClickDownload: 버튼 눌림, 데이터 로드 시작");
        if (taskDownload != null && taskDownload.getStatus() == AsyncTask.Status.RUNNING) {
            taskDownload.cancel(true);
        }
        textView.setText("로딩 중...");
        taskDownload = new CloadImage();
        taskDownload.execute(site_url + "/api/posts"); // 사용자 제공 엔드포인트 사용
        Toast.makeText(getApplicationContext(), "Download", Toast.LENGTH_LONG).show();
    }
    public void onClickUpload(View v) {
        //...여기에 코드 추가...
        Toast.makeText(getApplicationContext(), "Upload", Toast.LENGTH_LONG).show();
    }

    private class CloadImage extends AsyncTask<String, Integer, List<String>> {
        @Override
        protected List<String> doInBackground(String... urls) {
            List<String> urlList = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            HttpURLConnection conn = null;
            try {
                String apiUrl = urls[0];
                Log.d(TAG, "doInBackground: 호출 URL=" + apiUrl);
                URL urlAPI = new URL(apiUrl);
                conn = (HttpURLConnection) urlAPI.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                int responseCode = conn.getResponseCode();
                Log.d(TAG, "doInBackground: responseCode=" + responseCode);
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                    is.close();
                    String strJson = result.toString();
                    lastRawJson = strJson; // 저장
                    Log.d(TAG, "doInBackground: raw json=" + strJson);

                    // 1) 우선 최상위가 JSONArray인지 시도
                    JSONArray aryJson = null;
                    try {
                        aryJson = new JSONArray(strJson);
                    } catch (JSONException ex) {
                        // 2) JSONObject로 래핑된 경우(예: {"results": [...]}) 등 처리
                        try {
                            JSONObject root = new JSONObject(strJson);
                            if (root.has("results") && root.opt("results") instanceof JSONArray) {
                                aryJson = root.getJSONArray("results");
                            } else if (root.has("data") && root.opt("data") instanceof JSONArray) {
                                aryJson = root.getJSONArray("data");
                            } else {
                                // 배열을 찾기 위해 루트 객체의 값들을 순회
                                Iterator<String> keys = root.keys();
                                while (keys.hasNext() && aryJson == null) {
                                    String k = keys.next();
                                    Object v = root.opt(k);
                                    if (v instanceof JSONArray) {
                                        aryJson = (JSONArray) v;
                                        break;
                                    }
                                }
                                // 단일 객체에 직접 image 필드가 있는 경우
                                if (aryJson == null) {
                                    String singleImage = root.optString("image", "");
                                    if (!singleImage.isEmpty()) {
                                        String resolved = resolveUrl(singleImage);
                                        if (!seen.contains(resolved)) {
                                            seen.add(resolved);
                                            urlList.add(resolved);
                                        }
                                    }
                                }
                            }
                        } catch (JSONException e) {
                            Log.w(TAG, "doInBackground: JSON 파싱 실패", e);
                        }
                    }

                    // 3) aryJson가 채워졌으면 기존 로직으로 이미지 필드 추출
                    if (aryJson != null) {
                        for (int i = 0; i < aryJson.length(); i++) {
                            try {
                                JSONObject obj = aryJson.getJSONObject(i);
                                String img = obj.optString("image", "");
                                if (img.isEmpty()) {
                                    // 다른 키 이름 시도
                                    img = obj.optString("image_url", "");
                                }
                                if (img.isEmpty()) {
                                    img = obj.optString("photo", "");
                                }
                                if (!img.isEmpty()) {
                                    String resolved = resolveUrl(img);
                                    if (!seen.contains(resolved)) {
                                        seen.add(resolved);
                                        urlList.add(resolved);
                                    }
                                }
                            } catch (JSONException je) {
                                Log.w(TAG, "doInBackground: 배열 요소 파싱 실패", je);
                            }
                        }
                    }

                    // 4) 아직 URL이 없으면 응답 본문에서 http(s) URL을 정규식으로 추출 (플러백)
                    if (urlList.isEmpty() && lastRawJson != null) {
                        Pattern p = Pattern.compile("https?://[^\"'\\s,<>]+", Pattern.CASE_INSENSITIVE);
                        Matcher m = p.matcher(lastRawJson);
                        while (m.find()) {
                            String found = m.group();
                            if (!seen.contains(found)) {
                                seen.add(found);
                                urlList.add(found);
                            }
                        }
                    }

                } else {
                    Log.w(TAG, "doInBackground: HTTP 응답 코드가 OK가 아님: " + responseCode);
                }
            } catch (IOException e) {
                Log.e(TAG, "doInBackground: 예외 발생", e);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
            return urlList;
        }

        private String resolveUrl(String image) {
            String resolved = image;
            if (!image.startsWith("http")) {
                if (image.startsWith("/")) {
                    resolved = site_url + image;
                } else {
                    resolved = site_url + "/" + image;
                }
            }
            return resolved;
        }

        @Override
        protected void onPostExecute(List<String> images) {
            Log.d(TAG, "onPostExecute: images size=" + (images == null ? 0 : images.size()));
            if (images == null || images.isEmpty()) {
                String display = "불러올 이미지가 없습니다.";
                if (lastRawJson != null && !lastRawJson.isEmpty()) {
                    int max = Math.min(1000, lastRawJson.length());
                    display += "\nrawJson: " + lastRawJson.substring(0, max);
                }
                textView.setText(display);
                Log.d(TAG, "onPostExecute: 이미지 없음, rawJson shown");
            } else {
                textView.setText("이미지 로드 성공!");
                ImageAdapter adapter = new ImageAdapter(images);
                recyclerView.setAdapter(adapter);
                Log.d(TAG, "onPostExecute: RecyclerView에 adapter 적용 완료");
            }
        }
    }


 /*
 private class PutPost extends AsyncTask<String, Void, Void> {
//...여기에 코드 추가...
 } */
}
