/*===================================================================================================*
 *  when  |      who     |    keyword           |        why         |         what                  *
 *===================================================================================================*
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*
*====================================================================================================*
*20160531|mengzhiming.wt|   customer req       | customer req    | customer req                      *
*===================================================================================================*
*20160602|mengzhiming.wt|  bug183401      | bug183401   | bug183401                      *
*===================================================================================================*/
package com.wingtech.note.editor;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import com.wingtech.note.R;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.view.WindowManager;
import android.text.TextWatcher;
import android.text.TextUtils;
import android.app.KeyguardManager;
import android.content.Context;
//bug197153,mengzhiming.wt,add,20160712,start
import android.content.res.Resources;
import android.content.res.Configuration;
//bug197153,mengzhiming.wt,add,20160712,end
//bug200451,mengzhiming.wt,add,20160720,start
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
//bug200451,mengzhiming.wt,add,20160720,end

public class NoteEditActivity extends Activity {
    private RichEditorImpl mEditImpl;
    //bug173254,mengzhiming.wt, add,20160601 ,start
    private static final String LOG_TAG = "NoteEditActivity";
    private static final String LAUNCH_SOURCE = "magazinelockscreen";
    private static final String EXTRA_LAUNCH_SOURCE = "com.android.systemui.launch_source";
    private static String mMagazineLockScreenLaunchSource = "";
    private KeyguardManager     mKeyguardManager;
    //bug173254,mengzhiming.wt, add,20160601 ,end
    //bug200451,mengzhiming.wt, add,20160720
    private HomeWatcherReceiver mHomeKeyReceiver = null;

    public void onActivityResult(int requestCode, int resultCode, Intent paramIntent) {
        mEditImpl.onActivityResult(requestCode, resultCode, paramIntent);
    }

    public void onBackPressed() {
        if (!mEditImpl.onBackPressed())
            super.onBackPressed();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // TODO Auto-generated method stub
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_DOWN)
            mEditImpl.setBackKeyPressed(true);

        //bug195059,mengzhiming.wt,add 20160707 start
        if(event.getKeyCode() == KeyEvent.KEYCODE_BACK){
            mEditImpl.dismissPopupWindow();
        }
        //bug195059,mengzhiming.wt,add 20160707 end
        return super.dispatchKeyEvent(event);
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.editor);
        //bug173254,mengzhiming.wt, modified,20160601 ,start
        resetFlagIfSupportMagazineLockScreen();
        //bug173254,mengzhiming.wt, modified,20160601 ,end

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(R.layout.editor_header);
        /*
        View view = LayoutInflater.from(this).inflate(R.layout.editor_header, null);
        actionBar.setCustomView(view);
        */
        mEditImpl = new RichEditorImpl(this);
        if (!mEditImpl.onCreate(getIntent(), savedInstanceState))
            finish();

        //getWindow().clearFlags(LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);//bug183401,mengzhiming.wt, modified,2016060
        getWindow().setStatusBarColor(getResources().getColor(R.color.title_Bg));//bug183401,mengzhiming.wt, modified,2016060

        // mModeCallBack = new SketchModeCallBack();
       /*porting A1s enjoynotes, mengzhiming.wt,modified 20160405
	   boolean b = Settings.Global.getInt(getContentResolver(),
                Settings.Global.VISITOR_MODE_ON, 0) == 1 ? true : false;
        if(b){
            Toast.makeText(NoteEditActivity.this, R.string.private_mode_hint, Toast.LENGTH_LONG)
            .show();
            finish();
            return;
        }*/
    }

    protected void onNewIntent(Intent paramIntent) {
        super.onNewIntent(paramIntent);
        mEditImpl.onNewIntent(paramIntent);
    }

    public boolean onOptionsItemSelected(MenuItem paramMenuItem) {
        return mEditImpl.onOptionsItemSelected(paramMenuItem);
    }

    protected void onPause() {
        super.onPause();
        mEditImpl.onPause();
        unregisterHomeKeyReceiver(this);//bug 210726 ,mengzhiming.wt,add,20160819
    }

    public boolean onPrepareOptionsMenu(Menu paramMenu) {
        return mEditImpl.onPrepareOptionsMenu(paramMenu);
    }

    protected void onResume() {
        super.onResume();
        mEditImpl.onResume();
        //bug 210726 ,mengzhiming.wt,add,20160819,start
        if (isKeyguardLocked()) {
            registerHomeKeyReceiver(this);
        }
        //bug 210726 ,mengzhiming.wt,add,20160819,end
    }

    protected void onSaveInstanceState(Bundle paramBundle) {
        super.onSaveInstanceState(paramBundle);
        mEditImpl.onSaveInstanceState(paramBundle);
    }

    protected void onStop() {
        super.onStop();
        mEditImpl.onStop();
    }

    //bug173254,mengzhiming.wt, add,20160601 ,start
    private void resetFlagIfSupportMagazineLockScreen() {
        Intent mIntent = getIntent();
        if (mIntent != null) {
            mMagazineLockScreenLaunchSource = mIntent.getStringExtra(EXTRA_LAUNCH_SOURCE);
            Log.d(LOG_TAG, "mMagazineLockScreenLaunchSource=" + mMagazineLockScreenLaunchSource);
            if (!TextUtils.isEmpty(mMagazineLockScreenLaunchSource)
                    && mMagazineLockScreenLaunchSource.equals(LAUNCH_SOURCE)) {
                final Window win = getWindow();
                win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
            }
        }
    }


    public boolean isKeyguardLocked() {
       if (mKeyguardManager == null) {
           mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
       }

       boolean locked = (mKeyguardManager != null)
               && mKeyguardManager.inKeyguardRestrictedInputMode();

       Log.d("NoteEditActivity", "locked=" + locked + ", mKeyguardManager="
                   + mKeyguardManager);

       return locked;
   }
   //bug173254,mengzhiming.wt, add,20160601 ,end


    //bug197153,mengzhiming.wt,add,20160712,start
    @Override
    public Resources getResources() {
        Resources res = super.getResources();
        Configuration config = new Configuration();
        config.setToDefaults();
        res.updateConfiguration(config,res.getDisplayMetrics());
        return res;
    }
    //bug197153,mengzhiming.wt,add,20160712,end

    //bug200451,mengzhiming.wt,add,20160720,start
    private void registerHomeKeyReceiver(Context context) {
        mHomeKeyReceiver = new HomeWatcherReceiver();
        final IntentFilter homeFilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        context.registerReceiver(mHomeKeyReceiver, homeFilter);
    }


    private void unregisterHomeKeyReceiver(Context context) {
        if (null != mHomeKeyReceiver && context != null) {
            context.unregisterReceiver(mHomeKeyReceiver);
            mHomeKeyReceiver = null;//bug 210726 ,mengzhiming.wt,add,20160819
        }
    }

    public class HomeWatcherReceiver extends BroadcastReceiver {
        private static final String SYSTEM_DIALOG_REASON_KEY = "reason";
        private static final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("NoteEditActivity", "HomeWatcherReceiver onReceive action=" + action);
            if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                String reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY);
                if (SYSTEM_DIALOG_REASON_HOME_KEY.equals(reason)) {
                    if (isKeyguardLocked()) {
                        onBackPressed();
                    }
                    //unregisterHomeKeyReceiver(context);//bug 210726 ,mengzhiming.wt,del,20160819
                }
            }
        }

    }
    //bug200451,mengzhiming.wt,add,20160720,end

}

