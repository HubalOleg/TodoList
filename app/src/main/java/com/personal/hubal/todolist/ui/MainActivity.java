package com.personal.hubal.todolist.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.personal.hubal.todolist.Constants;
import com.personal.hubal.todolist.R;
import com.personal.hubal.todolist.TodoApplication;
import com.personal.hubal.todolist.adapters.FirebaseTaskListAdapter;
import com.personal.hubal.todolist.adapters.FirebaseTaskListEventListener;
import com.personal.hubal.todolist.adapters.SimpleItemTouchHelperCallback;
import com.personal.hubal.todolist.models.TodoTask;
import com.personal.hubal.todolist.util.util;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        GoogleApiClient.OnConnectionFailedListener,
        FirebaseTaskListEventListener {

    private static final String TAG = "MainActivity";

    private GoogleApiClient mGoogleApiClient;
    private TodoApplication mApplication;

    // RecyclerView instance variables
    private RecyclerView mTaskRecyclerView;
    private FirebaseTaskListAdapter mFirebaseAdapter;
    private ItemTouchHelper mItemTouchHelper;

    // Firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;

    // UI references.
    private LinearLayout mSplashView;
    private View mNavHeaderView;
    private ShowcaseView mShowcaseView;
    private FloatingActionButton mFloatingActionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mApplication = (TodoApplication) getApplication();

        mTaskRecyclerView = (RecyclerView) findViewById(R.id.taskRecyclerView);
        mTaskRecyclerView.setHasFixedSize(true);
        mTaskRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        progressBarVisible(View.INVISIBLE);
        setUpSplash();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Navigation View
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        mNavHeaderView = navigationView.inflateHeaderView(R.layout.nav_header_main);

        mFloatingActionButton = (FloatingActionButton) findViewById(R.id.fab);

        if (getIsFirst()) {
        } else {
            setSplashVisible(View.VISIBLE);
        }

        mFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startTaskEditActivity(new TodoTask(), Constants.NUM_UNDEFINED);

                hideCoachMark();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        // Initialize Google Api Client.
        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        mApplication.setGoogleApiClient(mGoogleApiClient);

        setUpFirebaseAuth();

        setUpFirebaseAdapter();

        setNavHeader();
    }

    private boolean getIsFirst() {
        Log.d(TAG, "getIsFirst");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return prefs.getBoolean(Constants.STR_IS_FIRST, true);
    }

    private void setIsFirst(boolean isFirst) {
        Log.d(TAG, "setIsFirst");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(Constants.STR_IS_FIRST, isFirst);
        editor.apply();
    }

    private void setUpSplash() {
        Log.d(TAG, "setUpSplash");

        mSplashView = (LinearLayout) findViewById(R.id.splash);

        TextView splushVerStrTextView = (TextView) findViewById(R.id.splash_ver_str);
        splushVerStrTextView.setText(getString(R.string.version_str, util.getVersionName(this)));
    }

    private void setUpFirebaseAuth() {
        Log.d(TAG, "setUpFirebaseAuth");

        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();
        mApplication.setFirebaseAuth(mFirebaseAuth);

        mFirebaseUser = mFirebaseAuth.getCurrentUser();

        if (mFirebaseUser == null) {
            startLoginActivity();
        }
    }

    private void setUpFirebaseAdapter() {
        Log.d(TAG, "setUpFirebaseAdapter");

        if (mFirebaseUser == null)
            return;

        // uid/tasks
        DatabaseReference dbRef = FirebaseDatabase.getInstance()
                .getReference(mFirebaseUser.getUid())
                .child(Constants.FIREBASE_DB_TASKS_CHILD);

        dbRef.runTransaction(new Transaction.Handler() {

            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b,
                                   DataSnapshot dataSnapshot) {
                // Transaction completed
                Log.d(TAG, "postTransaction:onComplete:" + databaseError);

                long taskCount = dataSnapshot.getChildrenCount();

                if (taskCount == 0) {
                    setSplashVisible(View.GONE);
//                    progressBarVisible(ProgressBar.INVISIBLE);
                }
            }
        });

        Query query = dbRef.orderByChild(Constants.FIREBASE_QUERY_INDEX);

        mFirebaseAdapter = new FirebaseTaskListAdapter(
                TodoTask.class,
                R.layout.item_task,
                FirebaseTaskViewHolder.class,
                query,
                this, this);

        mTaskRecyclerView.setAdapter(mFirebaseAdapter);

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mFirebaseAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(mTaskRecyclerView);
    }

    public void setNavHeader() {
        Log.d(TAG, "setNavHeader");

        if (mFirebaseUser == null)
            return;

        // image
        CircleImageView navheaderImage = (CircleImageView) mNavHeaderView.findViewById(R.id.nav_header_image);
        if (mFirebaseUser.getPhotoUrl() != null) {
            Glide.with(MainActivity.this)
                    .load(mFirebaseUser.getPhotoUrl().toString())
                    .into(navheaderImage);
        } else {
            navheaderImage.setImageDrawable(ContextCompat
                    .getDrawable(MainActivity.this,
                            R.drawable.ic_account_circle_black_36dp));
        }

        TextView navheaderName = (TextView) mNavHeaderView.findViewById(R.id.nav_header_name);
        navheaderName.setText(mFirebaseUser.getDisplayName());

        TextView navheaderEmail = (TextView) mNavHeaderView.findViewById(R.id.nav_header_email);
        navheaderEmail.setText(mFirebaseUser.getEmail());
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed");

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();

        if (mFirebaseAdapter != null)
            mFirebaseAdapter.setIndexInFirebase();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();

        if (mFirebaseAdapter != null) {
            mFirebaseAdapter.cleanup();
            mItemTouchHelper = null;
        }
    }

    // RecyclerView Event
    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        Log.d(TAG, "onStartDrag");
        mItemTouchHelper.startDrag(viewHolder);
    }

    @Override
    public void onClickItem(RecyclerView.ViewHolder viewHolder) {
        Log.d(TAG, "onClickItem");

        TodoTask task = ((FirebaseTaskViewHolder) viewHolder).getTodoTask();
        startTaskEditActivity(task, viewHolder.getAdapterPosition());
    }

    @Override
    public void onAddItem() {
        Log.d(TAG, "onAddItem");

        mTaskRecyclerView.getLayoutManager().scrollToPosition(0);
    }

    @Override
    public void onPopulateViewHolder() {
        Log.d(TAG, "onPopulateViewHolder");

        setSplashVisible(View.GONE);
//        progressBarVisible(ProgressBar.INVISIBLE);
    }

    @Override
    public void onItemDismiss() {
        Log.d(TAG, "onItemDismiss");

        FrameLayout layout = (FrameLayout) findViewById(R.id.mainActivityLayout);
        Snackbar.make(layout, getString(R.string.snackbar_text), Snackbar.LENGTH_SHORT)
                .show();
    }

    private void startLoginActivity() {
        Log.d(TAG, "startLoginActivity");

        Intent intent = new Intent(getApplication(), LoginActivity.class);
        startActivityForResult(intent, Constants.RESULT_LOGINACTIVITY);
    }

    private void startTaskEditActivity(TodoTask task, int position) {
        Log.d(TAG, "startTaskEditActivity");

        Intent intent = new Intent(getApplication(), TaskEditActivity.class);
        intent.putExtra(Constants.STR_TASK, task);
        intent.putExtra(Constants.STR_POSITION, position);

        startActivityForResult(intent, Constants.RESULT_TASKEDITACTIVITY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult");

        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            switch (requestCode) {
                case Constants.RESULT_TASKEDITACTIVITY:
                    TodoTask task = (TodoTask) data.getSerializableExtra(Constants.STR_TASK);
                    int position = data.getIntExtra(Constants.STR_POSITION, Constants.NUM_UNDEFINED);
                    UpdateTask(task, position);
                    break;
                case Constants.RESULT_LOGINACTIVITY:
                    if (getIsFirst()) {
                        showCoachMark(mFloatingActionButton);
                        setIsFirst(false);
                    }
                    changeLoginUser();
                    break;
                default:
                    break;
            }
        }
    }

    private void UpdateTask(TodoTask task, int position) {
        Log.d(TAG, "UpdateTask");

        if (position != Constants.NUM_UNDEFINED) {
            mFirebaseAdapter.getRef(position).setValue(task);
        } else {
            DatabaseReference dbRef = FirebaseDatabase
                    .getInstance()
                    .getReference(mFirebaseUser.getUid())
                    .child(Constants.FIREBASE_DB_TASKS_CHILD);

            if (dbRef != null)
                dbRef.push().setValue(task);
        }
    }

    private void changeLoginUser() {
        Log.d(TAG, "changeLoginUser");

        setUpFirebaseAuth();
        setUpFirebaseAdapter();
        setNavHeader();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu");

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected");

        switch (item.getItemId()) {
            case R.id.sign_out_menu: {
                mFirebaseAuth.signOut();
                Auth.GoogleSignInApi.signOut(mGoogleApiClient);
                startLoginActivity();
                return true;
            }
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
        Toast.makeText(this, "Google Play Services error.", Toast.LENGTH_SHORT).show();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        Log.d(TAG, "onNavigationItemSelected");

        switch (item.getItemId()) { // TODO:
            default:
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void progressBarVisible(int visible) {
        Log.d(TAG, "progressBarVisible");
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(visible);
    }

    private void setSplashVisible(int visible) {
        Log.d(TAG, "setSplashVisible");

        if (mSplashView == null || mSplashView.getVisibility() == visible)
            return;

        if (visible == View.GONE) {
            AlphaAnimation animation;
            animation = new AlphaAnimation(1, 0);
            animation.setDuration(100);
            mSplashView.startAnimation(animation);
        }

        mSplashView.setVisibility(visible);
    }

    private void showCoachMark(View view) {
        Log.d(TAG, "showCoachMark");

        mShowcaseView = new ShowcaseView.Builder(this)
                .setTarget(new ViewTarget(view))
                .setContentTitle(getString(R.string.coach_mark_title))
                .setContentText(getString(R.string.coach_mark_text))
                .hideOnTouchOutside()
                .setStyle(R.style.CustomShowcaseTheme)
                .withMaterialShowcase()
                .doNotBlockTouches()
                .build();
        mShowcaseView.hideButton();
    }

    private void hideCoachMark() {

        if (mShowcaseView != null)
            mShowcaseView.hide();
    }
}
