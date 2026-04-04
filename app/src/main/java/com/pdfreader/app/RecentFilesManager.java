package com.pdfreader.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class RecentFilesManager {

    private static final String PREF_NAME = "recent_files";
    private static final String KEY_FILES = "files";
    private static final int MAX_FILES = 20;

    private final SharedPreferences prefs;

    public RecentFilesManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public List<RecentFile> getRecentFiles() {
        List<RecentFile> files = new ArrayList<>();
        String json = prefs.getString(KEY_FILES, "[]");
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                files.add(new RecentFile(
                    obj.getString("uri"),
                    obj.getString("fileName"),
                    obj.getLong("lastOpened")
                ));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return files;
    }

    public void addFile(RecentFile newFile) {
        List<RecentFile> files = getRecentFiles();

        // Remove if already exists
        files.removeIf(f -> f.getUri().equals(newFile.getUri()));

        // Add to top
        files.add(0, newFile);

        // Trim to max
        if (files.size() > MAX_FILES) {
            files = files.subList(0, MAX_FILES);
        }

        saveFiles(files);
    }

    public void removeFile(RecentFile fileToRemove) {
        List<RecentFile> files = getRecentFiles();
        files.removeIf(f -> f.getUri().equals(fileToRemove.getUri()));
        saveFiles(files);
    }

    private void saveFiles(List<RecentFile> files) {
        JSONArray array = new JSONArray();
        for (RecentFile file : files) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("uri", file.getUri());
                obj.put("fileName", file.getFileName());
                obj.put("lastOpened", file.getLastOpened());
                array.put(obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        prefs.edit().putString(KEY_FILES, array.toString()).apply();
    }
}
