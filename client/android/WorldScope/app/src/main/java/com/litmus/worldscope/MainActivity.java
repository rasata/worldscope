package com.litmus.worldscope;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import com.litmus.worldscope.model.WorldScopeUser;
import com.litmus.worldscope.model.WorldScopeViewStream;
import com.litmus.worldscope.utility.CircleTransform;
import com.litmus.worldscope.utility.FacebookWrapper;
import com.litmus.worldscope.utility.WorldScopeAPIService;
import com.litmus.worldscope.utility.WorldScopeRestAPI;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Transformation;

import fragment.StreamRefreshListFragment;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
            FacebookWrapper.FacebookWrapperProfilePictureCallback,
            WorldScopeAPIService.OnUserRequestListener {

    private final String TAG = "MainActivity";

    // Welcome message shown in Toast
    private final String WELCOME_MSG = "Welcome to WorldScope, %s";

    private final int NUMBER_OF_TABS = 3;

    private static final Boolean IS_LOGOUT_ATTEMPT = true;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    // State variables
    private Context context;
    private boolean userIsLoaded;
    private boolean isRedirectedFromLoginActivity;

    // UI variables
    private ViewPager mViewPager;
    private WorldScopeUser loginUser;
    private Toolbar toolbar;

    private FacebookWrapper facebookWrapper = FacebookWrapper.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        checkIfRedirectedFromLoginActivity();

        // Initialize FacebookSDK
        facebookWrapper.initializeFacebookSDK(context);

        // Set the title of the toolbar
        setToolbarTitle();

        // Set FAB to redirect to StreamActivity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                redirectToStreamActivity();
            }
        });

        // Set up the drawer fragment and the menu inside
        setUpDrawerFragment();

        //Set up the tabs
        setUpTabsFragment();
    }

    @Override
    public void onBackPressed() {
        // Close the drawer on hardware back key pressed
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Get user information
        getUserInformation();
        return true;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_logout) {
            Log.d(TAG, "Logging out");
            logoutFromAppServer();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void checkIfRedirectedFromLoginActivity() {
        Intent intent = getIntent();
        if(!intent.hasExtra("activity")) {
            isRedirectedFromLoginActivity = false;
            return;
        }

        String previousActivityName = intent.getStringExtra("activity");
        if(previousActivityName.equals("LoginActivity")) {
            isRedirectedFromLoginActivity = true;
        } else {
            isRedirectedFromLoginActivity = false;
        }

    }

    private void showLoginToast(String alias) {
        // Show welcome message
        Toast toast = Toast.makeText(context,
                String.format(WELCOME_MSG, alias), Toast.LENGTH_SHORT);
        toast.show();
    }

    // Set up the ViewPager and TabLayout to form the Tabs Fragment in the activity
    private void setUpTabsFragment() {
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);

        // Keep all three tabs in memory
        mViewPager.setOffscreenPageLimit(NUMBER_OF_TABS);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
    }

    // Set up the drawer fragment and state listeners

    private void setUpDrawerFragment() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    // Set the title of toolbar

    private void setToolbarTitle() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        try {
            getSupportActionBar().setTitle(R.string.app_name);
        } catch(NullPointerException e){
            e.printStackTrace();
        }
    }

    // Redirects to LoginActivity

    protected void redirectToLoginActivity(boolean isAttemptLogout) {
        Intent intent = new Intent(this, LoginActivity.class);

        if(isAttemptLogout) {
            intent.putExtra("isAttemptLogout", true);
        }

        startActivity(intent);
    }


    // Redirects to view activity

    protected void redirectToViewActivity() {
        Intent intent = new Intent(this, ViewActivity.class);
        startActivity(intent);
    }


    // Redirects to stream activity

    protected void redirectToStreamActivity() {
        Intent intent = new Intent(this, StreamActivity.class);
        startActivity(intent);
    }


    // Query Facebook graph API for profile picture and loads Alias and picture into Drawer

    private void loadUserInfo(WorldScopeUser user) {

        loginUser = user;

        if(loginUser != null && isRedirectedFromLoginActivity) {
            showLoginToast(loginUser.getAlias());
        } else {
            Log.d(TAG, "Unable to get user");
        }

        TextView alias = (TextView) findViewById(R.id.drawerAlias);
        // Set alias into drawer
        alias.setText(loginUser.getAlias());
        Log.d(TAG, "Username loaded: " + loginUser.getAlias());

        facebookWrapper.setFacebookWrapperProfilePictureCallbackListener(this);
        facebookWrapper.getProfilePictureUrl(loginUser.getPlatformId());
    }

    @Override
    public void onProfilePictureUrl(String profilePictureUrl) {
        loadProfilePictureIntoView(profilePictureUrl);
    }

    private void loadProfilePictureIntoView(String profilePictureUrl) {
        // Get the view for profile picture
        final ImageView profilePicture = (ImageView) findViewById(R.id.drawerProfilePicture);

        // Set the image to the imageView and trim it to a circle
        Picasso.with(profilePicture.getContext())
                .load(profilePictureUrl)
                .transform(new CircleTransform())
                .into(profilePicture);
    }

    // Log out from WorldScope App server
    private void logoutFromAppServer() {
        Call<WorldScopeUser> call = new WorldScopeRestAPI(context
        ).buildWorldScopeAPIService().logoutUser();
        call.enqueue(new Callback<WorldScopeUser>() {
            @Override
            public void onResponse(Response<WorldScopeUser> response) {
                if (response.isSuccess()) {
                    Log.d(TAG, "Success!");
                    Log.d(TAG, "" + response.body().toString());

                    redirectToLoginActivity(IS_LOGOUT_ATTEMPT);
                } else {
                    Log.d(TAG, "Failure!");
                    Log.d(TAG, "" + response.code());
                    Log.d(TAG, "" + response.body().toString());

                    redirectToLoginActivity(IS_LOGOUT_ATTEMPT);
                }
            }

            @Override
            public void onFailure(Throwable t) {

                redirectToLoginActivity(IS_LOGOUT_ATTEMPT);
            }
        });
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            StreamRefreshListFragment fragment = StreamRefreshListFragment.newInstance(position + 1);
            return fragment;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Popular";
                case 1:
                    return "Latest";
                case 2:
                    return "Following";
            }
            return null;
        }
    }

    // Overide WorldScopeAPIService.UserRequest
    @Override
    public void getUser(WorldScopeUser user) {
        loadUserInfo(user);
    }

    public void getUserInformation() {
        WorldScopeAPIService.registerRequestUser(context);
        WorldScopeAPIService.requestUser(this);
    }

    // Function to make API call to subscribe to user
    private void subscribeUser(String userId) {

        if(userId == null) {
            return;
        }

        Call<Object> call = new WorldScopeRestAPI(this)
                .buildWorldScopeAPIService()
                .postSubscribe(userId);

        Log.d(TAG, call.toString());

        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Response<Object> response) {
                if (response.isSuccess()) {
                    Log.d(TAG, "RESPONSE SUCCESS FOR SUBSCRIPTION");
                    Log.d(TAG, response.toString());
                } else {
                    Log.d(TAG, "RESPONSE FAIL");
                    Log.d(TAG, response.toString());
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.d(TAG, "NO RESPONSE");
            }
        });
    }
}
