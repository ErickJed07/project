package com.example.project;

import android.content.Intent;
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
import com.android.volley.Response;
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
import java.io.IOException;
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
    private ProgressBar progressBar;  // Progress bar to show download progress

    // Add ?t= + System.currentTimeMillis() to force a fresh download every time
    private static final String VERSION_URL = "https://raw.githubusercontent.com/ErickJed07/project/main/app-updates/version.json?t=" + System.currentTimeMillis();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.d1_feed);

        // Initialize Views
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        recyclerView = findViewById(R.id.feedrecyclerView);
        // In onCreate()
        progressBar = findViewById(R.id.my_progress_bar);
        // Progress bar for download

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        postList = new ArrayList<>();
        postAdapter = new D_FeedAdapter(this, postList);
        recyclerView.setAdapter(postAdapter);

        postsRef = FirebaseDatabase.getInstance().getReference("PostEvents");

        // Set up the Refresh Listener
        swipeRefreshLayout.setOnRefreshListener(() -> fetchPostsFromFirebase());
        swipeRefreshLayout.setDistanceToTriggerSync(300);

        // Check for updates after login or when the feed activity is opened
        checkForUpdates();

        // Fetch posts from Firebase
        fetchPostsFromFirebase();
    }

    private void checkForUpdates() {
        // Request queue for network calls
        RequestQueue queue = Volley.newRequestQueue(this);

        // Create a StringRequest to fetch the version.json
        StringRequest stringRequest = new StringRequest(Request.Method.GET, VERSION_URL,
                new Response.Listener<String>() {
                    @Override
                    // Inside D1_FeedActivity.java -> checkForUpdates()
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            int latestVersionCode = jsonObject.getInt("version_code");
                            String apkUrl = jsonObject.getString("apk_url");

                            // LOGGING FOR DEBUGGING
                            System.out.println("Current Version: " + BuildConfig.VERSION_CODE);
                            System.out.println("Remote Version: " + latestVersionCode);

                            if (latestVersionCode > BuildConfig.VERSION_CODE) {
                                showUpdateDialog(apkUrl);
                            } else {
                                // Temporary Toast to confirm the check ran successfully
                                Toast.makeText(D1_FeedActivity.this, "App is up to date", Toast.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                }, error -> {
            // Handle error
            Toast.makeText(D1_FeedActivity.this, "Error fetching version info", Toast.LENGTH_SHORT).show();
        });

        // Add the request to the queue
        queue.add(stringRequest);
    }

    private void showUpdateDialog(final String apkUrl) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("New Update Available")
                .setMessage("A new version of the app is available. Do you want to update?")
                .setPositiveButton("Yes", (dialog, which) -> downloadAndInstallApk(apkUrl))
                .setNegativeButton("No", null)
                .setCancelable(false)
                .show();
    }

    private void downloadAndInstallApk(String apkUrl) {
        // Show progress bar while downloading
        progressBar.setVisibility(View.VISIBLE);

        // Create a new file for saving the APK
        File apkFile = new File(getExternalFilesDir(null), "app-release.apk");

        // Start downloading the APK file
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        FileDownloadRequest request = new FileDownloadRequest(apkUrl, apkFile, new Response.Listener<File>() {
            @Override
            public void onResponse(File file) {
                // Hide the progress bar once the download is complete
                progressBar.setVisibility(View.GONE);

                Toast.makeText(D1_FeedActivity.this, "Download Complete!", Toast.LENGTH_SHORT).show();
                // Install the APK after download
                installApk(file);
            }
        }, error -> {
            // Hide progress bar if download fails
            progressBar.setVisibility(View.GONE);
            Toast.makeText(D1_FeedActivity.this, "Download Failed", Toast.LENGTH_SHORT).show();
        });

        // Add the request to the queue to start downloading
        requestQueue.add(request);
    }

    private void installApk(File file) {
        Uri apkUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // For Android N and above, use FileProvider
            apkUri = FileProvider.getUriForFile(this, "com.example.project.fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
            intent.setData(apkUri);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } else {
            // For older versions, just use Uri.fromFile
            apkUri = Uri.fromFile(file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
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

                // Stop the refreshing animation when data loads
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(D1_FeedActivity.this, "Failed to load posts", Toast.LENGTH_SHORT).show();

                // Stop animation even if it fails
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
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
                e.printStackTrace();
                return 0;
            }
        });
    }

    public void onButtonClicked(View view) {
        Intent intent = null;
        int viewId = view.getId();

        if (viewId == R.id.home_menu) {
            intent = new Intent(this, D1_FeedActivity.class);
        } else if (viewId == R.id.calendar_menu) {
            intent = new Intent(this, E1_CalendarActivity.class);
        } else if (viewId == R.id.camera_menu) {
            intent = new Intent(this, F1_CameraActivity.class);
        } else if (viewId == R.id.closet_menu) {
            intent = new Intent(this, G1_ClosetActivity.class);
        } else if (viewId == R.id.profile_menu) {
            intent = new Intent(this, I1_ProfileActivity.class);
        }

        if (intent != null) {
            startActivity(intent);
            finish();
        }
    }
}
