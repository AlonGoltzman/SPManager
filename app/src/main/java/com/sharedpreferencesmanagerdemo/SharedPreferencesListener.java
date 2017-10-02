package com.sharedpreferencesmanagerdemo;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.aongoltzcrank.sharedpreferencesmanager.annotations.SPListener;
import com.aongoltzcrank.sharedpreferencesmanager.annotations.SPUpdateTarget;

/**
 * Created by Alon on 10/2/2017.
 */

@SPListener
public class SharedPreferencesListener {

    private static final String TAG = SharedPreferencesListener.class.getSimpleName();

    @SPUpdateTarget
    public void onUpdate(Context context, String key, Object value) {
        Log.d(TAG, "onUpdate() called with: context = [" + context + "], key = [" + key + "], value = [" + value + "]");
        switch (key) {
            case "test1":
                Toast.makeText(context, value.toString(), Toast.LENGTH_SHORT).show();
                break;
            case "test2": {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://www.google.com"));
                context.startActivity(intent);
                break;
            }
            case "test3": {
                Intent intent1 = new Intent(Intent.ACTION_VIEW);
                intent1.setData(Uri.parse("https://www.google.com"));
                Intent chooser = Intent.createChooser(intent1, "Please select a web site.");
                context.startActivity(chooser);
                break;
            }
            default:
                Log.d(TAG, "onUpdate() called with: context = [" + context + "], key = [" + key + "], value = [" + value + "], this is not an update that want to deal with.");
                break;
        }
    }

    @SPUpdateTarget(keys = {"test1", "test4"})
    public void onUpdateSpecific(Context context, String key, Object value) {
        Log.d(TAG, "onUpdateSpecific() called with: context = [" + context + "], key = [" + key + "], value = [" + value + "]");
        Toast.makeText(context, value.toString(), Toast.LENGTH_SHORT).show();
    }

}
