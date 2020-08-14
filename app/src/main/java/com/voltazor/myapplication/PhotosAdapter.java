package com.voltazor.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.voltazor.myapplication.PhotoDragController.DragHelperAdapter;
import com.voltazor.myapplication.PhotoDragController.ItemViewHelper;
import com.voltazor.myapplication.PhotoDragController.OnDragEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by voltazor on 11/04/16.
 */
public class PhotosAdapter extends RecyclerView.Adapter<PhotosAdapter.PhotoViewHolder> implements DragHelperAdapter, OnDragEventListener {

    private static final int ANIM_DURATION = 300;

    private static final int VIEW_TYPE_PHOTO = 0;
    private static final int VIEW_TYPE_STUB = 1;

    private static final int MAIN_PHOTO_POS = 0;
    private static final int COUNT = 5;

    private Context mContext;
    private boolean isBroAvatar;
    private LayoutInflater mLayoutInflater;
    private List<UserPicture> mUserPictures;
    private PhotosControlCallback mCallback;
    private GridLayoutManager mLayoutManager;
    private PhotoDragController mPhotoDragController;

    private View mAnimatedView;
    private Map<Integer, PhotoViewHolder> mViewHolderMap = new HashMap<>();

    public PhotosAdapter(Context context, List<UserPicture> pictures, GridLayoutManager layoutManager, PhotosControlCallback callback, boolean isBroAvatar) {
        mContext = context;
        mCallback = callback;
        mUserPictures = pictures;
        mLayoutManager = layoutManager;
        this.isBroAvatar = isBroAvatar;
        mLayoutInflater = LayoutInflater.from(context);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return position == MAIN_PHOTO_POS ? mLayoutManager.getSpanCount() : 1;
            }
        });
    }

    @Override
    public PhotoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new PhotoViewHolder(mLayoutInflater.inflate(R.layout.item_photo, parent, false));
    }

    @Override
    public void onBindViewHolder(final PhotoViewHolder holder, int position) {
        setViewInfo(holder);
        holder.progress.stop();
        int viewType = getItemViewType(position);
        if (viewType == VIEW_TYPE_PHOTO) {
            final UserPicture picture = mUserPictures.get(position);
            holder.progress.start();
            Picasso.with(mContext).load(picture.getPictureUrl()).fit().config(Bitmap.Config.RGB_565).into(holder.photo, new Callback() {
                @Override
                public void onSuccess() {
                    holder.progress.stop();
                }

                @Override
                public void onError() {
                    holder.progress.stop();
                }
            });
            holder.photo.setBackground(null);

            if (position == MAIN_PHOTO_POS) {
                if (isBroAvatar) {
                    holder.controlIcon.setRotation(45);
                    holder.photo.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            holder.progress.start();
                            mCallback.onAddPhoto();
                        }
                    });
                } else {
                    holder.controlIcon.setRotation(0);
                    holder.photo.setOnClickListener(null);
                    holder.controlIcon.setVisibility(getUserPicturesCount() > 1 ? View.VISIBLE : View.GONE);
                    holder.controlIcon.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mCallback.onRemovePhoto(picture);
                        }
                    });
                }
            } else {
                holder.controlIcon.setRotation(0);
                holder.controlIcon.setVisibility(View.VISIBLE);
                holder.photo.setTag(picture);
                holder.photo.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mCallback.onRemovePhoto((UserPicture) v.getTag());
                    }
                });
                holder.controlIcon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mCallback.onRemovePhoto(picture);
                    }
                });
            }

            if (mPhotoDragController != null) {
                holder.photo.setOnTouchListener(new LongPressDetector(mContext, holder, mPhotoDragController));
            }
            mViewHolderMap.put(position, holder);
            mPhotoDragController.updateViewHelper(position, holder);
        } else {
            holder.controlIcon.setRotation(45);
            holder.photo.setImageDrawable(null);
            holder.photo.setOnTouchListener(null);
            holder.photo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    holder.progress.start();
                    mCallback.onAddPhoto();
                }
            });
            holder.photo.setBackgroundResource(R.drawable.bg_item_photo);
            holder.controlIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    holder.progress.start();
                    mCallback.onAddPhoto();
                }
            });
        }
    }

    private void setViewInfo(final PhotoViewHolder holder) {
        if (mPhotoDragController.isViewInfoNeeded(holder.getAdapterPosition())) {
            final View view = holder.getItemView();
            view.post(new Runnable() {
                @Override
                public void run() {
                    mPhotoDragController.setViewInfo(holder.getItemPosition(), holder.getItemView());
                }
            });
        }
    }

    @Override
    public long getItemId(int position) {
        return getItemViewType(position) != VIEW_TYPE_STUB ? mUserPictures.get(position).getId() : super.getItemId(position);
    }

    @Override
    public int getItemViewType(int position) {
        return (position >= 0 && position < getUserPicturesCount()) ? VIEW_TYPE_PHOTO : VIEW_TYPE_STUB;
    }

    public int getUserPicturesCount() {
        return mUserPictures.size();
    }

    @Override
    public int getItemCount() {
        return mUserPictures.size();
    }

    public void addPhoto(UserPicture picture) {
        if (isBroAvatar) {
            mUserPictures.remove(0);
            isBroAvatar = false;
        }
        mUserPictures.add(picture);
        notifyDataSetChanged();
    }

    @Override
    public void onDragStarted() {
    }

    @Override
    public boolean onItemMoved(ItemViewHelper from, ItemViewHelper to) {
        int fromPosition = from.getItemPosition();
        int toPosition = to.getItemPosition();
        if (onCanDropOver(fromPosition, toPosition)) {
            mUserPictures.add(toPosition, mUserPictures.remove(fromPosition));
            if (mAnimatedView != null && fromPosition * toPosition == 0) {
                if (toPosition == 0) {
                    from = mViewHolderMap.get(1);
                } else if (fromPosition == 0) {
                    to = mViewHolderMap.get(1);
                }
                animate(from, to);
            }
            notifyDataSetChanged();

            from.getItemView().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mPhotoDragController.unblockController();
                }
            }, ANIM_DURATION);
            mCallback.onOrderChanged(mUserPictures);
            return true;
        }
        return false;
    }

    private void animate(ItemViewHelper from, final ItemViewHelper to) {
        View fromView = from.getItemView();
        View toView = to.getItemView();

        mAnimatedView.setBackground(to.getItemDrawable());

        mAnimatedView.setScaleX(1);
        mAnimatedView.setScaleY(1);

        int widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(toView.getWidth(), View.MeasureSpec.EXACTLY);
        int heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(toView.getHeight(), View.MeasureSpec.EXACTLY);
        mAnimatedView.setLayoutParams(new FrameLayout.LayoutParams(toView.getWidth(), toView.getHeight()));
        mAnimatedView.measure(widthMeasureSpec, heightMeasureSpec);

        mAnimatedView.setX(toView.getX());
        mAnimatedView.setY(toView.getY());

        float scaleX = (float) fromView.getWidth() / (float) toView.getWidth();
        float scaleY = (float) fromView.getHeight() / (float) toView.getHeight();

        mAnimatedView.setVisibility(View.VISIBLE);
        to.getItemView().setVisibility(View.INVISIBLE);

        scaleX = shouldScale(scaleX) ? scaleX : 1;
        scaleY = shouldScale(scaleY) ? scaleY : 1;

        float toX = fromView.getX() - toView.getX() - toView.getWidth() * (1 - scaleX) / 2;
        float toY = fromView.getY() - toView.getY() - toView.getHeight() * (1 - scaleY) / 2;

        mAnimatedView.animate().scaleX(scaleX).scaleY(scaleY)
                .translationXBy(toX).translationYBy(toY)
                .setDuration(ANIM_DURATION).withEndAction(new Runnable() {
            @Override
            public void run() {
                to.getItemView().setVisibility(View.VISIBLE);
                mAnimatedView.setVisibility(View.INVISIBLE);
            }
        });
    }

    private boolean shouldScale(float scale) {
        return Math.abs(scale - 1) > 0.05f;
    }

    @Override
    public void onDragFinished() {
        notifyDataSetChanged();
    }

    public boolean onCanDropOver(int fromPosition, int toPosition) {
        return fromPosition >= 0 && fromPosition < getUserPicturesCount() && toPosition >= 0 && toPosition < getUserPicturesCount();
    }

    @Override
    public void setPhotoDragController(PhotoDragController listener, View animatedView) {
        mPhotoDragController = listener;
        mAnimatedView = animatedView;
    }

    public void removeUserPicture(UserPicture picture) {
        int position = mUserPictures.indexOf(picture);
        if (position >= 0 && position < mUserPictures.size()) {
            mUserPictures.remove(position);
            mViewHolderMap.remove(position);
            mPhotoDragController.removeViewHelper(position);
            notifyDataSetChanged();
        }
    }

    public static class PhotoViewHolder extends RecyclerView.ViewHolder implements ItemViewHelper {
        private ImageView photo;
        private View controlIcon;
        private ProgressVector progress;

        public PhotoViewHolder(View itemView) {
            super(itemView);
            photo = itemView.findViewById(R.id.photo);
            controlIcon = itemView.findViewById(R.id.control_icon);
            progress = itemView.findViewById(R.id.progress);
        }

        @Override
        public Drawable getItemDrawable() {
            return photo.getDrawable();
        }

        @Override
        public View getItemView() {
            return itemView;
        }

        @Override
        public int getItemPosition() {
            return getAdapterPosition();
        }

    }

    public static class SpacesItemDecoration extends RecyclerView.ItemDecoration {

        private int mItemOffset;

        public SpacesItemDecoration(@NonNull Context context) {
            mItemOffset = context.getResources().getDimensionPixelSize(R.dimen.margin_medium_half);
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            outRect.bottom = mItemOffset;
            outRect.left = mItemOffset / 2;
            outRect.right = mItemOffset / 2;
        }

    }

    public interface PhotosControlCallback {

        void onAddPhoto();

        void onOrderChanged(List<UserPicture> pictures);

        void onRemovePhoto(UserPicture picture);

    }

    private static class LongPressDetector extends GestureDetector.SimpleOnGestureListener implements View.OnTouchListener {

        private final PhotoViewHolder mViewHolder;
        private final GestureDetector mGestureDetector;
        private final PhotoDragController mPhotoDragController;

        private LongPressDetector(Context context, PhotoViewHolder viewHolder, @NonNull PhotoDragController dragController) {
            mViewHolder = viewHolder;
            mPhotoDragController = dragController;
            mGestureDetector = new GestureDetector(context, this);
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return mGestureDetector.onTouchEvent(event);
        }

        @Override
        public void onLongPress(MotionEvent e) {
            mPhotoDragController.viewSelected(mViewHolder, e.getRawX(), e.getRawY());
        }

    }

}
