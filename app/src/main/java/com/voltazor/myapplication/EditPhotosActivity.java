package com.voltazor.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.*;

/**
 * Created by voltazor on 10/02/16.
 */
public class EditPhotosActivity extends AppCompatActivity implements PhotosAdapter.PhotosControlCallback {

    private ImageView mThumbView;
    private RecyclerView mPhotosRecyclerView;
    private View mAnimatedView;

    private PhotosAdapter mAdapter;
    private GridLayoutManager mLayoutManager;

    private int id = 1;
    private Random mRandom = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_photos);

        mThumbView = findViewById(R.id.thumb);
        mAnimatedView = findViewById(R.id.animated);
        mPhotosRecyclerView = findViewById(R.id.photos);
        setTitle(R.string.edit_photos);

        mPhotosRecyclerView.setLayoutManager(mLayoutManager = new GridLayoutManager(this, 4));
        mPhotosRecyclerView.addItemDecoration(new PhotosAdapter.SpacesItemDecoration(this));

        setPhotos(createPhotos(), false);
    }

    public void setPhotos(List<UserPicture> pictures, boolean isBroAvatar) {
        mAdapter = new PhotosAdapter(this, pictures, mLayoutManager, this, isBroAvatar);
        mAdapter.setHasStableIds(true);
        mPhotosRecyclerView.addOnItemTouchListener(new PhotoDragController(mAdapter, mThumbView, mAnimatedView, mAdapter));
        mPhotosRecyclerView.setAdapter(mAdapter);
        mPhotosRecyclerView.setItemAnimator(new DummyAnimator());
    }

    @Override
    public void onAddPhoto() {
        if (mAdapter.getUserPicturesCount() < mAdapter.getItemCount()) {
            mAdapter.addPhoto(createUserPhoto());
        }
    }

    @Override
    public void onRemovePhoto(UserPicture picture) {
        mAdapter.removeUserPicture(picture);
        mPhotosRecyclerView.getItemAnimator().dispatchAnimationsFinished();
    }

    @Override
    public void onOrderChanged(List<UserPicture> pictures) {

    }

    private List<UserPicture> createPhotos() {
        List<UserPicture> pictures = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            pictures.add(createUserPhoto());
        }
        return pictures;
    }

    private UserPicture createUserPhoto() {
        int index = id++;
        UserPicture picture = new UserPicture();
        picture.setId(index * 1000);
        switch (mRandom.nextInt(4)) {
            case 1:
                picture.setPictureUrl("http://67.media.tumblr.com/2d6fd89aeb4403659aa7167191eae531/tumblr_o9s3mpQDUy1s37ot7o1_1280.jpg");
                break;
            case 2:
                picture.setPictureUrl("http://66.media.tumblr.com/89603a907fcbb2b296b72f0052827d91/tumblr_o9qas2HIsS1tgc11ao1_1280.jpg");
                break;
            case 3:
                picture.setPictureUrl("http://66.media.tumblr.com/12696d73587e641d8d42fe581b9af5d4/tumblr_o9tz4xwbiS1s37ot7o1_1280.jpg");
                break;
            case 4:
            default:
                picture.setPictureUrl("http://66.media.tumblr.com/95e0e3f94b5867207ba4e1dbeb497a74/tumblr_my7a41OcEt1qkm9pho1_500.jpg");
                break;
        }
        return picture;
    }

}
