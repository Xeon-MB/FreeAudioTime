package com.example.freeaudiotime;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AchievementStorage {

    private static final String PREFS = "achievements";

    public static JSONObject load(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        String json = prefs.getString("unlocked", "{}");

        try {
            return new JSONObject(json);
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    public static void save(Context context, JSONObject achievements) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        prefs.edit()
                .putString("unlocked", achievements.toString())
                .apply();
    }

    public static boolean isUnlocked(Context context, String id) {
        return load(context).has(id);
    }

    public static void unlock(Context context, String id) {
        JSONObject achievements = load(context);

        if (achievements.has(id)) {
            return;
        }

        try {
            achievements.put(id, System.currentTimeMillis());
            save(context, achievements);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}