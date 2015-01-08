package com.gjican.snapshotframework;

import android.app.Activity;
import android.app.ActivityManager;
import android.graphics.Bitmap;
import android.view.View;

public class SnapUtil {

	/**
	 * Get the snap of activity
	 * 
	 * @param paramActivity
	 * @return
	 */
	public static Bitmap getSnapOfActivity(Activity paramActivity) {

		if (null == paramActivity) {
			return null;
		}

		ActivityManager localActivityManager = (ActivityManager) paramActivity
				.getSystemService("activity");
		ActivityManager.MemoryInfo localMemoryInfo = new ActivityManager.MemoryInfo();
		localActivityManager.getMemoryInfo(localMemoryInfo);
		if (localMemoryInfo.lowMemory) {
			return null;
		}
		View decorView = paramActivity.getWindow().getDecorView();
		Bitmap localBitmap = null;
		try {
			decorView.setDrawingCacheEnabled(true);
			Bitmap bitmap = decorView.getDrawingCache();
			localBitmap = bitmap.copy(bitmap.getConfig(), false);
			decorView.destroyDrawingCache();
		} catch (OutOfMemoryError e) {
			System.gc();
		} catch (Exception e) {
			// TODO
		}
		return localBitmap;
	}
}
