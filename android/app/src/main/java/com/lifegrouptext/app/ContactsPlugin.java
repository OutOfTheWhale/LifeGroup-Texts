package com.lifegrouptext.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;
import androidx.core.content.ContextCompat;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

@CapacitorPlugin(
    name = "ContactsPlugin",
    permissions = {
        @Permission(strings = { Manifest.permission.READ_CONTACTS }, alias = "contacts")
    }
)
public class ContactsPlugin extends Plugin {

    @PluginMethod
    public void getContacts(PluginCall call) {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionForAlias("contacts", call, "contactsPermissionCallback");
            return;
        }
        fetchContacts(call);
    }

    private void fetchContacts(PluginCall call) {
        JSArray contacts = new JSArray();
        Cursor cursor = getContext().getContentResolver().query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            new String[]{
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            },
            null, null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String name = cursor.getString(0);
                String phone = cursor.getString(1);
                if (name != null && phone != null && !phone.trim().isEmpty()) {
                    JSObject contact = new JSObject();
                    contact.put("name", name.trim());
                    contact.put("phone", phone.replaceAll("[^0-9]", ""));
                    contacts.put(contact);
                }
            }
            cursor.close();
        }

        JSObject result = new JSObject();
        result.put("contacts", contacts);
        call.resolve(result);
    }

    @PermissionCallback
    private void contactsPermissionCallback(PluginCall call) {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            fetchContacts(call);
        } else {
            call.reject("Contacts permission denied");
        }
    }
}
