package com.kw.mapit;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;
import com.kakao.auth.ISessionCallback;
import com.kakao.auth.Session;
import com.kakao.usermgmt.LoginButton;
import com.kakao.util.exception.KakaoException;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import static com.kakao.util.helper.Utility.getPackageInfo;

public class LoginActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener{

    String myJSON;

    public static Activity activity_login;

    //Google Login
    static public GoogleApiClient mGoogleApiClient;
    String userName = null;

    //Kakao Login
    private SessionCallback callback;

    private static final String TAG_RESULTS = "result";
    private static final String TAG_USER_ID = "id";
    private static final String TAG_USER_PASSWORD = "password";

    private static final String LOG_TAG = "LOGIN";

    String user_id;
    String user_password;

    EditText edit_id;
    EditText edit_pw;
    LoginButton kakao_sign_in;

    JSONArray userData = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        activity_login = LoginActivity.this;

        edit_id= (EditText)findViewById(R.id.ed_login_id);
        edit_pw= (EditText)findViewById(R.id.ed_login_password);
        kakao_sign_in = (LoginButton)findViewById(R.id.btn_kakao_sign_in);

        //EditText 글자 색 : 검정
        edit_id.setTextColor(Color.BLACK);
        edit_pw.setTextColor(Color.BLACK);

        callback = new SessionCallback();
        Session.getCurrentSession().addCallback(callback);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(Session.getCurrentSession() != null) {
            if (Session.getCurrentSession().handleActivityResult(requestCode, resultCode, data)) {
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(Session.getCurrentSession() != null) {
            Session.getCurrentSession().removeCallback(callback);
        }
    }

    //버튼 클릭 시
    public void onClick(View v) throws ExecutionException, InterruptedException {

        //로그인 버튼
        if(v.getId() == R.id.btn_login) {
            getUserData task = new getUserData();
            String result = task.execute(edit_id.getText().toString(), edit_pw.getText().toString()).get();
        }

        //Google Api 회원가입
        if(v.getId() == R.id.btn_google_sign_in) {

            Toast.makeText(this, "Google에 접속", Toast.LENGTH_SHORT).show();

            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Plus.API)
                    .addScope(Plus.SCOPE_PLUS_LOGIN)
                    .build();

            mGoogleApiClient.connect();
        }

        //Kakao Api 회원가입
        else if(v.getId() == R.id.iv_kakao_sign_in) {
            kakao_sign_in.performClick();
        }

        //자체 회원가입
        if(v.getId() == R.id.btn_sign_in) {
            Intent goIntent = new Intent(this, InputDataActivity.class);
            startActivity(goIntent);
        }
    }

    private class SessionCallback implements ISessionCallback {

        //세션 연결 성공 시 KakaoSignupAcitivity로 이동
        @Override
        public void onSessionOpened() {
            redirectSignupActivity();
        }

        //세션 연결 실패 시(다시 LoginActivity로 이동
        @Override
        public void onSessionOpenFailed(KakaoException exception) {
            if(exception != null) {
                Log.e("Session Open Failed = ",String.valueOf(exception));
            }
            setContentView(R.layout.activity_login);
        }

    }

    //SIgnupActivity로 이동
    protected void redirectSignupActivity() {
        final Intent intent = new Intent(this, KakaoSignupActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        finish();
    }

    class getUserData extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;

        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = ProgressDialog.show(LoginActivity.this,"please wait",null,true, true);
        }
        protected void onPostExecute(String result){
            super.onPostExecute(result);

            progressDialog.dismiss();
            Log.d(LOG_TAG,"POST response - " + result);

            myJSON = result;

            if(result.equalsIgnoreCase("true")) {               //로그인 성공 시
                Intent goIntent = new Intent(LoginActivity.this, PixelActivity.class);
                startActivity(goIntent);
                LoginActivity.this.finish();

                Toast.makeText(getApplicationContext(), "로그인에 성공하였습니다.", Toast.LENGTH_SHORT).show();
            }
            else if(result.equalsIgnoreCase("false")) {         //로그인 실패 시
                edit_pw.setText("");

                Toast.makeText(getApplicationContext(), "로그인에 실패하였습니다.", Toast.LENGTH_SHORT).show();
            }

        }
        @Override
        protected String doInBackground(String... params) {

            String serverURL = "http://" + getString(R.string.ip) + "/selectUserData.php";

            try{
                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();

                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.connect();

                //PHP로 id, password 정보 전달
                Uri.Builder builder = new Uri.Builder()
                        .appendQueryParameter("id", params[0])
                        .appendQueryParameter("password", params[1]);
                String query = builder.build().getEncodedQuery();

                OutputStream outputStream = httpURLConnection.getOutputStream();
                BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
                bufferedWriter.write(query);
                bufferedWriter.flush();
                bufferedWriter.close();
                //outputStream.write(sbParams.toString().getBytes("UTF-8"));
                //outputStream.write(postParameters.getBytes("UTF-8"));
                //outputStream.flush();
                outputStream.close();

                int responseStatusCode=httpURLConnection.getResponseCode();
                Log.d(LOG_TAG,"POST respose code - " +responseStatusCode);

                InputStream inputStream;
                if(responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = httpURLConnection.getInputStream();
                } else {
                    inputStream = httpURLConnection.getErrorStream();
                }

                InputStreamReader inputStreamReader = new InputStreamReader(inputStream,"UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder sb = new StringBuilder();
                String line = null;

                while((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }

                bufferedReader.close();

                return sb.toString();

            } catch (Exception e) {
                Log.e(LOG_TAG,"getUserData: Error ",e);

                return new String("Error: "+e.getMessage());
            }

        }

    }

    //GoogleLogin
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //구글 연결
        if (!mGoogleApiClient.isConnected() || Plus.PeopleApi.getCurrentPerson(mGoogleApiClient) == null) {
            //연결 실패
        } else {
            //연결 성공
            Person currentPerson = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);

            if (currentPerson.hasImage()) {
                //이미지 경로 : currentPerson.getImage().getUrl());
               /* Glide.with(MainActivity.this)
                        .load(currentPerson.getImage().getUrl())
                        .into(userphoto);*/
            }
            if (currentPerson.hasDisplayName()) {
                //디스플레이 이름 :currentPerson.getDisplayName());
                //디스플레이 아이디 : currentPerson.getId());
                userName = currentPerson.getDisplayName();
            }

            //InputData 액티비티로 이동
            Intent intent = new Intent(this, InputDataActivity.class);
            intent.putExtra("NAME", userName);
            startActivity(intent);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }


    //연결 실패 시
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            Log.e("Google Login",
                    String.format(
                            "Connection to Play Services Failed, error: %d, reason: %s",
                            connectionResult.getErrorCode(),
                            connectionResult.toString()));
            try {
                connectionResult.startResolutionForResult(this, 0);
            } catch (IntentSender.SendIntentException e) {
                Log.e("Google Login", e.toString(), e);
            }
        } else {
            Toast.makeText(getApplicationContext(), "이미 로그인 중", Toast.LENGTH_SHORT).show();
        }
    }

}
