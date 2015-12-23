
package com.uberspot.a2048_sdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.webkit.WebSettings;
import android.webkit.WebSettings.RenderPriority;
import android.webkit.WebView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.targeting402.sdk.main.Targeting_402;
import com.targeting402.sdk.main.Targeting_402.SocialNetworkAccess;
import com.targeting402.sdk.main.Targeting_402.SocialNetworkAccess.SocialNetwork;
import com.targeting402.sdk.main.Targeting_402_TestSuite;
import com.targeting402.sdk.util.Logger;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKAccessTokenTracker;
import com.vk.sdk.VKCallback;
import com.vk.sdk.VKSdk;
import com.vk.sdk.api.VKError;

public class MainActivity
        extends Activity
        implements Targeting_402.HotStateListener
{

    public static final String YOUR_DEVELOPER_ID = "";

    private static final String[] sVkScope = new String[] {};

    public static final String SOCIAL_PREFS = "social_prefs";
    public static final String PREF_TOKEN   = "token";
    public static final String PREF_NETWORK = "network";
    public static final String PREF_USER_ID = "user_id";

    private WebView mWebView;
    private long    mLastBackPress;
    private static final long    mBackPressThreshold = 3500;
    private static final String  IS_FULLSCREEN_PREF  = "is_fullscreen_pref";
    private static       boolean DEF_FULLSCREEN      = true;
    private long mLastTouch;
    private static final long mTouchThreshold = 2000;
    private Toast pressBackToast;

    private NfcAdapter mNfcAdapter;

    private CallbackManager callbackManager;
    private View            btnLogoutSocial;
    private View            layoutSocial;
    private View            btnFacebook;
    private View            btnVk;

    private FacebookTokenTracker facebookTokenTracker;

    @SuppressLint({
            "SetJavaScriptEnabled",
            "NewApi",
            "ShowToast"
    })

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Don't show an action bar or title
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // If on android 3.0+ activate hardware acceleration
        if (Build.VERSION.SDK_INT >= 11)
        {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
            );
        }

        // Apply previous setting about showing status bar or not
        applyFullScreen(isFullScreen());

        // Check if screen rotation is locked in settings
        boolean isOrientationEnabled = false;
        try
        {
            isOrientationEnabled = Settings.System.getInt(
                    getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION
            ) == 1;
        } catch (SettingNotFoundException e)
        {
        }

        // If rotation isn't locked and it's a LARGE screen then add orientation changes based on
        // sensor
        int screenLayout = getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK;
        if (((screenLayout == Configuration.SCREENLAYOUT_SIZE_LARGE)
                || (screenLayout == Configuration.SCREENLAYOUT_SIZE_XLARGE))
                && isOrientationEnabled)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }

        setContentView(R.layout.activity_main);

        // Load webview with game
        mWebView = (WebView) findViewById(R.id.mainWebView);
        WebSettings settings = mWebView.getSettings();
        String packageName = getPackageName();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setRenderPriority(RenderPriority.HIGH);
        settings.setDatabasePath("/data/data/" + packageName + "/databases");

        // If there is a previous instance restore it in the webview
        if (savedInstanceState != null)
        {
            mWebView.restoreState(savedInstanceState);
        } else
        {
            mWebView.loadUrl("file:///android_asset/2048/index.html");
        }

        Toast.makeText(
                getApplication(),
                R.string.toggle_fullscreen,
                Toast.LENGTH_SHORT
        )
             .show();
        // Set fullscreen toggle on webview LongClick
        mWebView.setOnTouchListener(
                new OnTouchListener()
                {

                    @Override
                    public boolean onTouch(
                            View v,
                            MotionEvent event
                    )
                    {
                        // Implement a long touch action by comparing
                        // time between action up and action down
                        long currentTime = System.currentTimeMillis();
                        if ((event.getAction() == MotionEvent.ACTION_UP)
                                && (Math.abs(currentTime - mLastTouch) > mTouchThreshold))
                        {
                            boolean toggledFullScreen = !isFullScreen();
                            saveFullScreen(toggledFullScreen);
                            applyFullScreen(toggledFullScreen);
                        } else if (event.getAction() == MotionEvent.ACTION_DOWN)
                        {
                            mLastTouch = currentTime;
                        }
                        // return so that the event isn't consumed but used
                        // by the webview as well
                        return false;
                    }
                }
        );

        pressBackToast = Toast.makeText(
                getApplicationContext(),
                R.string.press_back_again_to_exit,
                Toast.LENGTH_SHORT
        );

        // TARGETING 402 SDK
        Targeting_402.init(
                MainActivity.this/*getApplicationContext()*/,
                YOUR_DEVELOPER_ID
        );
        Targeting_402.setDebugMode(true);
        //Targeting_402.setHotStateListener(this);

        // end of TARGETING 402 SDK

        // NFC handling
        // uncomment the line below if you want to test with NFC
        //handlePossibleNfcAction(getIntent());

        findViewById(R.id.btn_send_log).setOnClickListener(
                new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        Targeting_402.getTestSuite()
                                     .sendDebugInfo(getApplicationContext());
                    }
                }
        );

        findViewById(R.id.btn_show_hot).setOnClickListener(
                new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        showHot();
                    }
                }
        );

        String network = getSharedPreferences(
                SOCIAL_PREFS,
                Context.MODE_PRIVATE
        ).getString(
                PREF_NETWORK,
                null
        );

        btnLogoutSocial = findViewById(R.id.btn_logout_social);
        layoutSocial = findViewById(R.id.layout_social_networks);

        btnLogoutSocial.setOnClickListener(
                new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        updateUiForLoggedOut();
                        logoutSocial();
                    }
                }
        );

        btnFacebook = findViewById(R.id.btn_login_facebook);
        btnFacebook.setOnClickListener(
                new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        loginFacebook();
                    }
                }
        );

        btnVk = findViewById(R.id.btn_login_vk);
        btnVk.setOnClickListener(
                new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        loginVK();
                    }
                }
        );

        if (network != null)
        {
            updateUiForLoggedIn();

            if (network.equals(SocialNetwork.VK.name()))
            {
                vkAccessTokenTracker.startTracking();
            } else
            {
                facebookTokenTracker = new FacebookTokenTracker();
                facebookTokenTracker.startTracking();
            }
        }

        // facebook
        callbackManager = CallbackManager.Factory.create();
        LoginManager.getInstance()
                    .registerCallback(
                            callbackManager,
                            new FacebookCallback()
                    );
        // end of Facebook
    }

    private void updateUiForLoggedOut()
    {
        btnLogoutSocial.setVisibility(View.GONE);
        layoutSocial.setVisibility(View.VISIBLE);
    }

    private void updateUiForLoggedIn()
    {
        layoutSocial.setVisibility(View.GONE);
        btnLogoutSocial.setVisibility(View.VISIBLE);
    }

    private void logoutSocial()
    {
        saveToken(
                null,
                null,
                null
        );
    }

    private void saveTokenAndPassToSdk(
            String network,
            String token,
            String userId
    )
    {
        new SocialNetworkAccess().passTokenData(
                SocialNetwork.valueOf(network),
                token,
                userId
        );

        saveToken(
                network,
                token,
                userId
        );
    }

    private void saveToken(
            String network,
            String token,
            String userId
    )
    {
        getSharedPreferences(
                SOCIAL_PREFS,
                MODE_PRIVATE
        ).edit()
         .putString(
                 PREF_NETWORK,
                 network
         )
         .putString(
                 PREF_TOKEN,
                 token
         )
         .putString(
                 PREF_USER_ID,
                 userId
         )
         .commit();
    }

    private void handlePossibleNfcAction(Intent intent)
    {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_TECH_DISCOVERED.equals(action))
        {

            mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

            if (mNfcAdapter == null)
            {
                Toast.makeText(
                        this,
                        "This device doesn't support NFC.",
                        Toast.LENGTH_LONG
                )
                     .show();
            } else
            {
                handleNfcIntent(intent);
            }
        }
    }

    private void handleNfcIntent(Intent intent)
    {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        String tagId = byteArrayToHex(tag.getId());
        Log.e(
                getClass().getSimpleName(),
                tagId
        );

        showHot();
    }

    private void showHot()
    {
        Targeting_402_TestSuite testSuite = Targeting_402.getTestSuite();
        if (testSuite != null)
        {
            testSuite.getDebugHot();
        }
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);

        handlePossibleNfcAction(intent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        mWebView.saveState(outState);
    }

    @Override
    protected void onDestroy()
    {
        if (vkAccessTokenTracker != null
                && vkAccessTokenTracker.isTracking())
        {
            vkAccessTokenTracker.stopTracking();
        }

        if (facebookTokenTracker != null
                && facebookTokenTracker.isTracking())
        {
            facebookTokenTracker.stopTracking();
        }

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        // getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void saveFullScreen(boolean isFullScreen)
    {
        // save in preferences
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this)
                                                           .edit();
        editor.putBoolean(
                IS_FULLSCREEN_PREF,
                isFullScreen
        );
        editor.commit();
    }

    private boolean isFullScreen()
    {
        return PreferenceManager.getDefaultSharedPreferences(this)
                                .getBoolean(
                                        IS_FULLSCREEN_PREF,
                                        DEF_FULLSCREEN
                                );
    }

    /**
     * Toggles the activities fullscreen mode by setting the corresponding window flag
     *
     * @param isFullScreen
     */
    private void applyFullScreen(boolean isFullScreen)
    {
        if (isFullScreen)
        {
            getWindow().clearFlags(LayoutParams.FLAG_FULLSCREEN);
        } else
        {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
            );
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed()
    {
        long currentTime = System.currentTimeMillis();
        if (Math.abs(currentTime - mLastBackPress) > mBackPressThreshold)
        {
            pressBackToast.show();
            mLastBackPress = currentTime;
        } else
        {
            pressBackToast.cancel();
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    )
    {
        if (!VKSdk.onActivityResult(
                requestCode,
                resultCode,
                data,
                callback
        ))
        {
            super.onActivityResult(
                    requestCode,
                    resultCode,
                    data
            );

            handleFacebookCallback(
                    requestCode,
                    resultCode,
                    data
            );
        }
    }

    public static String byteArrayToHex(byte[] a)
    {
        StringBuilder sb = new StringBuilder(a.length * 2);
        for (byte b : a)
        {
            sb.append(
                    String.format(
                            "%02x",
                            b & 0xff
                    )
            );
        }
        return sb.toString();
    }

    private void loginVK()
    {
        VKSdk.login(
                this,
                sVkScope
        );
    }

    private void loginFacebook()
    {
        AccessToken token = AccessToken.getCurrentAccessToken();

        if (token != null)
        {
            updateUiForLoggedIn();

            saveTokenAndPassToSdk(
                    SocialNetwork.Facebook.name(),
                    token.getToken(),
                    token.getUserId()
            );
        } else
        {
            getNewFacebookToken();
        }
    }

    private void handleFacebookCallback(
            int requestCode,
            int resultCode,
            Intent data
    )
    {
        callbackManager.onActivityResult(
                requestCode,
                resultCode,
                data
        );
    }

    private void getNewFacebookToken()
    {
        LoginManager.getInstance()
                    .logInWithReadPermissions(
                            this,
                            null
                    );
    }

    @Override
    public void onHotNeedAppear(Targeting_402.HotState hotState)
    {
        /*impl*/
    }

    @Override
    public void onHotNeedDisappearSoon(Targeting_402.HotState hotState)
    {
        /*impl*/
    }

    @Override
    public void onHotNeedDisappear(Targeting_402.HotState hotState)
    {
        /*impl*/
    }

    VKCallback<VKAccessToken> callback = new VKCallback<VKAccessToken>()
    {
        @Override
        public void onResult(VKAccessToken token)
        {
            Logger.e(
                    null,
                    null
            );

            updateUiForLoggedIn();

            saveTokenAndPassToSdk(
                    SocialNetwork.VK.name(),
                    token.accessToken,
                    token.userId
            );
        }

        @Override
        public void onError(VKError error)
        {
            if (error != null)
            {
                Logger.e(
                        error.errorReason + ":" + error.errorMessage,
                        null
                );
            } else
            {
                Logger.e(
                        null,
                        null
                );
            }
        }
    };

    private class FacebookCallback
            implements com.facebook.FacebookCallback<LoginResult>
    {
        @Override
        public void onSuccess(LoginResult loginResult)
        {
            AccessToken token = loginResult.getAccessToken();

            if (token != null)
            {
                Logger.i(
                        null,
                        null
                );

                updateUiForLoggedIn();

                saveTokenAndPassToSdk(
                        SocialNetwork.Facebook.name(),
                        token.getToken(),
                        token.getUserId()
                );
            }
        }

        @Override
        public void onCancel()
        {
            Logger.e(
                    null,
                    null
            );
        }

        @Override
        public void onError(FacebookException exception)
        {
            if (exception != null)
            {
                Logger.e(
                        exception.getMessage(),
                        null
                );
            } else
            {
                Logger.e(
                        null,
                        null
                );
            }
        }
    }

    VKAccessTokenTracker vkAccessTokenTracker = new VKAccessTokenTracker()
    {
        @Override
        public void onVKAccessTokenChanged(
                VKAccessToken oldToken,
                VKAccessToken newToken
        )
        {
            if (newToken == null)
            {
                logoutSocial();
            } else
            {
                saveTokenAndPassToSdk(
                        SocialNetwork.VK.name(),
                        newToken.accessToken,
                        newToken.userId
                );
            }
        }
    };

    private class FacebookTokenTracker
            extends AccessTokenTracker

    {
        @Override
        protected void onCurrentAccessTokenChanged(
                AccessToken oldAccessToken,
                AccessToken currentAccessToken
        )
        {
            if (currentAccessToken != null)
            {
                saveTokenAndPassToSdk(
                        SocialNetwork.Facebook.name(),
                        currentAccessToken.getToken(),
                        currentAccessToken.getUserId()
                );
            }
        }
    }
}
