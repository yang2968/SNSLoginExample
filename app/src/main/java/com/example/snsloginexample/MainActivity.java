package com.example.snsloginexample;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.nhn.android.naverlogin.OAuthLogin;
import com.nhn.android.naverlogin.OAuthLoginHandler;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private OAuthLogin oAuthLogin;
    private Context context;
    private LinearLayout googleLogin , kakaoLogin, naverLogin;
    private TextView googleLogout, kakaoLogout, naverLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();

        googleLogin = findViewById(R.id.googleLogin);
        googleLogout = findViewById(R.id.googleLogout);
        kakaoLogin = findViewById(R.id.kakaoLogin);
        kakaoLogout = findViewById(R.id.kakaoLogout);
        naverLogin = findViewById(R.id.naverLogin);
        naverLogout = findViewById(R.id.naverLogout);
        // 네이버 로그인
        naverLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                oAuthLogin = OAuthLogin.getInstance();
                oAuthLogin.init(
                        context
                        ,getString(R.string.naver_client_id)
                        ,getString(R.string.naver_client_secret)
                        ,getString(R.string.naver_client_name)
                );

                OAuthLoginHandler oAuthLoginHandler = new OAuthLoginHandler() {
                    @Override
                    public void run(boolean success) {
                        if (success) { // 로그인 성공
                            String accessToken = oAuthLogin.getAccessToken(context); // 토큰 가져오기
                            String refreshToken = oAuthLogin.getRefreshToken(context);
                            long expiresAt = oAuthLogin.getExpiresAt(context);
                            String tokenType = oAuthLogin.getTokenType(context);
//                            Log.i("LoginData","accessToken : "+ accessToken);
//                            Log.i("LoginData","refreshToken : "+ refreshToken);
//                            Log.i("LoginData","expiresAt : "+ expiresAt);
//                            Log.i("LoginData","tokenType : "+ tokenType);
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    String header = "Bearer " +  accessToken;
                                    Map<String, String> requestHeaders = new HashMap<>();
                                    requestHeaders.put("Authorization", header);
                                    String apiURL = "https://openapi.naver.com/v1/nid/me"; //엑세스 토큰으로 유저정보를 받아올 주소
                                    String responseBody = get(apiURL,requestHeaders);
                                    naverUserInfo(responseBody);
                                }
                            }).start();

                        } else { // 로그인 실패
                            String errorCode = oAuthLogin
                                    .getLastErrorCode(context).getCode();
                            String errorDesc = oAuthLogin.getLastErrorDesc(context);
//                            Toast.makeText(context, "errorCode:" + errorCode
//                                    + ", errorDesc:" + errorDesc, Toast.LENGTH_SHORT).show();
                            Toast.makeText(MainActivity.this, "로그인 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                        }
                    }
                };

                oAuthLogin.startOauthLoginActivity(MainActivity.this, oAuthLoginHandler);
            }
        });
        // 네이버 로그아웃
        naverLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    AlertDialog.Builder logoutDialog = new AlertDialog.Builder(MainActivity.this);
                    logoutDialog.setTitle("알림");
                    logoutDialog.setMessage("로그아웃 하시겠습니까?");
                    logoutDialog.setNegativeButton(" 취소", null);
                    logoutDialog.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            oAuthLogin.logout(context);
                            naverLogout.setVisibility(View.INVISIBLE);
                            naverLogout.setEnabled(false);
                            Toast.makeText(MainActivity.this, "네이버 로그아웃 되었습니다.", Toast.LENGTH_SHORT).show();
                        }
                    });
                    logoutDialog.show();
            }
        });
    }
    // 유저 정보 가져오기
    private void naverUserInfo(String msg){

        try {
            JSONObject userInfo = new JSONObject(msg);
            String resultCode = userInfo.getString("resultcode");

            if(resultCode.equals("00")) {
                String response = userInfo.getString("response");

                JSONObject userInfo2 = new JSONObject(response);

                String userName = userInfo2.getString("name");
                String mobile = userInfo2.getString("mobile");
                String email = userInfo2.getString("email");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        naverLogout.setVisibility(View.VISIBLE);
                        naverLogout.setEnabled(true);
                        Toast.makeText(getApplicationContext(), "이름 : " + userName + "\n" + "전화번호 : " + mobile + "\n" + "이메일 : " + email ,Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(getApplicationContext(),"로그인 오류가 발생했습니다.",Toast.LENGTH_SHORT).show();
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private static String get(String apiUrl, Map<String, String> requestHeaders){
        HttpURLConnection con = connect(apiUrl);
        try {
            con.setRequestMethod("GET");
            for(Map.Entry<String, String> header :requestHeaders.entrySet()) {
                con.setRequestProperty(header.getKey(), header.getValue());
            }

            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) { // 정상 호출
                return readBody(con.getInputStream());
            } else { // 에러 발생
                return readBody(con.getErrorStream());
            }
        } catch (IOException e) {
            throw new RuntimeException("API 요청과 응답 실패", e);
        } finally {
            con.disconnect();
        }
    }

    private static HttpURLConnection connect(String apiUrl){
        try {
            java.net.URL url = new URL(apiUrl);
            return (HttpURLConnection)url.openConnection();
        } catch (MalformedURLException e) {
            throw new RuntimeException("API URL이 잘못되었습니다. : " + apiUrl, e);
        } catch (IOException e) {
            throw new RuntimeException("연결이 실패했습니다. : " + apiUrl, e);
        }
    }

    private static String readBody(InputStream body){
        InputStreamReader streamReader = new InputStreamReader(body);

        try (BufferedReader lineReader = new BufferedReader(streamReader)) {
            StringBuilder responseBody = new StringBuilder();

            String line;
            while ((line = lineReader.readLine()) != null) {
                responseBody.append(line);
            }

            return responseBody.toString();
        } catch (IOException e) {
            throw new RuntimeException("API 응답을 읽는데 실패했습니다.", e);
        }
    }
}