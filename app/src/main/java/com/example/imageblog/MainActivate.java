package com.example.imageblog;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Spanned;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

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
    TextView textView;
    RecyclerView recyclerView;
    MaterialButton btnLoad;
    MaterialButton btnSave;
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

        // 버튼 참조 (XML의 onClick은 그대로 사용)
        btnLoad = findViewById(R.id.btn_load);
        btnSave = findViewById(R.id.btn_save);

        // ...changed: 초기에는 게시글을 즉시 불러오지 않고, RecyclerView를 숨깁니다...
        recyclerView.setVisibility(View.GONE);
        textView.setText("동기화 버튼을 눌러 게시글을 불러오세요.");

        Log.d(TAG, "onCreate: 초기 상태, 자동 로드 없이 대기합니다.");
        // 자동 로드 제거: startFetch 호출 없음
    }

    public void onClickDownload(View v) {
// 수동으로 버튼 눌렀을 때 재요청
        Log.d(TAG, "onClickDownload: 버튼 눌림, 데이터 로드 시작");
        if (fetchThread != null && fetchThread.isAlive()) {
            fetchThread.interrupt();
        }
        // 로딩 시작 시 기존 목록 숨기고 상태 표시
        recyclerView.setVisibility(View.GONE);
        textView.setText("로딩 중...");
        // 중복 요청 방지: 버튼 비활성화
        if (btnLoad != null) {
            btnLoad.setEnabled(false);
            btnLoad.setAlpha(0.6f);
        }
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
                                if (!resolved.isEmpty()) {
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
            // 게시글이 없을 땐 리스트 숨김, 안내 문구 표시
            recyclerView.setVisibility(View.GONE);
            textView.setText(display);
            // 동기화 완료/실패 후 버튼 다시 활성화
            if (btnLoad != null) {
                btnLoad.setEnabled(true);
                btnLoad.setAlpha(1f);
            }
            Log.d(TAG, "onPostsFetched: 게시글 없음, rawJson shown");
        } else {
            String html = "이미지 로드 성공! &nbsp;&nbsp;&nbsp; 총 글 개수: <b><font color='#FF424242'>" + posts.size() + "개</font></b>";
            Spanned sp = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY);
            textView.setText(sp, TextView.BufferType.SPANNABLE);
            // 게시글이 있을 땐 리스트 보이고 어댑터 적용
            recyclerView.setVisibility(View.VISIBLE);
            ImageAdapter adapter = new ImageAdapter(posts);
            recyclerView.setAdapter(adapter);
            // 동기화 완료 후 버튼 다시 활성화
            if (btnLoad != null) {
                btnLoad.setEnabled(true);
                btnLoad.setAlpha(1f);
            }
            Log.d(TAG, "onPostsFetched: RecyclerView에 adapter 적용 완료");
        }
    }
}
