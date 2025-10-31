package com.example.imageblog;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

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

import android.text.Html;

public class MainActivate extends AppCompatActivity {
    private static final String TAG = "MainActivate";
    TextView textView;
    RecyclerView recyclerView;
    String site_url = "https://cwijiq.pythonanywhere.com"; // 변경된 API 호스트
    Thread fetchThread;
    String lastRawJson = null; // 디버깅용으로 원시 JSON을 저장
    //PutPost taskUpload;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activate_main); // 레이아웃 이름 수정

        // Toolbar를 레이아웃에서 찾아서 지원 액션바로 설정
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                // 기본 타이틀은 숨기고 커스텀 TextView로 대체
                getSupportActionBar().setDisplayShowTitleEnabled(false);
            }
        }

        textView = findViewById(R.id.textView);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        textView.setText("초기 로딩 중...");
        Log.d(TAG, "onCreate: 시작, 자동으로 데이터 로드 시도합니다.");
        // 자동으로 데이터 로드 시도
        if (fetchThread != null && fetchThread.isAlive()) {
            fetchThread.interrupt();
        }
        startFetch(site_url + "/api/posts");
    }

    public void onClickDownload(View v) {
// 수동으로 버튼 눌렀을 때 재요청
        Log.d(TAG, "onClickDownload: 버튼 눌림, 데이터 로드 시작");
        if (fetchThread != null && fetchThread.isAlive()) {
            fetchThread.interrupt();
        }
        textView.setText("로딩 중...");
        startFetch(site_url + "/api/posts"); // 사용자 제공 엔드포인트 사용
        Toast.makeText(getApplicationContext(), "Download", Toast.LENGTH_LONG).show();
    }
    public void onClickUpload(View v) {
        //...여기에 코드 추가...
        Toast.makeText(getApplicationContext(), "Upload", Toast.LENGTH_LONG).show();
    }

    // startFetch: 백그라운드 스레드에서 API 호출 및 파싱 수행
    private void startFetch(final String apiUrl) {
        fetchThread = new Thread(() -> {
            List<Post> postList = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            HttpURLConnection conn = null;
            try {
                Log.d(TAG, "startFetch: 호출 URL=" + apiUrl);
                URL urlAPI = new URL(apiUrl);
                conn = (HttpURLConnection) urlAPI.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                int responseCode = conn.getResponseCode();
                Log.d(TAG, "startFetch: responseCode=" + responseCode);
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder result = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                        if (Thread.currentThread().isInterrupted()) {
                            // 중단 신호
                            reader.close();
                            return;
                        }
                    }
                    is.close();
                    String strJson = result.toString();
                    lastRawJson = strJson; // 저장
                    Log.d(TAG, "startFetch: raw json=" + strJson);

                    JSONArray aryJson = null;
                    try {
                        aryJson = new JSONArray(strJson);
                    } catch (JSONException ex) {
                        try {
                            JSONObject root = new JSONObject(strJson);
                            if (root.has("results") && root.opt("results") instanceof JSONArray) {
                                aryJson = root.getJSONArray("results");
                            } else if (root.has("data") && root.opt("data") instanceof JSONArray) {
                                aryJson = root.getJSONArray("data");
                            } else {
                                Iterator<String> keys = root.keys();
                                while (keys.hasNext()) {
                                    String k = keys.next();
                                    Object v = root.opt(k);
                                    if (v instanceof JSONArray) {
                                        aryJson = (JSONArray) v;
                                        break;
                                    }
                                }
                                if (aryJson == null) {
                                    String author = root.optString("author", "");
                                    String title = root.optString("title", "");
                                    String text = root.optString("text", root.optString("body", ""));
                                    String published = root.optString("published_date", root.optString("published", ""));
                                    String img = root.optString("image", "");
                                    if (img.isEmpty()) img = root.optString("image_url", "");
                                    if (img.isEmpty()) img = root.optString("photo", "");
                                    String resolved = img.isEmpty() ? "" : resolveUrl(img);
                                    Post p = new Post(author, title, text, published, resolved);
                                    postList.add(p);
                                }
                            }
                        } catch (JSONException e) {
                            Log.w(TAG, "startFetch: JSON 파싱 실패", e);
                        }
                    }

                    if (aryJson != null) {
                        for (int i = 0; i < aryJson.length(); i++) {
                            if (Thread.currentThread().isInterrupted()) return;
                            try {
                                JSONObject obj = aryJson.getJSONObject(i);
                                String author = obj.optString("author", "");
                                String title = obj.optString("title", "");
                                String text = obj.optString("text", obj.optString("body", ""));
                                String published = obj.optString("published_date", obj.optString("published", ""));
                                String img = obj.optString("image", "");
                                if (img.isEmpty()) img = obj.optString("image_url", "");
                                if (img.isEmpty()) img = obj.optString("photo", "");
                                String resolved = img.isEmpty() ? "" : resolveUrl(img);
                                if (!resolved.isEmpty() && !seen.contains(resolved)) {
                                    seen.add(resolved);
                                }
                                Post p = new Post(author, title, text, published, resolved);
                                postList.add(p);
                            } catch (JSONException je) {
                                Log.w(TAG, "startFetch: 배열 요소 파싱 실패", je);
                            }
                        }
                    }

                    if (postList.isEmpty() && lastRawJson != null) {
                        Pattern p = Pattern.compile("https?://[^\"'\\s,<>]+", Pattern.CASE_INSENSITIVE);
                        Matcher m = p.matcher(lastRawJson);
                        if (m.find()) {
                            String found = m.group();
                            if (!seen.contains(found)) {
                                Post p0 = new Post("", "", "", "", found);
                                postList.add(p0);
                            }
                        }
                    }

                } else {
                    Log.w(TAG, "startFetch: HTTP 응답 코드가 OK가 아님: " + responseCode);
                }
            } catch (IOException e) {
                Log.e(TAG, "startFetch: 예외 발생", e);
            } finally {
                if (conn != null) conn.disconnect();
            }

            // UI 업데이트
            final List<Post> finalPosts = postList;
            runOnUiThread(() -> onPostsFetched(finalPosts));
        });

        fetchThread.start();
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

    private void onPostsFetched(List<Post> posts) {
        Log.d(TAG, "onPostsFetched: posts size=" + (posts == null ? 0 : posts.size()));
        if (posts == null || posts.isEmpty()) {
            String display = "불러올 게시글이 없습니다.";
            if (lastRawJson != null && !lastRawJson.isEmpty()) {
                int max = Math.min(1000, lastRawJson.length());
                display += "\nrawJson: " + lastRawJson.substring(0, max);
            }
            textView.setText(display);
            Log.d(TAG, "onPostsFetched: 게시글 없음, rawJson shown");
        } else {
            // Show '동기화의 인포: X개' with the count highlighted in dark gray
            String coloredCount = "<b><font color='@color/purple_500'>" + posts.size() + "개</font></b>";
            // Html doesn't resolve @color, so use hex from colors.xml (purple_500 = #FF6200EE)
            String html = "이미지 로드 성공!&nbsp;&nbsp;&nbsp; 총 글 개수: <b><font color='#FF424242'>" + posts.size() + "개</font></b>";
            textView.setText(Html.fromHtml(html), TextView.BufferType.SPANNABLE);
            ImageAdapter adapter = new ImageAdapter(posts);
            recyclerView.setAdapter(adapter);
            Log.d(TAG, "onPostsFetched: RecyclerView에 adapter 적용 완료");
        }
    }
}
