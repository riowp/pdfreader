package com.pdfreader.app;

import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class PdfPageAdapter extends RecyclerView.Adapter<PdfPageAdapter.PageViewHolder> {

    public interface OnTapListener { void onTap(); }

    private final PdfRenderer pdfRenderer;
    private final int pageCount;
    private final OnTapListener tapListener;

    public PdfPageAdapter(PdfRenderer renderer, int count, OnTapListener listener) {
        this.pdfRenderer = renderer;
        this.pageCount = count;
        this.tapListener = listener;
    }

    // Called from toolbar zoom buttons - delegates to visible views
    private ZoomableImageView lastBoundView;

    public void zoomIn() {
        if (lastBoundView != null) lastBoundView.zoomIn();
    }

    public void zoomOut() {
        if (lastBoundView != null) lastBoundView.zoomOut();
    }

    public void resetZoom() {
        if (lastBoundView != null) lastBoundView.resetZoom();
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_pdf_page, parent, false);
        return new PageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        lastBoundView = holder.imageView;
        holder.bind(pdfRenderer, position, tapListener);
    }

    @Override
    public int getItemCount() { return pageCount; }

    static class PageViewHolder extends RecyclerView.ViewHolder {
        final ZoomableImageView imageView;
        final ProgressBar progressBar;

        PageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView   = itemView.findViewById(R.id.ivPage);
            progressBar = itemView.findViewById(R.id.progressBar);
        }

        void bind(PdfRenderer renderer, int pageIndex, OnTapListener tapListener) {
            imageView.setImageBitmap(null);
            progressBar.setVisibility(View.VISIBLE);

            imageView.setOnTapListener(() -> {
                if (tapListener != null) tapListener.onTap();
            });

            new Thread(() -> {
                Bitmap bitmap;
                synchronized (renderer) {
                    try (PdfRenderer.Page page = renderer.openPage(pageIndex)) {
                        // Render at 2x density for sharpness
                        int width  = page.getWidth()  * 2;
                        int height = page.getHeight() * 2;
                        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                        bitmap.eraseColor(android.graphics.Color.WHITE);
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    }
                }
                Bitmap finalBitmap = bitmap;
                imageView.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    imageView.setImageBitmap(finalBitmap);
                });
            }).start();
        }
    }
}
