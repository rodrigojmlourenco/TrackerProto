package org.trace.trackerproto.ui;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Messenger;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import org.trace.trackerproto.R;
import org.trace.trackerproto.ui.slidingmenu.adapter.NavDrawerListAdapter;
import org.trace.trackerproto.ui.slidingmenu.model.NavDrawerItem;
import org.trace.tracking.TrackingConstants;
import org.trace.tracking.store.TraceAuthenticationManager;
import org.trace.tracking.store.auth.AuthenticationRenewalListener;
import org.trace.tracking.store.exceptions.UserIsNotLoggedException;
import org.trace.tracking.tracker.TRACETracker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

//TODO: update the count of navdraweritem on delete or create track
public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks, TrackCountListener, GoogleApiClient.OnConnectionFailedListener {

    private static final String LOG_TAG = "MainActivity";
    private Fragment mCurrentFragment = null;

    private AuthenticationRenewalListener authRenewalListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(savedInstanceState!=null)
            updateState(savedInstanceState);

        setupTraceAuthenticationManager();
        setupSlidingMenu(savedInstanceState);

        authRenewalListener = new AuthenticationRenewalListener(this, mAuthManager);
        registerReceiver(authRenewalListener, AuthenticationRenewalListener.getAuthenticationRenewalFilter());
    }


    /* Activity Life-Cycle
    /* Activity Life-Cycle
    /* Activity Life-Cycle
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    private boolean isBound = false;
    private Messenger mService = null;
    private TRACETracker.Client mTrakerClient = null;

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService= new Messenger(service);
            mTrakerClient = new TRACETracker.Client(MainActivity.this, mService);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService= null;
            mTrakerClient = null;

        }
    };

    @Override
    protected void onStart() {
        super.onStart();

        Intent trackerService = new Intent(this, TRACETracker.class);
        trackerService.setFlags(Service.START_STICKY);
        bindService(trackerService, mConnection, Context.BIND_AUTO_CREATE);

        isBound = true;

    }

    @Override
    protected void onDestroy() {

        if(isBound){
            isBound = false;
            this.unbindService(mConnection);
        }

        if(isFinishing()) {
            unregisterReceiver(authRenewalListener);
            mAuthManager.logout();

            //TRACEStore.Client.requestLogout(MainActivity.this);
        }

        if(mCurrentFragment instanceof MapViewFragment){
            ((MapViewFragment)mCurrentFragment).cleanMap();
        }

        super.onDestroy();
    }

    /* Sliding Menu
    /* Sliding Menu
    /* Sliding Menu
    /* Sliding Menu
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;

    // nav drawer title
    private CharSequence mDrawerTitle;

    // used to store app title
    private CharSequence mTitle;

    // slide menu items
    private String[] navMenuTitles;
    private TypedArray navMenuIcons;

    private ArrayList<NavDrawerItem> navDrawerItems;
    private NavDrawerListAdapter adapter;



    @SuppressWarnings("deprecation")
    private void setupSlidingMenu(Bundle savedInstanceState){


        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_app);

        mTitle = mDrawerTitle = getTitle();

        // load slide menu items
        navMenuTitles = getResources().getStringArray(R.array.nav_drawer_items);

        // nav drawer icons from resources
        navMenuIcons = getResources()
                .obtainTypedArray(R.array.nav_drawer_icons);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.list_slidermenu);

        navDrawerItems = new ArrayList<>();

        // adding nav drawer items to array
        // Home
        navDrawerItems.add(new NavDrawerItem(navMenuTitles[0], navMenuIcons.getResourceId(0, -1)));
        // Tracks
        int count = TRACETracker.Client.getStoredTracksCount(this);
        navDrawerItems.add(new NavDrawerItem(navMenuTitles[1], navMenuIcons.getResourceId(1, -1), true, String.valueOf(count)));
        // Settings
        navDrawerItems.add(new NavDrawerItem(navMenuTitles[2], navMenuIcons.getResourceId(2, -1)));

        //LogOut
        navDrawerItems.add(new NavDrawerItem(navMenuTitles[3], navMenuIcons.getResourceId(3, -1)));

        // Recycle the typed array
        navMenuIcons.recycle();

        // setting the nav drawer list adapter
        adapter = new NavDrawerListAdapter(getApplicationContext(),
                navDrawerItems);
        mDrawerList.setAdapter(adapter);

        // enabling action bar app icon and behaving it as toggle button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);


        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                //R.drawable.ic_drawer, //nav menu toggle icon
                R.string.app_name, // nav drawer open - description for accessibility
                R.string.app_name // nav drawer close - description for accessibility
        ){
            public void onDrawerClosed(View view) {
                getSupportActionBar().setTitle(mTitle);
                // calling onPrepareOptionsMenu() to show action bar icons
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                getSupportActionBar().setTitle(mDrawerTitle);
                // calling onPrepareOptionsMenu() to hide action bar icons
                invalidateOptionsMenu();
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);

        if (savedInstanceState == null) {
            // on first time display view for first nav item
            displayView(0);
        }


        mDrawerList.setOnItemClickListener(new SlideMenuClickListener());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // toggle nav drawer on selecting action bar app icon/title
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle action bar actions click
        switch (item.getItemId()) {
            case R.id.action_settings:
                displayView(2);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // if nav drawer is opened, hide the action items
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        menu.findItem(R.id.action_settings).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getSupportActionBar().setTitle(title);
    }

    @Override
    public void onPostCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onPostCreate(savedInstanceState, persistentState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    /* Permissions
    /* Permissions
    /* Permissions
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,@NonNull String[] permissions,@NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }


    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        Log.e("PERMISSIONS!", "Permissions granted but on the mainActivity");
    }


    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Log.e("PERMISSIONS!", "Permissions denied but on the mainActivity");
    }

    @AfterPermissionGranted(TrackingConstants.permissions.DRAW_MAPS)
    private void redrawOSMDroidMap(){
        if(mCurrentFragment != null && mCurrentFragment instanceof MapViewFragment){
            ((MapViewFragment)mCurrentFragment).redrawMap();
        }
    }

    @AfterPermissionGranted(TrackingConstants.permissions.FOCUS_ON_MAP)
    private void focusOnMap(){
        if(mCurrentFragment != null && mCurrentFragment instanceof TrackingFragment){
            ((TrackingFragment)mCurrentFragment).focusOnCurrentLocation();
        }
    }

    @AfterPermissionGranted(TrackingConstants.permissions.TRACKING)
    private void startTracking(){
        if(mCurrentFragment != null && mCurrentFragment instanceof TrackingFragment){
            ((TrackingFragment)mCurrentFragment).startTracking();
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    /*
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    private class SlideMenuClickListener implements ListView.OnItemClickListener{

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            displayView(position);
        }
    }

    private HashMap<String, Fragment.SavedState> mFragmentStates = new HashMap<>();

    /**
     * Diplaying fragment view for selected nav drawer list item
     * */
    private void displayView(int position) {
        // update the main content by replacing fragments
        Fragment fragment = null;

        switch (position) {
            case 0:
                fragment = new HomeFragment();
                break;
            case 1:
                fragment = new TracksFragment();
                break;
            case 2:
                fragment = new SettingsFragment();
                break;
            case 3:
                buildLogoutDialogAlert();
                break;

            default:
                break;
        }

        if (fragment != null) {

            CURRENT_FRAG_TAG = fragment.getClass().getSimpleName();

            FragmentManager fragmentManager = getFragmentManager();

            Fragment.SavedState state =
                    saveCurrentStateAndLoadNext(mCurrentFragment, fragment, fragmentManager);

            if (state != null)
                fragment.setInitialSavedState(state);

            fragmentManager.beginTransaction()
                    .replace(R.id.frame_container, fragment, CURRENT_FRAG_TAG).commit();

            mCurrentFragment = fragment;

            // update selected item and title, then close the drawer
            mDrawerList.setItemChecked(position, true);
            mDrawerList.setSelection(position);
            setTitle(navMenuTitles[position]);
            mDrawerLayout.closeDrawer(mDrawerList);
        } else {
            // error in creating fragment
            Log.e("MainActivity", "Error in creating fragment");
        }
    }




    @Override
    public void onBackPressed() {
        if(mCurrentFragment!=null && !(mCurrentFragment instanceof HomeFragment))
            displayView(0);
        else{
            if(mCurrentFragment!=null && mCurrentFragment instanceof TrackingFragment) {

                final TrackingFragment trackingFragment = (TrackingFragment)mCurrentFragment;

                if(trackingFragment.isTracking()) {
                    new AlertDialog.Builder(this)
                            .setTitle("Exiting")
                            .setMessage("TrackerProto is currently tracking, are you sure you want to exit?")
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    trackingFragment.stopTracking();
                                    finish();
                                }
                            })
                            .setNegativeButton(R.string.no, null)
                            .show();
                }else
                    super.onBackPressed();


            }else {
                super.onBackPressed();
            }
        }
    }


    /* Fragment State Management
    /* Fragment State Management
    /* Fragment State Management
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    private String CURRENT_FRAG_TAG = "";

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Bundle extra = new Bundle();

        for(String frag : mFragmentStates.keySet())
            extra.putParcelable(frag, mFragmentStates.get(frag));

        outState.putParcelable("states", extra);
        outState.putString("current", CURRENT_FRAG_TAG);

        super.onSaveInstanceState(outState);
    }

    private void updateState(Bundle state){

        if(state.containsKey("states")){
            Bundle statesExtra = state.getParcelable("states");

            for(String key : statesExtra.keySet())
                mFragmentStates.put(key, (Fragment.SavedState) statesExtra.getParcelable(key));
        }

        if(state.containsKey("current")) {
            FragmentManager man = getFragmentManager();
            CURRENT_FRAG_TAG = state.getString("current");
            mCurrentFragment = man.findFragmentByTag(CURRENT_FRAG_TAG);
        }

    }

    private Fragment.SavedState saveCurrentStateAndLoadNext(Fragment current, Fragment next, FragmentManager manager){

        String currentKey, nextKey;

        //Step 1 - Save the current fragment's state if he exists
        if(current != null) {
            currentKey = mCurrentFragment.getClass().getSimpleName();
            Fragment.SavedState saveState = manager.saveFragmentInstanceState(mCurrentFragment);
            mFragmentStates.put(currentKey, saveState);
        }

        //Step 2 - Load the new fragment's state if exists
        nextKey = next.getClass().getSimpleName();
        Fragment.SavedState state = null;
        if (mFragmentStates.containsKey(nextKey))
            state = mFragmentStates.get(nextKey);

        return state;
    }

    /* Update Track Count
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */

    @Override
    public void updateTrackCount(){
        int count = TRACETracker.Client.getStoredTracksCount(this);
        navDrawerItems.get(1).setCount(String.valueOf(count));
    }


    /* Logout
    /* Logout
    /* Logout
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    private void buildLogoutDialogAlert() {

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.logout))
                .setMessage(getString(R.string.logout_rationale))
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //AuthenticationManager.clearCredentials(MainActivity.this);
                        Log.e(LOG_TAG, "Not doing anything right now when logging out...");
                        Intent logoutIntent = new Intent(MainActivity.this, LoginActivity.class);
                        startActivity(logoutIntent);
                        finish();
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    /* Security
    /* Security
    /* Security
     ***********************************************************************************************
     ***********************************************************************************************
     ***********************************************************************************************
     */
    private TraceAuthenticationManager mAuthManager;
    private GoogleApiClient mGoogleApiClient;

    private void setupTraceAuthenticationManager(){
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .addApi(Auth.CREDENTIALS_API)
                .build();

        mAuthManager = TraceAuthenticationManager.getAuthenticationManager(this, mGoogleApiClient);
    }

    public String getAuthenticationToken(){ //TODO: migrar isto para uma interface

        String authToken = "";

        try {
            authToken = mAuthManager.getAuthenticationToken();
        } catch (UserIsNotLoggedException e) {
            e.printStackTrace();
        }

        Log.d(LOG_TAG, authToken);
        return authToken;
    }

    //TESTING
    //TODO: remover
    public void forceFetchNewSession(){
        mAuthManager.fetchNewTrackingSession();
    }
}
