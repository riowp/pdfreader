package com.pdfreader.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RecentFilesAdapter adapter;
    private List<RecentFile> recentFiles;
    private View layoutEmpty;
    private RecentFilesManager recentFilesManager;

    private final ActivityResultLauncher<String> pickPdfLauncher =
        registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) openPdf(uri);
        });

    private final ActivityResultLauncher<String> requestPermissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            pickPdfLauncher.launch("application/pdf");
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recentFilesManager = new RecentFilesManager(this);
        recentFiles = new ArrayList<>();

        recyclerView = findViewById(R.id.recyclerView);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        ExtendedFloatingActionButton fabOpen = findViewById(R.id.fabOpen);

        adapter = new RecentFilesAdapter(this, recentFiles,
            file -> openPdf(Uri.parse(file.getUri())),
            file -> {
                recentFilesManager.removeFile(file);
                loadRecentFiles();
            }
        );

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        fabOpen.setOnClickListener(v -> openFilePicker());

        // Handle open from file manager
        if (Intent.ACTION_VIEW.equals(getIntent().getAction()) && getIntent().getData() != null) {
            openPdf(getIntent().getData());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecentFiles();
    }

    private void loadRecentFiles() {
        recentFiles.clear();
        recentFiles.addAll(recentFilesManager.getRecentFiles());
        adapter.notifyDataSetChanged();

        if (recentFiles.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void openFilePicker() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            pickPdfLauncher.launch("application/pdf");
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    private void openPdf(Uri uri) {
        try {
            getContentResolver().takePersistableUriPermission(uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {}

        String fileName = UriUtils.getFileName(this, uri);
        recentFilesManager.addFile(new RecentFile(uri.toString(), fileName, System.currentTimeMillis()));

        Intent intent = new Intent(this, PdfViewerActivity.class);
        intent.setData(uri);
        intent.putExtra(PdfViewerActivity.EXTRA_FILE_NAME, fileName);
        startActivity(intent);
    }
}
