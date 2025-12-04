package com.example.project;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class D1_FeedActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private D_FeedAdapter postAdapter;
    private List<I_NewPost_Event> postList;
    private DatabaseReference postsRef;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;

    // For handling the download ID
    private long downloadId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.d1_feed);

        // Initialize Views
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        recyclerView = findViewById(R.id.feedrecyclerView);
        progressBar = findViewById(R.id.my_progress_bar);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        postList = new ArrayList<>();
        postAdapter = new D_FeedAdapter(this, postList);
        recyclerView.setAdapter(postAdapter);

        postsRef = FirebaseDatabase.getInstance().getReference("PostEvents");

        // Set up the Refresh Listener
        swipeRefreshLayout.setOnRefreshListener(() -> fetchPostsFromFirebase());
        swipeRefreshLayout.setDistanceToTriggerSync(300);

        // Check for updates
        checkForUpdates();

        // Fetch posts from Firebase
        fetchPostsFromFirebase();

        // Register receiver for when download completes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(onDownloadComplete);
        } catch (Exception e) {
            // Receiver might not be registered
        }
    }

    // Receiver to detect when download is finished
    private final BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (downloadId == id) {
                installApk();
            }
        }
    };

    private void checkForUpdates() {
        String versionUrl = "https://raw.githubusercontent.com/ErickJed07/project/main/app-updates/version.json?t=" + System.currentTimeMillis();

        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, versionUrl,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        int latestVersionCode = jsonObject.getInt("version_code");
                        String apkUrl = jsonObject.getString("apk_url");

                        if (latestVersionCode > BuildConfig.VERSION_CODE) {
                            showUpdateDialog(apkUrl);
                        } else {
                            // Toast.makeText(D1_FeedActivity.this, "App is up to date", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    // Error handling
                });

        stringRequest.setShouldCache(false);
        queue.add(stringRequest);
    }

    private void showUpdateDialog(final String apkUrl) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("New Update Available")
                .setMessage("A new version of the app is available. Downloading will happen in the background.")
                .setPositiveButton("Update Now", (dialog, which) -> downloadAndInstallApk(apkUrl))
                .setNegativeButton("Later", null)
                .setCancelable(false)
                .show();
    }

    private void downloadAndInstallApk(String apkUrl) {
        Toast.makeText(this, "Downloading update...", Toast.LENGTH_LONG).show();

        // 1. Prepare the file path in app-specific storage (No permissions needed here)
        File file = new File(getExternalFilesDir(null), "update.apk");
        if (file.exists()) {
            file.delete();
        }

        // 2. Create Request
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
        request.setTitle("App Update");
        request.setDescription("Downloading new version...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);

        // IMPORTANT: This ensures the file is saved where FileProvider can read it
        request.setDestinationInExternalFilesDir(this, null, "update.apk");
        request.setMimeType("application/vnd.android.package-archive");

        // 3. Enqueue
        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        downloadId = manager.enqueue(request);
    }

    private void installApk() {
        try {
            File file = new File(getExternalFilesDir(null), "update.apk");

            // Get the URI using the FileProvider
            // Authority must match: applicationId + ".fileprovider"
            Uri apkUri = FileProvider.getUriForFile(
                    this,
                    BuildConfig.APPLICATION_ID + ".fileprovider",
                    file
            );

            Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            intent.setData(apkUri);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Required when starting from non-activity context

            startActivity(intent);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Installation failed. Please try again.", Toast.LENGTH_SHORT).show();

            // Fallback: If internal install fails, open browser
            // openBrowserFallback();
        }
    }

    private void fetchPostsFromFirebase() {
        postsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                postList.clear();
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    I_NewPost_Event postEvent = postSnapshot.getValue(I_NewPost_Event.class);
                    if (postEvent != null) {
                        postList.add(postEvent);
                    }
                }
                sortPostsByDate();
                postAdapter.notifyDataSetChanged();
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(D1_FeedActivity.this, "Failed to load posts", Toast.LENGTH_SHORT).show();
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void sortPostsByDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        Collections.sort(postList, (post1, post2) -> {
            try {
                Date date1 = dateFormat.parse(post1.getDate());
                Date date2 = dateFormat.parse(post2.getDate());
                return date2.compareTo(date1);
            } catch (Exception e) {
                return 0;
            }
        });
    }

    public void onButtonClicked(View view) {
        Intent intent = null;
        int viewId = view.getId();
        if (viewId == R.id.home_menu) intent = new Intent(this, D1_FeedActivity.class);
        else if (viewId == R.id.calendar_menu) intent = new Intent(this, E1_CalendarActivity.class);
        else if (viewId == R.id.camera_menu) intent = new Intent(this, F1_CameraActivity.class);
        else if (viewId == R.id.closet_menu) intent = new Intent(this, G1_ClosetActivity.class);
        else if (viewId == R.id.profile_menu) intent = new Intent(this, I1_ProfileActivity.class);

        if (intent != null) {
            startActivity(intent);
            finish();
        }
    }
}
