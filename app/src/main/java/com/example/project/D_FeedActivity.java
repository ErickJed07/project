package com.example.project;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
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

public class D_FeedActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private D_FeedAdapter postAdapter;
    private List<I_PostEvent> postList;
    private DatabaseReference postsRef;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private View notificationBadge;
    private View updateBanner;
    private String pendingApkUrl;

    private long downloadId = -1;

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.d1_feed);

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAffinity();
            }
        });

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        recyclerView = findViewById(R.id.feedrecyclerView);
        progressBar = findViewById(R.id.my_progress_bar);
        
        notificationBadge = findViewById(R.id.notificationBadge);
        View notificationIcon = findViewById(R.id.NotificationIcon);
        if (notificationIcon != null) {
            notificationIcon.setOnClickListener(v -> {
                Intent intent = new Intent(D_FeedActivity.this, NotificationActivity.class);
                startActivity(intent);
            });
        }
        
        if (notificationBadge != null) {
            checkUnreadNotifications();
        }

        /* 
        updateBanner = findViewById(R.id.updateBanner);
        Button btnUpdateNow = findViewById(R.id.btnUpdateNow);
        if (btnUpdateNow != null) {
            btnUpdateNow.setOnClickListener(v -> {
                if (pendingApkUrl != null) showUpdateDialog(pendingApkUrl);
            });
        }

        View logoFeed = findViewById(R.id.logo_feed);
        if (logoFeed != null) {
            logoFeed.setOnClickListener(v -> {
                Toast.makeText(this, "Checking for updates...", Toast.LENGTH_SHORT).show();
                checkForUpdates();
            });
        }
        */

        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);

        postList = new ArrayList<>();
        postAdapter = new D_FeedAdapter(this, postList);
        recyclerView.setAdapter(postAdapter);

        postsRef = FirebaseDatabase.getInstance().getReference("PostEvents");

        swipeRefreshLayout.setOnRefreshListener(() -> fetchPostsFromFirebase());
        swipeRefreshLayout.setDistanceToTriggerSync(300);

        // checkForUpdates();
        fetchPostsFromFirebase();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(onDownloadComplete); } catch (Exception e) {}
    }

    private final BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (downloadId == id) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
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
                        if (jsonObject.getInt("version_code") > BuildConfig.VERSION_CODE) {
                            pendingApkUrl = jsonObject.getString("apk_url");
                            if (updateBanner != null) updateBanner.setVisibility(View.VISIBLE);
                            showUpdateDialog(pendingApkUrl);
                        }
                    } catch (JSONException e) { e.printStackTrace(); }
                }, error -> {});
        stringRequest.setShouldCache(false);
        queue.add(stringRequest);
    }

    private void showUpdateDialog(String apkUrl) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Update Available")
                .setMessage("A new version of the app is available. Do you want to update?")
                .setPositiveButton("Yes", (dialog, which) -> downloadUpdate(apkUrl))
                .setNegativeButton("No", null)
                .setCancelable(false)
                .show();
    }

    private void downloadUpdate(String apkUrl) {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Downloading update...", Toast.LENGTH_SHORT).show();

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
        request.setTitle("App Update");
        request.setDescription("Downloading new version...");
        request.setDestinationInExternalFilesDir(this, null, "update.apk");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        if (manager != null) {
            downloadId = manager.enqueue(request);
        }
    }

    private void installApk() {
        try {
            File file = new File(getExternalFilesDir(null), "update.apk");
            if (!file.exists()) return;
            Uri apkUri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Installation failed.", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchPostsFromFirebase() {
        postsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                postList.clear();
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    I_PostEvent postEvent = postSnapshot.getValue(I_PostEvent.class);
                    if (postEvent != null) {
                        postEvent.setPostId(postSnapshot.getKey());
                        postList.add(postEvent);
                    }
                }
                sortPostsByDate();
                postAdapter.notifyDataSetChanged();
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            }
            @Override public void onCancelled(DatabaseError error) {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void checkUnreadNotifications() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        if (uid.isEmpty()) return;
        
        FirebaseDatabase.getInstance().getReference("Notifications").child(uid)
                .orderByChild("read").equalTo(false)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (notificationBadge != null) {
                            notificationBadge.setVisibility(snapshot.exists() ? View.VISIBLE : View.GONE);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void sortPostsByDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        Collections.sort(postList, (post1, post2) -> {
            try {
                Date date1 = dateFormat.parse(post1.getDate());
                Date date2 = dateFormat.parse(post2.getDate());
                return date2.compareTo(date1);
            } catch (Exception e) { return 0; }
        });
    }

    public void onButtonClicked(View view) {
        Intent intent = null;
        int viewId = view.getId();
        if (viewId == R.id.home_menu) {
            // Already here
            return;
        } else if (viewId == R.id.wardrobe_menu) {
            intent = new Intent(this, WardrobeActivity.class);
        } else if (viewId == R.id.calendar_menu) {
            intent = new Intent(this, E_CalendarActivity.class);
        } else if (viewId == R.id.discover_menu) {
            intent = new Intent(this, DiscoverActivity.class);
        } else if (viewId == R.id.profile_menu) {
            intent = new Intent(this, I_ProfileActivity.class);
        }
        
        if (intent != null) {
            startActivity(intent);
            finish();
        }
    }
}
//ereefe