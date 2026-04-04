package com.pdfreader.app;

import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;

public class PdfViewerActivity extends AppCompatActivity {

    public static final String EXTRA_FILE_NAME = "extra_file_name";

    private RecyclerView recyclerView;
    private PdfPageAdapter pdfPageAdapter;
    private TextView tvPageInfo;
    private SeekBar seekBarPage;
    private View appBarLayout;
    private View bottomBar;
    private View loadingOverlay;

    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor fileDescriptor;
    private int totalPages = 0;
    private boolean barsVisible = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }

        recyclerView   = findViewById(R.id.recyclerView);
        tvPageInfo     = findViewById(R.id.tvPageInfo);
        seekBarPage    = findViewById(R.id.seekBarPage);
        appBarLayout   = findViewById(R.id.appBarLayout);
        bottomBar      = findViewById(R.id.bottomBar);
        loadingOverlay = findViewById(R.id.loadingOverlay);

        String fileName = getIntent().getStringExtra(EXTRA_FILE_NAME);
        if (fileName != null && getSupportActionBar() != null) {
            getSupportActionBar().setTitle(fileName);
        }

        seekBarPage.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (fromUser) recyclerView.smoothScrollToPosition(p);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrolled(RecyclerView rv, int dx, int dy) {
                LinearLayoutManager lm = (LinearLayoutManager) rv.getLayoutManager();
                if (lm != null) {
                    int pos = lm.findFirstVisibleItemPosition();
                    if (pos >= 0) {
                        tvPageInfo.setText((pos + 1) + " / " + totalPages);
                        seekBarPage.setProgress(pos);
                    }
                }
            }
        });

        loadPdf();
    }

    private void loadPdf() {
        Uri uri = getIntent().getData();
        if (uri == null) { finish(); return; }

        loadingOverlay.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                fileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
                if (fileDescriptor == null) throw new IOException("Tidak bisa membuka file");
                pdfRenderer = new PdfRenderer(fileDescriptor);
                totalPages = pdfRenderer.getPageCount();

                runOnUiThread(() -> {
                    loadingOverlay.setVisibility(View.GONE);
                    seekBarPage.setMax(Math.max(totalPages - 1, 0));
                    tvPageInfo.setText("1 / " + totalPages);

                    LinearLayoutManager lm = new LinearLayoutManager(this);
                    recyclerView.setLayoutManager(lm);

                    // Disable scroll when user is zooming inside a page
                    recyclerView.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
                        @Override
                        public boolean onInterceptTouchEvent(RecyclerView rv, android.view.MotionEvent e) {
                            // Allow RecyclerView to scroll only with 1 finger when not zoomed
                            return false;
                        }
                    });

                    pdfPageAdapter = new PdfPageAdapter(pdfRenderer, totalPages, this::toggleBars);
                    recyclerView.setAdapter(pdfPageAdapter);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    loadingOverlay.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }).start();
    }

    private void toggleBars() {
        if (barsVisible) {
            barsVisible = false;
            appBarLayout.setVisibility(View.GONE);
            bottomBar.setVisibility(View.GONE);
        } else {
            barsVisible = true;
            appBarLayout.setVisibility(View.VISIBLE);
            bottomBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.pdf_viewer_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_zoom_in && pdfPageAdapter != null) {
            pdfPageAdapter.zoomIn();
            return true;
        } else if (id == R.id.action_zoom_out && pdfPageAdapter != null) {
            pdfPageAdapter.zoomOut();
            return true;
        } else if (id == R.id.action_zoom_reset && pdfPageAdapter != null) {
            pdfPageAdapter.resetZoom();
            return true;
        } else if (id == R.id.action_page_first) {
            recyclerView.scrollToPosition(0);
            return true;
        } else if (id == R.id.action_page_last && totalPages > 0) {
            recyclerView.scrollToPosition(totalPages - 1);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (pdfRenderer != null) pdfRenderer.close();
            if (fileDescriptor != null) fileDescriptor.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
