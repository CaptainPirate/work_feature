/*===================================================================================================*	
 *  when  |      who     |    keyword           |        why         |         what                  *	
 *===================================================================================================*	
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*	
*====================================================================================================*/

package com.wingtech.note.loader;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class LoadingWorker {
    private static final String TAG = LoadingWorker.class.getSimpleName();
    private Handler mCallbackHandler;
    private Executor mExecutor;
    private LinkedBlockingQueue<Job> mJobs = new LinkedBlockingQueue<Job>();
    private String mName;
    private boolean mStop;
    private Thread mWorkerThread;

    public LoadingWorker(Context context, String name) {
        mCallbackHandler = new Handler(context.getMainLooper());
        mName = name;
        mStop = true;
    }

    public void addJob(Job job) {
        if (!mStop)
            mJobs.add(job);
        Log.w("OSBORN", "add job");

        return;
    }

    public void pause() {
        if (!mStop)
            mExecutor.pause();
    }

    public void removeJob(Job job) {
        if (!mStop)
            mJobs.remove(job);
    }

    public void resume() {
        if (!mStop)
            mExecutor.resume();
    }

    public void start() {
        if (mStop) {
            mExecutor = new Executor(mJobs);
            mWorkerThread = new Thread(mExecutor, mName);
            Log.w("OSBORN", "start");

            mWorkerThread.start();
            mStop = false;
        }
    }

    public void stop(){
        if (!mStop){
            mStop = true;
            mExecutor.resume();
            mExecutor.stop();
            mExecutor = null;
            mWorkerThread = null;
            mJobs.clear();
        }
    }

    private class Executor implements Runnable {
        private boolean nExit;
        private LinkedBlockingQueue<LoadingWorker.Job> nJobs;
        private boolean nPause;

        public Executor(LinkedBlockingQueue<Job> jobs) {
            nJobs = jobs;
        }

        private boolean validState() {
            if (nExit)
                return false;
            synchronized (this) {
                while (nPause) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        Log.w(LoadingWorker.TAG, "Exception occurs when run executor.",
                                e);
                    }
                }
            }
            return true;
        }

        public void pause() {
            nPause = true;
        }

        public void resume() {
            nPause = false;
            notifyAll();
        }

        public void run() {
            while (true) {
                Log.w("OSBORN", "running");

                try {
                    final LoadingWorker.Job job = nJobs.poll(5, TimeUnit.SECONDS);// Èç¿Õ£¬µÈ´ý5Sºó·µ»ØNULL
                    if (validState()) {
                        if (job == null)
                            continue;
                        job.run();
                        Log.w("OSBORN", "DO running jobs");
                        mCallbackHandler.post(new Runnable() {
                            public void run() {
                                job.callback();
                            }
                        });
                    }
                } catch (InterruptedException interruptedException) {
                    Log.w(LoadingWorker.TAG, "Exception occurs when run executor.",
                            interruptedException);
                }
            }
        }

        public void stop() {
            nExit = true;
        }
    }

    public static abstract interface Job extends Runnable
    {
        public abstract void callback();
    }
}

