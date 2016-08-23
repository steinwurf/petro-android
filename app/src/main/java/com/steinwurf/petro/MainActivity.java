package com.steinwurf.petro;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "MainActivity";
    private static final int ACTIVITY_CHOOSE_FILE = 1;
    private static final String ACTIVITY_NAME = "ACTIVITY_NAME";
    public static final String FILEPATH = "FILEPATH";

    private Class<?> mTargetActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        verifyStoragePermissions(this);

        findViewById(R.id.play_extractor_video_btn).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                openFileSelector(VideoExtractorActivity.class);
            }
        });
        findViewById(R.id.play_video_btn).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                openFileSelector(VideoActivity.class);
            }
        });
        findViewById(R.id.play_extractor_audio_btn).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                openFileSelector(AudioExtractorActivity.class);
            }
        });
        findViewById(R.id.play_audio_btn).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                openFileSelector(AudioActivity.class);
            }
        });
        findViewById(R.id.play_both_btn).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                openFileSelector(BothActivity.class);
            }
        });
    }

    protected void openFileSelector(Class<?> nextActivity)
    {
        mTargetActivity = nextActivity;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        startActivityForResult(intent, ACTIVITY_CHOOSE_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == ACTIVITY_CHOOSE_FILE && resultCode == RESULT_OK)
        {
            try
            {
                Uri uri = data.getData();
                String filePath = Utils.getFilePathFromURI(this, uri);
                Log.d(TAG, "File path: " + filePath);

                Intent intent = new Intent(MainActivity.this, mTargetActivity);
                intent.putExtra(FILEPATH, filePath);
                startActivity(intent);
            }
            catch (Throwable th)
            {
                Log.e(TAG, "Failed to get file path from URI: ");
                th.printStackTrace();
            }
        }
    }

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
        Manifest.permission.READ_EXTERNAL_STORAGE
    };

    /**
     * Checks if the app has permission to write to device storage
     * <p/>
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity)
    {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity,
            Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED)
        {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                activity,
                PERMISSIONS_STORAGE,
                REQUEST_EXTERNAL_STORAGE
            );
        }
    }
}
