package com.example.project;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;

public class UpdateHelper {

    public static void checkForUpdates(Activity activity, UpdateCallback callback) {
        String versionUrl = "https://raw.githubusercontent.com/ErickJed07/project/main/app-updates/version.json?t=" + System.currentTimeMillis();
        RequestQueue queue = Volley.newRequestQueue(activity);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, versionUrl,
                response -> {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        int latestVersion = jsonObject.getInt("version_code");
                        int currentVersion = BuildConfig.VERSION_CODE;

                        // This will show exactly what the app "sees"
                        Toast.makeText(activity, "Checking: App(" + currentVersion + ") vs GitHub(" + latestVersion + ")", Toast.LENGTH_LONG).show();

                        if (latestVersion > currentVersion) {
                            String apkUrl = jsonObject.getString("apk_url");
                            showForcedUpdateDialog(activity, apkUrl);
                            if (callback != null) callback.onUpdateAvailable(apkUrl);
                        } else {
                            if (callback == null) {
                                Toast.makeText(activity, "No update needed.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(activity, "Update check error: JSON Error", Toast.LENGTH_SHORT).show();
                    }
                }, error -> {
                    error.printStackTrace();
                    Toast.makeText(activity, "Update check error: Network Failed", Toast.LENGTH_SHORT).show();
                });
        stringRequest.setShouldCache(false);
        queue.add(stringRequest);
    }

    public static void showForcedUpdateDialog(Activity activity, String apkUrl) {
        if (activity.isFinishing() || activity.isDestroyed()) return;

        new AlertDialog.Builder(activity)
                .setTitle("Critical Update Required")
                .setMessage("A new version of Phindee is available. You must update the app to continue using it.")
                .setPositiveButton("Update Now", (dialog, which) -> {
                    // Check if the activity is D_FeedActivity to use its tracking logic
                    if (activity instanceof D_FeedActivity) {
                        ((D_FeedActivity) activity).downloadUpdate(apkUrl);
                    } else {
                        // Fallback if called from elsewhere
                        Toast.makeText(activity, "Starting download...", Toast.LENGTH_SHORT).show();
                        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(apkUrl));
                        activity.startActivity(intent);
                    }
                })
                .setCancelable(false)
                .show();
    }

    public interface UpdateCallback {
        void onUpdateAvailable(String apkUrl);
    }
}
