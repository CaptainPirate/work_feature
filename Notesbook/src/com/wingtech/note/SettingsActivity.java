
/*===================================================================================================*	
 *  when  |      who     |    keyword           |        why         |         what                  *	
 *===================================================================================================*	
 *20160407|mengzhiming.wt|porting A1s enjoynotes| Port_EnjoyNotes | porting A1s enjoynotes to CMCC N2*	
*====================================================================================================*/

package com.wingtech.note;

import android.app.ActionBar;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.NinePatch;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.view.MenuItem;

public class SettingsActivity extends PreferenceActivity implements
		OnPreferenceChangeListener {
	private Preference bgPrefer = null;
	private int[] colorDrawables = { R.drawable.color_picker_1,
			R.drawable.color_picker_2, R.drawable.color_picker_3,
			R.drawable.color_picker_4, R.drawable.color_picker_5,
			R.drawable.color_picker_6 };

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);

		ActionBar actionBar = getActionBar();
		if (actionBar != null) {
			actionBar.setTitle(R.string.settings);
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
		bgPrefer = findPreference("background_settings");
		bgPrefer.setOnPreferenceChangeListener(this);
		int value = PreferenceUtils.getBgAppear(this);
		if (value == colorDrawables.length - 1)// The last one is random
			bgPrefer.setSummary(getResources().getStringArray(
					R.array.background_entries)[value]);
//+bug_270465, zhoupengfei.wt, MODIFY, 20140408
		//bgPrefer.setIcon(colorDrawables[value]);
		bgPrefer.setIcon(getCoveredDrawable(colorDrawables[value]));
//-+bug_270465, zhoupengfei.wt, MODIFY, 20140408		
		if (bgPrefer instanceof ListPreference) {
			ListPreference lp = (ListPreference) bgPrefer;
			CharSequence[] entries = lp.getEntries();
			for (int i = 0; i < colorDrawables.length - 1; i++) {
//+bug_270465, zhoupengfei.wt, MODIFY, 20140408				
				//ImageSpan span = new ImageSpan(this, colorDrawables[i]);
				ImageSpan span = new ImageSpan(getCoveredDrawable(colorDrawables[i]).getBitmap());
//+bug_270465, zhoupengfei.wt, MODIFY, 20140408
				SpannableStringBuilder localSpannableStringBuilder = new SpannableStringBuilder(
						entries[i]);
				localSpannableStringBuilder.setSpan(span, 0,
						localSpannableStringBuilder.length(),
						Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				entries[i] = localSpannableStringBuilder;
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO Auto-generated method stub
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			break;
		default:
			break;

		}
		return super.onOptionsItemSelected(item);
	}

	public boolean onPreferenceChange(Preference preference, Object newValue) {
		// TODO Auto-generated method stub
		String key = preference.getKey();
		if (key.equals("background_settings")) {
			PreferenceUtils.setBgAppear(this,
					Integer.valueOf((String) newValue));
			int index = Integer.valueOf((String) newValue);
			if (index == colorDrawables.length - 1) {// The last one is random
				bgPrefer.setSummary(getResources().getStringArray(
						R.array.background_entries)[index]);
			} else {
				bgPrefer.setSummary("");
			}
//+bug_270465, zhoupengfei.wt, MODIFY, 20140408
			
			//bgPrefer.setIcon(colorDrawables[index]);
			bgPrefer.setIcon(getCoveredDrawable(colorDrawables[index]));
//-bug_270465, zhoupengfei.wt, MODIFY, 20140408			
		}
		return true;
	}
//+bug_270465, zhoupengfei.wt, ADD, 20140408
	private BitmapDrawable getCoveredDrawable(int resId) {
		BitmapFactory.Options opts = new Options();
		BitmapFactory.decodeResource(getResources(), resId, opts);
		Bitmap bmp = Bitmap.createBitmap(opts.outWidth, opts.outHeight,
				Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bmp);
		Drawable drawable = getResources().getDrawable(resId);
		drawable.setBounds(0, 0, opts.outWidth, opts.outHeight);
		drawable.draw(canvas);
	    RectF dst = new RectF(0, 0, opts.outWidth, opts.outHeight);
	    Bitmap cover = BitmapFactory.decodeResource(getResources(), R.drawable.bg_select_icon_cover);
	    NinePatch ninePath  = new NinePatch(cover, cover.getNinePatchChunk(), null);
	    ninePath.draw(canvas, dst);
	    cover.recycle();
		return new BitmapDrawable(bmp);
	}
//-bug_270465, zhoupengfei.wt, ADD, 20140408
	
}
