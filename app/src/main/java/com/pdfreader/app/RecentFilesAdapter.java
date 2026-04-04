package com.pdfreader.app;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecentFilesAdapter extends RecyclerView.Adapter<RecentFilesAdapter.ViewHolder> {

    public interface OnFileClickListener {
        void onFileClick(RecentFile file);
    }

    public interface OnFileDeleteListener {
        void onFileDelete(RecentFile file);
    }

    private final Context context;
    private final List<RecentFile> files;
    private final OnFileClickListener clickListener;
    private final OnFileDeleteListener deleteListener;

    public RecentFilesAdapter(Context context, List<RecentFile> files,
            OnFileClickListener clickListener, OnFileDeleteListener deleteListener) {
        this.context = context;
        this.files = files;
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recent_file, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecentFile file = files.get(position);

        holder.tvFileName.setText(file.getFileName());

        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
            file.getLastOpened(),
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        );
        holder.tvLastOpened.setText(timeAgo);

        holder.itemView.setOnClickListener(v -> clickListener.onFileClick(file));
        holder.btnDelete.setOnClickListener(v -> deleteListener.onFileDelete(file));
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFileName;
        TextView tvLastOpened;
        ImageButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvLastOpened = itemView.findViewById(R.id.tvLastOpened);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
