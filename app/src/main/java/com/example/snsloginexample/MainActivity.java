package com.example.snsloginexample;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
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

    // Google Sign In API와 호출할 구글 로그인 클라이언트
    GoogleSignInClient mGoogleSignInClient;
    private final int RC_SIGN_IN = 123;
    private static final String TAG = "MainActivity";

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

        // 구글 로그인
        // 앱에 필요한 사용자 데이터를 요청하도록 로그인 옵션을 설정한다.
        // DEFAULT_SIGN_IN parameter는 유저의 ID와 기본적인 프로필 정보를 요청하는데 사용된다.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail() // email addresses도 요청함
                .build();
        // 위에서 만든 GoogleSignInOptions을 사용해 GoogleSignInClient 객체를 만듬
        mGoogleSignInClient = GoogleSignIn.getClient(MainActivity.this, gso);
        // 기존에 로그인 했던 계정을 확인한다.
        GoogleSignInAccount gsa = GoogleSignIn.getLastSignedInAccount(MainActivity.this);
        // 로그인 되있는 경우 (토큰으로 로그인 처리)
        if (gsa != null && gsa.getId() != null) {

        }

        googleLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn();
            }
        });

        googleLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder logoutDialog = new AlertDialog.Builder(MainActivity.this);
                logoutDialog.setTitle("알림");
                logoutDialog.setMessage("로그아웃 하시겠습니까?");
                logoutDialog.setNegativeButton(" 취소", null);
                logoutDialog.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mGoogleSignInClient.signOut()
                                .addOnCompleteListener(MainActivity.this, task -> {
                                    Log.d(TAG, "onClick:logout success ");
                                    mGoogleSignInClient.revokeAccess()
                                            .addOnCompleteListener(MainActivity.this, task1 -> Log.d(TAG, "onClick:revokeAccess success "));
                                });
                        googleLogout.setVisibility(View.INVISIBLE);
                        googleLogout.setEnabled(false);
                        Toast.makeText(getApplicationContext(), "구글 로그아웃되었습니다." ,Toast.LENGTH_SHORT).show();
                    }
                });
                logoutDialog.show();
            }
        });

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
    // 구글
    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount acct = completedTask.getResult(ApiException.class);

            if (acct != null) {
                String personName = acct.getDisplayName();
                String personGivenName = acct.getGivenName();
                String personFamilyName = acct.getFamilyName();
                String personEmail = acct.getEmail();
                String personId = acct.getId();
                Uri personPhoto = acct.getPhotoUrl();

                Log.d(TAG, "handleSignInResult:personName "+personName);
                Log.d(TAG, "handleSignInResult:personGivenName "+personGivenName);
                Log.d(TAG, "handleSignInResult:personEmail "+personEmail);
                Log.d(TAG, "handleSignInResult:personId "+personId);
                Log.d(TAG, "handleSignInResult:personFamilyName "+personFamilyName);
                Log.d(TAG, "handleSignInResult:personPhoto "+personPhoto);
                googleLogout.setVisibility(View.VISIBLE);
                googleLogout.setEnabled(true);
                Toast.makeText(getApplicationContext(), "이름 : " + personName + "\n" + "이메일 : " + personEmail ,Toast.LENGTH_SHORT).show();
            }
        } catch (ApiException e) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.e(TAG, "signInResult:failed code=" + e.getStatusCode());

        }
    }
    // 카카오

    // 네이버
    private void naverUserInfo(String msg){ // 유저 정보 가져오기

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 구글 로그아웃
        mGoogleSignInClient.signOut()
                .addOnCompleteListener(MainActivity.this, task -> {
                    Log.d(TAG, "onClick:logout success ");
                    mGoogleSignInClient.revokeAccess()
                            .addOnCompleteListener(MainActivity.this, task1 -> Log.d(TAG, "onClick:revokeAccess success "));
                });
        // 네이버 로그아웃
        oAuthLogin.logout(context);
    }
}