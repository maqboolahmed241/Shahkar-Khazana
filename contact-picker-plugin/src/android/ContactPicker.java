package com.maqboolahmed.contactpicker;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ContactPicker extends CordovaPlugin {
    private static final int REQUEST_CONTACT_PERMISSION = 48172;
    private static final int REQUEST_PICK_CONTACT = 48173;
    private CallbackContext pendingCallback;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (!"pick".equals(action)) return false;
        if (pendingCallback != null) {
            callbackContext.error("A contact selection is already open");
            return true;
        }
        pendingCallback = callbackContext;
        cordova.getActivity().runOnUiThread(() -> {
            try {
                if (cordova.hasPermission(Manifest.permission.READ_CONTACTS)) {
                    openContactPicker();
                } else {
                    cordova.requestPermission(this, REQUEST_CONTACT_PERMISSION, Manifest.permission.READ_CONTACTS);
                }
            } catch (Exception e) {
                fail("Unable to request contacts permission: " + e.getMessage());
            }
        });
        return true;
    }

    private void openContactPicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setData(ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            cordova.startActivityForResult(this, intent, REQUEST_PICK_CONTACT);
        } catch (Exception e) {
            fail("Unable to open contacts: " + e.getMessage());
        }
    }

    private void fail(String message) {
        CallbackContext cb = pendingCallback;
        pendingCallback = null;
        if (cb != null) cb.error(message);
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
        if (requestCode != REQUEST_CONTACT_PERMISSION || pendingCallback == null) return;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openContactPicker();
        } else {
            fail("Contacts permission denied. Allow Contacts permission in Android App settings and try again.");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode != REQUEST_PICK_CONTACT || pendingCallback == null) return;
        CallbackContext callback = pendingCallback;
        pendingCallback = null;
        if (resultCode != Activity.RESULT_OK || intent == null || intent.getData() == null) {
            callback.error("cancelled");
            return;
        }
        Cursor cursor = null;
        try {
            Uri uri = intent.getData();
            String[] columns = {
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            };
            cursor = cordova.getActivity().getContentResolver().query(uri, columns, null, null, null);
            if (cursor == null || !cursor.moveToFirst()) {
                callback.error("The selected contact has no readable phone number");
                return;
            }
            int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            JSONObject result = new JSONObject();
            result.put("name", nameIndex >= 0 ? cursor.getString(nameIndex) : "");
            result.put("phone", numberIndex >= 0 ? cursor.getString(numberIndex) : "");
            callback.success(result);
        } catch (Exception e) {
            callback.error("Could not read selected contact: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
        }
    }
}