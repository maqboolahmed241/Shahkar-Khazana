package com.maqboolahmed.backuppicker;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class BackupPicker extends CordovaPlugin {
    private static final int REQUEST_CREATE_DOCUMENT = 48172;
    private CallbackContext pendingCallback;
    private String pendingJson;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (!"saveJson".equals(action)) return false;
        if (pendingCallback != null) {
            callbackContext.error("A save operation is already running");
            return true;
        }
        final String filename = args.getString(0);
        final String json = args.getString(1);
        pendingJson = json;
        pendingCallback = callbackContext;
        cordova.getActivity().runOnUiThread(() -> {
            try {
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_TITLE, filename);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                cordova.startActivityForResult(this, intent, REQUEST_CREATE_DOCUMENT);
            } catch (Exception e) {
                CallbackContext cb = pendingCallback;
                pendingCallback = null;
                pendingJson = null;
                if (cb != null) cb.error("Unable to open save location: " + e.getMessage());
            }
        });
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode != REQUEST_CREATE_DOCUMENT || pendingCallback == null) return;
        CallbackContext callback = pendingCallback;
        pendingCallback = null;
        if (resultCode != Activity.RESULT_OK || intent == null || intent.getData() == null) {
            pendingJson = null;
            callback.error("cancelled");
            return;
        }
        Uri uri = intent.getData();
        try (OutputStream output = cordova.getActivity().getContentResolver().openOutputStream(uri, "wt")) {
            if (output == null) throw new Exception("Unable to open selected file");
            output.write(pendingJson.getBytes(StandardCharsets.UTF_8));
            output.flush();
            pendingJson = null;
            JSONObject result = new JSONObject();
            result.put("saved", true);
            result.put("uri", uri.toString());
            callback.success(result);
        } catch (Exception e) {
            pendingJson = null;
            callback.error("Save failed: " + e.getMessage());
        }
    }
}