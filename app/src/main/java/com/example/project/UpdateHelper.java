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
                        if (latestVersion > BuildConfig.VERSION_CODE) {
                            String apkUrl = jsonObject.getString("apk_url");
                            showForcedUpdateDialog(activity, apkUrl);
                            if (callback != null) callback.onUpdateAvailable(apkUrl);
                        } else {
                            if (callback == null) {
                                Toast.makeText(activity, "App is up to date!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }, error -> {});
        stringRequest.setShouldCache(false);
        queue.add(stringRequest);
    }

    public static void showForcedUpdateDialog(Activity activity, String apkUrl) {
        if (activity.isFinishing() || activity.isDestroyed()) return;

        new AlertDialog.Builder(activity)
                .setTitle("Critical Update Required")
                .setMessage("A new version of Phindee is available. You must update the app to continue using it.")
                .setPositiveButton("Update Now", (dialog, which) -> {
                    downloadUpdate(activity, apkUrl);
                    // Do not dismiss or allow continuation
                })
                .setCancelable(false)
                .show();
    }

    private static void downloadUpdate(Context context, String apkUrl) {
        Toast.makeText(context, "Downloading update...", Toast.LENGTH_SHORT).show();
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
        request.setTitle("Phindee Update");
        request.setDescription("Downloading critical update...");
        request.setDestinationInExternalFilesDir(context, null, "update.apk");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (manager != null) {
            manager.enqueue(request);
        }
    }

    public interface UpdateCallback {
        void onUpdateAvailable(String apkUrl);
    }
}
