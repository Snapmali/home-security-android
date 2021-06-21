package com.snapkirin.homesecurity.ui.picview;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

import com.bumptech.glide.Glide;
import com.snapkirin.homesecurity.R;

import static com.snapkirin.homesecurity.HomeSecurity.IMAGE_URL;
import static com.snapkirin.homesecurity.HomeSecurity.RECT;
import static com.snapkirin.homesecurity.HomeSecurity.SCALE_TYPE;


public class PicViewActivity extends Activity {

    private static final long ANIM_TIME = 200;

    private RectF mThumbMaskRect;
    private Matrix mThumbImageMatrix;

    private ObjectAnimator mBackgroundAnimator;

    private View mBackground;
    private PinchImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //获取参数
        final Rect rect = getIntent().getParcelableExtra(RECT);
        final ImageView.ScaleType scaleType = (ImageView.ScaleType) getIntent().getSerializableExtra(SCALE_TYPE);
        String imageUrl = getIntent().getStringExtra(IMAGE_URL);
        int thumbWidth = rect.bottom - rect.top;
        int thumbHeight = rect.right - rect.left;

        //view初始化
        setContentView(R.layout.activity_pic_view);
        mImageView = findViewById(R.id.pic);
        mBackground = findViewById(R.id.background);

        CircularProgressDrawable progressDrawable = new CircularProgressDrawable(this);
        progressDrawable.setStyle(CircularProgressDrawable.LARGE);
        progressDrawable.setColorSchemeColors(ContextCompat.getColor(this, R.color.design_default_color_primary));
        progressDrawable.start();

        Glide.with(PicViewActivity.this)
                .load(imageUrl)
                .placeholder(progressDrawable)
                .error(R.drawable.ic_baseline_error_24_red)
                .into(mImageView);

        mImageView.post(() -> {
            mImageView.setAlpha(1f);

            //背景动画
            mBackgroundAnimator = ObjectAnimator.ofFloat(mBackground, "alpha", 0f, 1f);
            mBackgroundAnimator.setDuration(ANIM_TIME);
            mBackgroundAnimator.start();

            //status bar高度修正
            Rect tempRect = new Rect();
            mImageView.getGlobalVisibleRect(tempRect);
            rect.top = rect.top - tempRect.top;
            rect.bottom = rect.bottom - tempRect.top;

            //mask动画
            mThumbMaskRect = new RectF(rect);
            RectF bigMaskRect = new RectF(0, 0, mImageView.getWidth(), mImageView.getHeight());
            mImageView.zoomMaskTo(mThumbMaskRect, 0);
            mImageView.zoomMaskTo(bigMaskRect, ANIM_TIME);


            //图片放大动画
            RectF thumbImageMatrixRect = new RectF();
            PinchImageView.MathUtils.calculateScaledRectInContainer(new RectF(rect), thumbWidth, thumbHeight, scaleType, thumbImageMatrixRect);
            RectF bigImageMatrixRect = new RectF();
            PinchImageView.MathUtils.calculateScaledRectInContainer(new RectF(0, 0, mImageView.getWidth(), mImageView.getHeight()), thumbWidth, thumbHeight, ImageView.ScaleType.FIT_CENTER, bigImageMatrixRect);
            mThumbImageMatrix = new Matrix();
            PinchImageView.MathUtils.calculateRectTranslateMatrix(bigImageMatrixRect, thumbImageMatrixRect, mThumbImageMatrix);
            mImageView.outerMatrixTo(mThumbImageMatrix, 0);
            mImageView.outerMatrixTo(new Matrix(), ANIM_TIME);
        });
        mImageView.setOnClickListener(v -> finish());
    }

    @Override
    public void finish() {
        if ((mBackgroundAnimator != null && mBackgroundAnimator.isRunning())) {
            return;
        }

        //背景动画
        mBackgroundAnimator = ObjectAnimator.ofFloat(mBackground, "alpha", mBackground.getAlpha(), 0f);
        mBackgroundAnimator.setDuration(ANIM_TIME);
        mBackgroundAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                PicViewActivity.super.finish();
                overridePendingTransition(0, 0);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mBackgroundAnimator.start();

        //mask动画
        mImageView.zoomMaskTo(mThumbMaskRect, ANIM_TIME);

        //图片缩小动画
        mImageView.outerMatrixTo(mThumbImageMatrix, ANIM_TIME);
    }
}