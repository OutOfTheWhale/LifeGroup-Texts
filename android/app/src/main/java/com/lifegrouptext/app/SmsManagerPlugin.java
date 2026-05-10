package com.lifegrouptext.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import android.os.Build;
import androidx.core.content.ContextCompat;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

@CapacitorPlugin(
    name = "SmsManager",
    permissions = {
        @Permission(strings = { Manifest.permission.SEND_SMS }, alias = "sms")
    }
)
public class SmsManagerPlugin extends Plugin {

    private String sanitizeText(String text) {
        if (text == null) return null;
        return text
            // Replace curly apostrophes/quotes with straight ones
            .replace('\u2018', '\'')
            .replace('\u2019', '\'')
            .replace('\u201C', '"')
            .replace('\u201D', '"')
            // Replace em/en dashes with hyphens
            .replace('\u2013', '-')
            .replace('\u2014', '-')
            // Replace ellipsis with three dots
            .replace('\u2026', '.')
            // Normalize line endings
            .replace("\r\n", "\n")
            .replace('\r', '\n');
    }

    @PluginMethod
    public void send(PluginCall call) {
        String to = call.getString("to");
        String text = call.getString("text");

        if (to == null || text == null) {
            call.reject("Missing 'to' or 'text'");
            return;
        }

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionForAlias("sms", call, "smsPermissionCallback");
            return;
        }

        try {
            SmsManager smsManager;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                smsManager = getContext().getSystemService(SmsManager.class);
            } else {
                smsManager = SmsManager.getDefault();
            }
            String sanitized = sanitizeText(text);
            smsManager.sendTextMessage(to, null, sanitized, null, null);
            JSObject result = new JSObject();
            result.put("success", true);
            call.resolve(result);
        } catch (Exception e) {
            call.reject("SMS failed: " + e.getMessage());
        }
    }

    @PermissionCallback
    private void smsPermissionCallback(PluginCall call) {
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            send(call);
        } else {
            call.reject("SMS permission denied");
        }
    }
}
