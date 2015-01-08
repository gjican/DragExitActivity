package com.gjican.snapshotframework;

import java.io.ByteArrayOutputStream;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class SnapShotActivity extends Activity {
	public static int QUALITY = 30;

	private int mWidth;
	private int mHeight;
	public static final String ACTIVITY_SNAP = "activity.snap";
	private Bitmap mPreBitmap;
	private ImageView mPreImageView;
	private View mContentView;
	private byte[] mPreBitmapArray;
	private float mDownX;
	private float mDownY;
	private boolean mIsSupportSnap = true;
	private float mShadowViewRatio = 0.2f;
	private float mUpViewRatio = 0.3f;
	private boolean isSnaping = false;
	private boolean isAutoSnaping = false;

	public static void startSnapActivity(Activity activity, Intent intent) {
		Bitmap bitmap = SnapUtil.getSnapOfActivity(activity);
		if (null != bitmap) {
			ByteArrayOutputStream outStream = new ByteArrayOutputStream();
			bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, outStream);
			intent.putExtra(ACTIVITY_SNAP, outStream.toByteArray());
		}
		activity.startActivity(intent);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		if (mIsSupportSnap) {
			parserIntent();
			addPreView();
		}
	}

	@Override
	public void finish() {
		super.finish();
		overridePendingTransition(0, 0);
	}

	public void setIsSupportSnap(boolean isSupportSnap) {
		mIsSupportSnap = isSupportSnap;
	}

	public void parserIntent() {
		if (null != getIntent() && getIntent().hasExtra(ACTIVITY_SNAP)) {
			mPreBitmapArray = getIntent().getByteArrayExtra(ACTIVITY_SNAP);
			if (null != mPreBitmapArray && mPreBitmapArray.length > 0) {
				mPreBitmap = BitmapFactory.decodeByteArray(mPreBitmapArray, 0,
						mPreBitmapArray.length);
			}
		}
	}

	public void addPreView() {
		if (null == mPreBitmap) {
			return;
		}
		mWidth = mPreBitmap.getWidth();
		mHeight = mPreBitmap.getHeight();

		mPreImageView = new ImageView(this);
		mPreImageView.setImageBitmap(mPreBitmap);
		ViewGroup topView = (ViewGroup) getWindow().getDecorView();
		FrameLayout.LayoutParams param = new FrameLayout.LayoutParams(
				FrameLayout.LayoutParams.MATCH_PARENT,
				FrameLayout.LayoutParams.MATCH_PARENT);
		topView.addView(mPreImageView, 0, param);
		mContentView = topView.getChildAt(1);
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {

		if (!mIsSupportSnap) {
			return super.dispatchTouchEvent(ev);
		}

		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN: {
			initDown(ev);
			break;
		}
		case MotionEvent.ACTION_MOVE: {
			if (isInSnapRect() && !isAutoSnaping) {
				float xDistance = ev.getX() - mDownX;
				float yDistance = ev.getY() - mDownY;
				if (Math.abs(xDistance) > Math.abs(yDistance) && xDistance > 0) {
					handMove((int) xDistance);
					isSnaping = true;
					return true;
				}
				break;
			}
		}
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL: {
			if (isSnaping) {
				handleUpAction(ev);
				isSnaping = false;
				return true;
			}
			break;
		}
		default:
			break;
		}
		return super.dispatchTouchEvent(ev);
	}

	private void initDown(MotionEvent event) {
		mDownX = event.getX();
		mDownY = event.getY();
	}

	private boolean isInSnapRect() {
		if (mDownX < mWidth * mShadowViewRatio) {
			return true;
		}
		return false;
	}

	private void handMove(int distance) {
		if (null != mContentView && distance >= 0) {
			offsetView(mContentView, distance, mWidth, mHeight);
			offsetView(mPreImageView, (distance - mWidth) * mShadowViewRatio,
					mWidth, mHeight);
		}
	}

	private void offsetView(View view, float offset, float width, int height) {
		if (view == null) {
			return;
		}
		if (Build.VERSION.SDK_INT >= 11) {
			view.setX(offset);
			return;
		}
		view.layout((int) offset, 0, (int) (offset + width), height);
	}

	private void handleUpAction(MotionEvent ev) {
		if (ev.getX() < 0) {
			return;
		}
		mContentView.clearAnimation();
		mPreImageView.clearAnimation();
		float xDistance = ev.getX() - mDownX;
		float moveDistance = mWidth - xDistance;
		if (xDistance < mUpViewRatio * mWidth && xDistance > 0) {
			moveDistance = -xDistance;
		}
		final boolean moveLeft = moveDistance < 0 ? true : false;
		CustonTranslateAnimation animation = new CustonTranslateAnimation(
				mContentView.getX(), mContentView.getX() + moveDistance);
		animation.setInterpolator(new LinearInterpolator());
		animation.setFillAfter(true);
		animation.setFillEnabled(true);
		animation.setDuration(200L);
		mContentView.setAnimation(animation);
		animation.start();
		animation.setAnimationListener(new AnimationListener() {

			@Override
			public void onAnimationStart(Animation arg0) {
				isAutoSnaping = true;
			}

			@Override
			public void onAnimationRepeat(Animation arg0) {

			}

			@Override
			public void onAnimationEnd(Animation arg0) {
				if (moveLeft) {
					offsetView(mContentView, 0, mWidth, mHeight);
					offsetView(mPreImageView, -mShadowViewRatio * mWidth,
							mWidth, mHeight);
				} else {
					finish();
				}
				isAutoSnaping = false;
			}
		});
	}

	class CustonTranslateAnimation extends Animation {
		private float mFromXDelta;
		private float mToXDelta;

		public CustonTranslateAnimation(float fromXDelta, float toXDelta) {
			mFromXDelta = fromXDelta;
			mToXDelta = toXDelta;
		}

		@Override
		protected void applyTransformation(float interpolatedTime,
				Transformation t) {
			float dx = mFromXDelta;
			if (mFromXDelta != mToXDelta) {
				dx = mFromXDelta
						+ ((mToXDelta - mFromXDelta) * interpolatedTime);
			}
			handMove((int) dx);
		}
	}
}
