package com.litmus.worldscope;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.litmus.worldscope.model.WorldScopeUser;
import com.litmus.worldscope.utility.WorldScopeAPIService;
import com.litmus.worldscope.utility.WorldScopeRestAPI;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import fragment.FacebookLoginFragment;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity implements FacebookLoginFragment.OnFragmentInteractionListener {

    private static final String TAG = "LoginActivity";
    private static final String WELCOME_GIF_LINK = "file:///android_asset/welcomeGifAssets/welcome.html";
    private static final String APP_SERVER_AUTH_FAILED_MSG = "Authentication with WorldScope's server has failed, please check that you have internet connections and try again.";
    private static Context context;
    private FacebookLoginFragment facebookLoginFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_facebook_login);
        context = this;

        loadGifIntoWebView();
    }

    @Override
    public void onFacebookLoginSuccess(AccessToken accessToken) {
        // Successful login -> Redirect to Main activity
        Log.d(TAG, "Login Success!");
        Log.d(TAG, "AccessToken: " + accessToken.getToken());

        // Instantiate and make a call to login user into WorldScope servers
        Call<WorldScopeUser> call = new WorldScopeRestAPI(context).buildWorldScopeAPIService().loginUser(new WorldScopeAPIService.LoginUserRequest(accessToken.getToken()));
        call.enqueue(new Callback<WorldScopeUser>() {
            @Override
            public void onResponse(Response<WorldScopeUser> response) {
                if(response.isSuccess()) {
                    Log.d(TAG, "Success!");
                    Log.d(TAG, "" + response.body().toString());

                    WorldScopeUser user = response.body();
                    WorldScopeAPIService.setUser(user);
                    // Redirect to MainActivity if successful
                    redirectToMainActivity();

                } else {
                    Log.d(TAG, "Failure" + response.code() + ": " + response.message());
                    // Logout of Facebook
                    logoutOfFacebook();
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.d(TAG, "Failure: " + t.getMessage());
                // Logout of Facebook
                logoutOfFacebook();
            }
        });
    }

    //Redirects to MainActivity
    protected void redirectToMainActivity() {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("activity", TAG);
        startActivity(intent);
    }

    // Called to logout of Facebook when attempt to authenticate with App server fails
    private void logoutOfFacebook() {

        if(facebookLoginFragment == null) {
            // Get FacebookLoginFragment if missing
            facebookLoginFragment = (FacebookLoginFragment) getSupportFragmentManager().findFragmentById(R.id.facebookLoginButtonFragment);
        }

        // Toast to inform user
        Toast toast = Toast.makeText(context, APP_SERVER_AUTH_FAILED_MSG, Toast.LENGTH_LONG);
        toast.show();
        facebookLoginFragment.logoutFromFacebook();

    }

    // Method to load Gif's html data into WebView
    private void loadGifIntoWebView() {
        WebView welcomeGifWebView = (WebView) findViewById(R.id.welcomeGifWebView);
        welcomeGifWebView.loadUrl(WELCOME_GIF_LINK);
    }
}
