package uk.ac.tees.aad.A0264334.screenrecorder.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

import uk.ac.tees.aad.A0264334.screenrecorder.Constants;
import uk.ac.tees.aad.A0264334.screenrecorder.R;
import uk.ac.tees.aad.A0264334.screenrecorder.adapters.AdapterMyVideoFileList;


public class MyRecordingsFragment extends Fragment {
    RecyclerView myRecordingsRv;
    TextView videoNotFound;
    private ArrayList<File> fileArrayList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_my_recordings, container, false);
        myRecordingsRv = view.findViewById(R.id.my_recordings_rv);
        videoNotFound = view.findViewById(R.id.no_video_found);
        return view;
    }
    @Override
    public void onResume() {
        super.onResume();
        getAllFiles();
        if (fileArrayList.size() == 0) {
            videoNotFound.setVisibility(View.VISIBLE);
        } else {
            videoNotFound.setVisibility(View.GONE);
        }
    }


    private void getAllFiles() {
        fileArrayList = new ArrayList<>();
        File myrecoeding=new File(Constants.PathFileDirectory);

        if (!myrecoeding.exists()) {
            myrecoeding.mkdir();

        } else {
            Log.d("jjjjj", "Constance.FileDirectory : " + myrecoeding);
            File[] files = myrecoeding.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().endsWith(".mp4"))
                    {
                        fileArrayList.add(file);
                    }
                }
                for(int i=0;i<fileArrayList.size();i++){
                    if(fileArrayList.get(i).isDirectory()){
                        fileArrayList.remove(i);
                    }
                }
                AdapterMyVideoFileList adapterMyVideoFileList = new AdapterMyVideoFileList(requireContext(), fileArrayList);
                adapterMyVideoFileList.notifyDataSetChanged();
                myRecordingsRv.setAdapter(adapterMyVideoFileList);
            }
            else {
                Toast.makeText(requireContext(), "Data Not Found", Toast.LENGTH_LONG).show();
            }
        }
    }


}