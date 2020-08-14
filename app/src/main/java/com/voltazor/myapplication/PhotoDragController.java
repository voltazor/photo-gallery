package com.voltazor.myapplication;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

/**
 * Created by voltazor on 11/04/16.
 */
public class PhotoDragController implements RecyclerView.OnItemTouchListener {

    private static final long DURATION = 250;

    private boolean isHold;
    private volatile boolean isDropEnabled;
    private final ImageView mThumbView;
    private ItemViewHelper mViewHelper;

    private final int mPos[] = new int[2];
    private final Rect mPadding = new Rect();
    private final Rect mSourceRect = new Rect();
    private final Rect mTargetRect = new Rect();
    private final Rect mIntersection = new Rect();

    private OnDragEventListener mDragEventListener;
    private List<Integer> mCollidedHolders = new ArrayList<>();
    private Map<Integer, Integer> mCollisionValue = new HashMap<>();
    private Map<Integer, ItemViewHelper> mViewHelperMap = new HashMap<>();
    private Map<Integer, ViewInfo> mViewInfoMap = new HashMap<>();

    public PhotoDragController(@NonNull DragHelperAdapter dragHelper, @NonNull ImageView thumbView, @NonNull View animatedView) {
        this(dragHelper, thumbView, animatedView, null);
    }

    public PhotoDragController(@NonNull DragHelperAdapter dragHelper, @NonNull ImageView thumbView, @NonNull View animatedView, OnDragEventListener dragEventListener) {
        dragHelper.setPhotoDragController(this, animatedView);
        mThumbView = thumbView;
        mThumbView.setBackgroundResource(R.drawable.shadow);
        mThumbView.getBackground().getPadding(mPadding);
        mDragEventListener = dragEventListener;
    }

    public void setDragEventListener(OnDragEventListener dragEventListener) {
        mDragEventListener = dragEventListener;
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_UP) {
            if (isHold) {
                animateToOriginal();
            }
            return false;
        }
        return isHold;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_UP:
                if (isHold) {
                    animateToOriginal();
                    isHold = false;
                }
            case MotionEvent.ACTION_MOVE:
                if (isHold) {
                    moveThumb(e);
                }
        }
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }

    public void updateViewHelper(int position, ItemViewHelper viewHelper) {
        mViewHelperMap.put(position, viewHelper);
        if (mViewHelper != null && mViewHelper.getItemPosition() == position) {
            mViewHelper = viewHelper;
        }
    }

    public boolean isViewInfoNeeded(int position) {
        return !mViewInfoMap.containsKey(position);
    }

    public void setViewInfo(int position, View view) {
        if (isViewInfoNeeded(position)) {
            view.getLocationOnScreen(mPos);
            mViewInfoMap.put(position, new ViewInfo(position, mPos[0], mPos[1], view.getWidth(), view.getHeight()));
            Timber.d(mViewInfoMap.get(position).toString());
        }
    }

    public void removeViewHelper(int position) {
        mViewHelperMap.remove(position);
        if (mViewHelper != null && mViewHelper.getItemPosition() == position) {
            mViewHelper = null;
        }
    }

    public void viewSelected(ItemViewHelper viewHelper, float rawX, float rawY) {
        isHold = true;
        isDropEnabled = true;
        if (mDragEventListener != null) {
            mDragEventListener.onDragStarted();
        }
        mViewHelper = viewHelper;
        View view = mViewHelper.getItemView();
        ViewInfo viewInfo = getViewInfo();
        mThumbView.setImageDrawable(mViewHelper.getItemDrawable());
        mThumbView.setBackgroundResource(R.drawable.shadow);

        float thumbWidth = mThumbView.getWidth() + mPadding.width();
        float thumbHeight = mThumbView.getHeight() + mPadding.height();

        float scaleX = (float) viewInfo.width / thumbWidth;
        float scaleY = (float) viewInfo.height / thumbHeight;

        mThumbView.setScaleX(scaleX);
        mThumbView.setScaleY(scaleY);

        mThumbView.getLocationOnScreen(mPos);
        float thumbDx = mPos[0] - mThumbView.getX();
        float thumbDy = mPos[1] - mThumbView.getY();

        mThumbView.setX(viewInfo.x - thumbDx);
        mThumbView.setY(viewInfo.y - thumbDy);

        mThumbView.getLocationOnScreen(mPos);

        float posX = rawX - mThumbView.getPivotX();
        float posY = rawY - mThumbView.getPivotY();
        float x = posX - mPos[0] - viewInfo.width * (1 - (thumbWidth / (float) viewInfo.width)) / 2f;
        float y = posY - mPos[1] - viewInfo.height * (1 - (thumbHeight / (float) viewInfo.height)) / 2f;

        mThumbView.setVisibility(View.VISIBLE);
        view.setVisibility(View.INVISIBLE);
        mThumbView.animate().setInterpolator(new OvershootInterpolator()).setDuration(DURATION)
                .translationXBy(x).translationYBy(y).scaleX(1).scaleY(1);
    }

    private void moveThumb(MotionEvent e) {
        mThumbView.getLocationOnScreen(mPos);
        float thumbDx = mPos[0] - mThumbView.getX();
        float thumbDy = mPos[1] - mThumbView.getY();
        mThumbView.setX(e.getRawX() - thumbDx - mThumbView.getPivotX());
        mThumbView.setY(e.getRawY() - thumbDy - mThumbView.getPivotY());

        if (isDropEnabled) {
            performDropIfRequired();
        }
    }

    private void performDropIfRequired() {
        mCollidedHolders.clear();
        for (ItemViewHelper helper : mViewHelperMap.values()) {
            if (helper != null && helper.getItemPosition() != mViewHelper.getItemPosition()) {
                Pair<Boolean, Integer> collision = checkCollision(mThumbView, helper.getItemView());
                if (collision.first) {
                    int position = helper.getItemPosition();
                    mCollisionValue.put(position, collision.second);
                    mCollidedHolders.add(position);
                }
            }
        }
        int maxInterceptionSquare = 0;
        int chosenPosition = -1;
        for (int position : mCollidedHolders) {
            int interceptionSquare = mCollisionValue.get(position);
            if (maxInterceptionSquare < interceptionSquare) {
                maxInterceptionSquare = interceptionSquare;
                chosenPosition = position;
            }
        }
        ItemViewHelper helper = mViewHelperMap.get(chosenPosition);
        if (helper != null) {
            if (mDragEventListener != null) {
                isDropEnabled = !mDragEventListener.onItemMoved(mViewHelper, helper);
            }
        }
    }

    private void animateToOriginal() {
        final View view = mViewHelper.getItemView();
        ViewInfo viewInfo = getViewInfo();
        if (viewInfo != null) {
            mThumbView.setBackground(null);
            mThumbView.setPadding(0, 0, 0, 0);

            float scaleX = (float) viewInfo.width / (float) mThumbView.getWidth();
            float scaleY = (float) viewInfo.height / (float) mThumbView.getHeight();

            float viewX = viewInfo.x;
            float viewY = viewInfo.y;

            mThumbView.getLocationOnScreen(mPos);

            float x = viewX - mPos[0] - mThumbView.getWidth() * (1 - scaleX) / 2f;
            float y = viewY - mPos[1] - mThumbView.getHeight() * (1 - scaleY) / 2f;

            mThumbView.animate().setInterpolator(new OvershootInterpolator()).setDuration(DURATION)
                    .translationXBy(x).translationYBy(y).scaleX(scaleX).scaleY(scaleY).withEndAction(new Runnable() {
                @Override
                public void run() {
                    view.setVisibility(View.VISIBLE);
                    mThumbView.setVisibility(View.INVISIBLE);
                    isHold = false;
                    isDropEnabled = false;
                    if (mDragEventListener != null) {
                        mDragEventListener.onDragFinished();
                    }
                }
            });
        } else {
            view.setVisibility(View.VISIBLE);
            mThumbView.setVisibility(View.INVISIBLE);
        }

    }

    private ViewInfo getViewInfo() {
        ViewInfo viewInfo = mViewInfoMap.get(mViewHelper.getItemPosition());
        if (viewInfo == null) {
            View v = mViewHelper.getItemView();
            viewInfo = new ViewInfo(mViewHelper.getItemPosition(), v.getX(), v.getY(), v.getWidth(), v.getHeight());
        }
        return viewInfo;
    }

    private Pair<Boolean, Integer> checkCollision(View source, View target) {
        source.getLocationOnScreen(mPos);
        mSourceRect.set(mPos[0], mPos[1], mPos[0] + source.getWidth(), mPos[1] + source.getHeight());
        int sourceSquare = mSourceRect.width() * mSourceRect.height();

        target.getLocationOnScreen(mPos);
        mTargetRect.set(mPos[0], mPos[1], mPos[0] + target.getWidth(), mPos[1] + target.getHeight());
        int targetSquare = mTargetRect.width() * mTargetRect.height();

        if (mSourceRect.intersect(mTargetRect)) {
            mIntersection.setEmpty();
            if (mIntersection.setIntersect(mSourceRect, mTargetRect)) {
                int interceptionSquare = mIntersection.width() * mIntersection.height();
                if (interceptionSquare > (targetSquare * .5f) || interceptionSquare > (sourceSquare * .5f)
                        || (mSourceRect.bottom < mTargetRect.bottom && mSourceRect.top > mTargetRect.top)) {
                    return new Pair<>(true, interceptionSquare);
                }
            }
        }
        return new Pair<>(false, 0);
    }

    public void unblockController() {
        isDropEnabled = true;
    }

    public interface DragHelperAdapter {

        void setPhotoDragController(PhotoDragController listener, View animatedView);

    }

    public interface OnDragEventListener {

        void onDragStarted();

        boolean onItemMoved(ItemViewHelper from, ItemViewHelper to);

        void onDragFinished();

    }

    public interface ItemViewHelper {

        Drawable getItemDrawable();

        View getItemView();

        int getItemPosition();

    }

    private static class ViewInfo {

        public final int position;
        public final float x, y;
        public final int width, height;

        public ViewInfo(int position, float x, float y, int width, int height) {
            this.position = position;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        @Override
        public String toString() {
            return "ViewInfo{" +
                    "position=" + position +
                    ", x=" + x +
                    ", y=" + y +
                    ", width=" + width +
                    ", height=" + height +
                    '}';
        }
    }

}

