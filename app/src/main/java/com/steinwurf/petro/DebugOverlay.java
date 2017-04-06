package com.steinwurf.petro;

import android.util.Log;

import java.util.ArrayList;

class DebugOverlay extends Thread
{
    private static String TAG = "DebugOverlay";

    static long UPDATE_FREQUENCY = 100;

    interface DebugOverlayLineConstructor
    {
        String constructLine();
    }

    private ArrayList<DebugOverlayLineConstructor> debugOverlayLineConstructors = new ArrayList<>();
    void addDebugOverlayLineConstructor(DebugOverlayLineConstructor constructor)
    {
        debugOverlayLineConstructors.add(constructor);
    }


    interface DebugInfoHandler
    {
        void handle(String debugInfo);
    }

    DebugInfoHandler mDebugInfoHandler;

    void setDebugInfoHandler(DebugInfoHandler debugInfoHandler)
    {
        mDebugInfoHandler = debugInfoHandler;
    }

    @Override
    public void run()
    {
        try
        {
            while (!isInterrupted())
            {
                if (debugOverlayLineConstructors.isEmpty())
                    return;
                StringBuilder debugInfoBuilder = new StringBuilder();
                for (DebugOverlayLineConstructor c: debugOverlayLineConstructors)
                {
                    debugInfoBuilder.append(c.constructLine());
                    debugInfoBuilder.append('\n');
                }
                debugInfoBuilder.deleteCharAt(debugInfoBuilder.length() -1);
                if (mDebugInfoHandler != null)
                    mDebugInfoHandler.handle(debugInfoBuilder.toString());

                Thread.sleep(UPDATE_FREQUENCY);
            }
        }
        catch (InterruptedException ignored)
        {
            Log.d(TAG, "interrupted!");
        }
    }
}
