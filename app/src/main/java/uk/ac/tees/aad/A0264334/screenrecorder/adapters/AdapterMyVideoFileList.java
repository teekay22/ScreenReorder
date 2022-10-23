package uk.ac.tees.aad.A0264334.screenrecorder.adapters;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;

import uk.ac.tees.aad.A0264334.screenrecorder.R;

public class AdapterMyVideoFileList extends RecyclerView.Adapter<AdapterMyVideoFileList.ViewHolder> {

    private Context context;
    private ArrayList<File> fileArrayList;
    File fileItem;
    public AdapterMyVideoFileList(Context context, ArrayList<File> files) {
        this.context = context;
        this.fileArrayList = files;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_myvideofilelist, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        fileItem = fileArrayList.get(position);
        final String urlpath = fileItem.getAbsolutePath();
        final String filename = fileItem.getName();
        long length = fileItem.length();
        holder.tv_filesize.setText(size(length));
        holder.tv_fileName.setText(filename);
        Glide.with(context)
                .load(fileItem.getPath())
                .into(holder.iv_fileimage);
        holder.iv_popupmenu.setOnClickListener(view -> {
            final PopupMenu popupMenu = new PopupMenu(context, holder.iv_popupmenu);
            popupMenu.getMenuInflater().inflate(R.menu.popup_menu,popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.item_delete:
                            deleteFile(position);
                            return true;

                        case R.id.item_share:
                            shareVideo(context,urlpath);
                            return true;

                    }
                    popupMenu.dismiss();
                    return true;
                }
            });
            popupMenu.show();
        });
        holder.itemView.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.parse(urlpath);
            intent.setDataAndType(uri, "video/*");
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return fileArrayList == null ? 0 : fileArrayList.size();
    }

    public static String size(long size) {
        if (size <= 0)
            return "0";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
    public static void shareVideo(Context context, String filePath) {
        Uri mainUri = Uri.parse(filePath);
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("video/mp4");
        sharingIntent.putExtra(Intent.EXTRA_STREAM, mainUri);
        sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            context.startActivity(Intent.createChooser(sharingIntent, "Share Video using"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "Application not found to open this file", Toast.LENGTH_LONG).show();
        }
    }
    private void deleteFile(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Confirm Delete ?");
        builder.setMessage("Are you sure you want delete this !");
        builder.setIcon(R.drawable.ic_delete);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                fileItem.delete();
                Log.d("aaaaaaa",""+position);
                fileArrayList.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, fileArrayList.size());
//                if (fileArrayList.size() == 0) {
//                    MyRecordingsFragment.videoNotFound.setVisibility(View.VISIBLE);
//                }
            }
        });
        builder.setNegativeButton("No", (dialogInterface, i) -> {


        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView iv_fileimage,iv_popupmenu;
        TextView tv_fileName,tv_filesize,tv_fileduration;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tv_fileName = itemView.findViewById(R.id.tv_filetittle);
            tv_filesize = itemView.findViewById(R.id.tv_filesize);
            tv_fileduration = itemView.findViewById(R.id.tv_fileduration);
            iv_fileimage = itemView.findViewById(R.id.iv_fileimage);
            iv_popupmenu = itemView.findViewById(R.id.iv_popupmenu);

        }
    }
}
