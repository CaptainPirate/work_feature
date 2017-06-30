/*===================================================================================================*
 *  when  |      who     |    keyword           |        why         |         what                  *
 *===================================================================================================*
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*
*====================================================================================================*/

package com.wingtech.note.editor;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.wingtech.note.R;
import com.wingtech.note.spannableparser.SpanUtils;

public class AlarmAlertDialog extends Activity implements DialogInterface.OnClickListener,
        DialogInterface.OnDismissListener {
    private static final String ALARM_NOTIFICATION_TAG = "AlarmAlertActivity";
	private static final String TAG = ALARM_NOTIFICATION_TAG;
    private TelephonyManager mTelephonyManager;
    private MediaPlayer mPlayer;
    private CharSequence mContent;
    private AlertDialog mAlertDialog;
    private static final String ACTION_MUTE = "com.wingtech.note.editor.AlarmAlertActivity.MUTE";
    private static final String ACTION_VIEW = "com.wingtech.note.editor.AlarmAlertActivity.VIEW";
    private static final int CODE_MUTE = 2;
    private static final int CODE_PLAY = 1;
    private AudioManager mAudioManager = null;//bug211884,mengzhiming.wt,add,20160822

    private static final int[] MUTE_PERIOD = {
            300000, 600000, 1200000
    };
    private long mNoteId;

    private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        public void onCallStateChanged(int state, String incomingNumber) {
            if (isCallOn(state))
                stopAlertSound();
        }
    };
    private BroadcastReceiver mAlarmReceiver = new BroadcastReceiver() {
        public void onReceive(Context paramAnonymousContext, Intent paramAnonymousIntent) {
            muteMusic();
            showAlarmNotification(false);
        }
    };
    private Handler mHandler = new Handler() {
        public void handleMessage(Message message) {
            switch (message.what) {
                case CODE_PLAY:
                    playAlertSound();
                    sendMessageDelayed(obtainMessage(CODE_MUTE, message.arg1, 0),
                            MUTE_PERIOD[0]);
                    break;
                case CODE_MUTE:
                    stopAlertSound();
                    if (message.arg1 < MUTE_PERIOD.length)
                        sendMessageDelayed(obtainMessage(CODE_PLAY, message.arg1 + 1, 0),
                                MUTE_PERIOD[message.arg1]);
                    break;
                default:
                    break;
            }

        }
    };

    private boolean isCallOn(int state) {
        if (state == TelephonyManager.CALL_STATE_IDLE)
            return false;
        return true;
    }

    private void stopAlertSound() {
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        }
        //bug211884,mengzhiming.wt,add,20160822,start
        if(mAudioManager != null){
             mAudioManager.abandonAudioFocus(null);
             mAudioManager =null;
        }
        //bug211884,mengzhiming.wt,add,20160822,end
    }

    private void muteMusic() {
        mHandler.removeMessages(CODE_PLAY);
        mHandler.removeMessages(CODE_MUTE);
        stopAlertSound();
    }

    private void playAlertSound() {
        if (isCallOn(mTelephonyManager.getCallState()))
            Log.w(TAG, "Don't play alarm for call is on");
        else {
            Uri localUri = RingtoneManager.getActualDefaultRingtoneUri(this,
                    RingtoneManager.TYPE_ALARM);
            if (localUri == null) {
                Log.e(TAG, "url is null.");
            }
            else {
                if (mPlayer != null)
                    stopAlertSound();
                // bug211884,mengzhiming.wt,add,20160822,start
                if (mAudioManager == null) {
                    mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    mAudioManager.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                }
                // bug211884,mengzhiming.wt,add,20160822,end
                mPlayer = new MediaPlayer();
                mPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                try {
                    mPlayer.setDataSource(this, localUri);
                    mPlayer.prepare();
                    mPlayer.setLooping(true);
                    mPlayer.start();
                } catch (Exception localException) {
                    Log.e(TAG, "playAlarmSound failed.", localException);
                }
            }
        }
    }

    protected void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        Window localWindow = getWindow();
        localWindow.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        if (!isScreenOn())
            localWindow.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
        mTelephonyManager = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE));
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        IntentFilter localIntentFilter = new IntentFilter(ACTION_MUTE);
        registerReceiver(mAlarmReceiver, localIntentFilter);
        mPlayer = new MediaPlayer();
        initialize();
        showAlarm();
    }

    private void showAlarm() {
        showDialog();
        showAlarmNotification(true);
        muteMusic();
        playMusic();
    }

    private void playMusic() {
        mHandler.obtainMessage(CODE_PLAY, 0, 0).sendToTarget();
    }
	/**
	 * when b is true,add action mute.
	 * */
    private void showAlarmNotification(boolean b) {
        Notification.Builder localBuilder = new Notification.Builder(this);
        localBuilder.setContentTitle(getString(R.string.note_alarm_title));
        localBuilder.setSmallIcon(R.drawable.stat_notify_alarm);
        localBuilder.setContentText(getString(R.string.note_alarm_content));
        localBuilder.setAutoCancel(false);
        localBuilder.setOngoing(true);
        Intent localIntent = new Intent(getIntent());
        localIntent.setAction(ACTION_VIEW);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,localIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        localBuilder.setContentIntent(pendingIntent);
        localBuilder.addAction(R.drawable.stat_action_view,getString(R.string.notify_alarm_action_view), pendingIntent);
        if (b) {
		PendingIntent pendingIntent2 = PendingIntent.getBroadcast(this, 0,new Intent(ACTION_MUTE), PendingIntent.FLAG_UPDATE_CURRENT);
		localBuilder.addAction(R.drawable.stat_action_mute,getString(R.string.notify_alarm_action_mute), pendingIntent2);
        }
        Notification notify = localBuilder.getNotification();
        Log.d(TAG,"get Notification info"+notify.toString());
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(ALARM_NOTIFICATION_TAG,0, localBuilder.getNotification());
    }

    private void showDialog() {
        if (mAlertDialog == null) {
            //porting A1s enjoynotes, mengzhiming.wt,modified 20160405,start
            //AlertDialog.Builder localBuilder = new AlertDialog.Builder(this,com.android.internal.R.style.Theme_Enjoy_Light_Dialog_Alert);
            //AlertDialog.Builder localBuilder = new AlertDialog.Builder(this,com.android.internal.R.style.Theme_Dialog_Alert);
            AlertDialog.Builder localBuilder = new AlertDialog.Builder(this,android.R.style.Theme_Dialog);
            //porting A1s enjoynotes, mengzhiming.wt,modified 20160405,end
            localBuilder.setTitle(R.string.note_alarm_title);
            localBuilder.setPositiveButton(R.string.note_alert_ok, this);
            localBuilder.setCancelable(false);
            localBuilder.setNegativeButton(R.string.note_alert_enter, this);
            mAlertDialog = localBuilder.create();
            mAlertDialog.setOnDismissListener(this);
        }
        mAlertDialog.setMessage(mContent);
        mAlertDialog.show();
    }

    private void initialize() {
        Intent localIntent = getIntent();
        mNoteId = ContentUris.parseId(localIntent.getData());
        mContent = SpanUtils.normalizeSnippet(this,
                localIntent.getStringExtra("com.wingtech.note.snippet"));
        if (mContent.length() > 60) {
            SpannableStringBuilder localSpannableStringBuilder = new SpannableStringBuilder(
                    mContent.subSequence(0, 60));
            localSpannableStringBuilder.append(getResources().getString(
                    R.string.note_alert_info_more));
            mContent = localSpannableStringBuilder;
        }
    }

    private boolean isScreenOn() {
        return ((PowerManager) getSystemService(Context.POWER_SERVICE)).isScreenOn();
    }

    public void onDismiss(DialogInterface dialog) {
        muteMusic();
        cancelAlarmNotification();
        finish();
    }

    private void cancelAlarmNotification() {
         ((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancel(ALARM_NOTIFICATION_TAG, 0);
    }

    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_NEGATIVE:
                Intent localIntent = new Intent(this, NoteEditActivity.class);
                localIntent.setAction(Intent.ACTION_VIEW);
                localIntent.putExtra(Intent.EXTRA_UID, mNoteId);
                startActivity(localIntent);
                break;
            case DialogInterface.BUTTON_POSITIVE:
            default:
                finish();
                break;
        }

    }

    protected void onNewIntent(Intent paramIntent) {
        super.onNewIntent(paramIntent);
        setIntent(paramIntent);
        if (ACTION_VIEW.equals(paramIntent.getAction())) {
            initialize();
            showAlarm();
        }
    }

    protected void onDestroy() {
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        unregisterReceiver(mAlarmReceiver);
        super.onDestroy();
    }

}
