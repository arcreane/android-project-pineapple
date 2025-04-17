package com.pineapple.capture.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.pineapple.capture.BuildConfig;

import java.util.HashMap;
import java.util.Map;

public class CloudinaryManager {

    private static final String TAG = "CloudinaryManager";
    private static boolean isInitialized = false;

    public static void init(Context context) {
        String cloudName = BuildConfig.CLOUDINARY_CLOUD_NAME;
        String apiKey = BuildConfig.CLOUDINARY_API_KEY;
        String apiSecret = BuildConfig.CLOUDINARY_API_SECRET;

        if (!isInitialized) {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", cloudName);
            config.put("api_key", apiKey);
            config.put("api_secret", apiSecret);
            MediaManager.init(context, config);
            isInitialized = true;
        }
    }

    public static void uploadImage(Uri fileUri, UploadCallback callback) {
        MediaManager.get().upload(fileUri)
                .option("upload_preset", "unsigned_feed_uploads")
                .callback(callback)
                .dispatch();
    }

    /**
     * Extract the secure URL from the Cloudinary response
     * @param resultData The result map from Cloudinary upload
     * @return A cleaned and properly formatted image URL
     */
    public static String getImageUrl(Map resultData) {
        if (resultData == null) {
            Log.e(TAG, "Result data is null");
            return "";
        }

        // Try to get the secure URL first (recommended)
        String secureUrl = (String) resultData.get("secure_url");
        if (secureUrl != null && !secureUrl.isEmpty()) {
            Log.d(TAG, "Using secure_url: " + secureUrl);
            return secureUrl;
        }

        // Fall back to regular URL if secure is not available
        String url = (String) resultData.get("url");
        if (url != null && !url.isEmpty()) {
            // Convert to https if it's http
            if (url.startsWith("http:")) {
                url = url.replace("http:", "https:");
            }
            Log.d(TAG, "Using url: " + url);
            return url;
        }

        // If all else fails, try to construct the URL from public_id
        String publicId = (String) resultData.get("public_id");
        if (publicId != null && !publicId.isEmpty()) {
            String format = (String) resultData.get("format");
            if (format == null || format.isEmpty()) {
                format = "jpg"; // Default format
            }
            String cloudName = BuildConfig.CLOUDINARY_CLOUD_NAME;
            String constructedUrl = "https://res.cloudinary.com/" + cloudName + "/image/upload/" + publicId + "." + format;
            Log.d(TAG, "Constructed url: " + constructedUrl);
            return constructedUrl;
        }

        Log.e(TAG, "Could not extract image URL from: " + resultData);
        return "";
    }
}
