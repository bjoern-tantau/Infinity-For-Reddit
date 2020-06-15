package ml.docilealligator.infinityforreddit.Activity;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.thefuntasty.hauler.HaulerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import ml.docilealligator.infinityforreddit.API.ImgurAPI;
import ml.docilealligator.infinityforreddit.Font.ContentFontFamily;
import ml.docilealligator.infinityforreddit.Font.ContentFontStyle;
import ml.docilealligator.infinityforreddit.Font.FontFamily;
import ml.docilealligator.infinityforreddit.Font.FontStyle;
import ml.docilealligator.infinityforreddit.Font.TitleFontFamily;
import ml.docilealligator.infinityforreddit.Font.TitleFontStyle;
import ml.docilealligator.infinityforreddit.Fragment.ViewImgurImageFragment;
import ml.docilealligator.infinityforreddit.Fragment.ViewImgurVideoFragment;
import ml.docilealligator.infinityforreddit.ImgurMedia;
import ml.docilealligator.infinityforreddit.Infinity;
import ml.docilealligator.infinityforreddit.R;
import ml.docilealligator.infinityforreddit.SetAsWallpaperCallback;
import ml.docilealligator.infinityforreddit.Utils.APIUtils;
import ml.docilealligator.infinityforreddit.Utils.JSONUtils;
import ml.docilealligator.infinityforreddit.Utils.SharedPreferencesUtils;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class ViewImgurMediaActivity extends AppCompatActivity implements SetAsWallpaperCallback {

    public static final String EXTRA_IMGUR_TYPE = "EIT";
    public static final String EXTRA_IMGUR_ID = "EII";
    public static final int IMGUR_TYPE_GALLERY = 0;
    public static final int IMGUR_TYPE_ALBUM = 1;
    public static final int IMGUR_TYPE_IMAGE = 2;
    private static final String IMGUR_IMAGES_STATE = "IIS";

    @BindView(R.id.hauler_view_view_imgur_media_activity)
    HaulerView haulerView;
    @BindView(R.id.progress_bar_view_imgur_media_activity)
    ProgressBar progressBar;
    @BindView(R.id.view_pager_view_imgur_media_activity)
    ViewPager viewPager;
    @BindView(R.id.load_image_error_linear_layout_view_imgur_media_activity)
    LinearLayout errorLinearLayout;
    private SectionsPagerAdapter sectionsPagerAdapter;
    private ArrayList<ImgurMedia> images;
    @Inject
    @Named("imgur")
    Retrofit imgurRetrofit;
    @Inject
    @Named("default")
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((Infinity) getApplication()).getAppComponent().inject(this);

        getTheme().applyStyle(R.style.Theme_Normal, true);

        getTheme().applyStyle(FontStyle.valueOf(sharedPreferences
                .getString(SharedPreferencesUtils.FONT_SIZE_KEY, FontStyle.Normal.name())).getResId(), true);

        getTheme().applyStyle(TitleFontStyle.valueOf(sharedPreferences
                .getString(SharedPreferencesUtils.TITLE_FONT_SIZE_KEY, TitleFontStyle.Normal.name())).getResId(), true);

        getTheme().applyStyle(ContentFontStyle.valueOf(sharedPreferences
                .getString(SharedPreferencesUtils.CONTENT_FONT_SIZE_KEY, ContentFontStyle.Normal.name())).getResId(), true);

        getTheme().applyStyle(FontFamily.valueOf(sharedPreferences
                .getString(SharedPreferencesUtils.FONT_FAMILY_KEY, FontFamily.Default.name())).getResId(), true);

        getTheme().applyStyle(TitleFontFamily.valueOf(sharedPreferences
                .getString(SharedPreferencesUtils.TITLE_FONT_FAMILY_KEY, TitleFontFamily.Default.name())).getResId(), true);

        getTheme().applyStyle(ContentFontFamily.valueOf(sharedPreferences
                .getString(SharedPreferencesUtils.CONTENT_FONT_FAMILY_KEY, ContentFontFamily.Default.name())).getResId(), true);

        setContentView(R.layout.activity_view_imgur_media);

        ButterKnife.bind(this);

        ActionBar actionBar = getSupportActionBar();
        Drawable upArrow = getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp);
        actionBar.setHomeAsUpIndicator(upArrow);
        actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.transparentActionBarAndExoPlayerControllerColor)));

        setTitle(" ");

        String imgurId = getIntent().getStringExtra(EXTRA_IMGUR_ID);
        if (imgurId == null) {
            finish();
            return;
        }

        if (savedInstanceState != null) {
            images = savedInstanceState.getParcelableArrayList(IMGUR_IMAGES_STATE);
        }

        haulerView.setOnDragDismissedListener(dragDirection -> finish());

        if (images == null) {
            fetchImgurMedia(imgurId);
        } else {
            progressBar.setVisibility(View.GONE);
            setupViewPager();
        }

        errorLinearLayout.setOnClickListener(view -> {
            fetchImgurMedia(imgurId);
        });
    }

    private void fetchImgurMedia(String imgurId) {
        errorLinearLayout.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        switch (getIntent().getIntExtra(EXTRA_IMGUR_TYPE, IMGUR_TYPE_IMAGE)) {
            case IMGUR_TYPE_GALLERY:
                imgurRetrofit.create(ImgurAPI.class).getGalleryImages(APIUtils.IMGUR_CLIENT_ID, imgurId)
                        .enqueue(new Callback<String>() {
                            @Override
                            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                                if (response.isSuccessful()) {
                                    new ParseImgurImagesAsyncTask(response.body(), new ParseImgurImagesAsyncTask.ParseImgurImagesAsyncTaskListener() {
                                        @Override
                                        public void success(ArrayList<ImgurMedia> images) {
                                            ViewImgurMediaActivity.this.images = images;
                                            progressBar.setVisibility(View.GONE);
                                            errorLinearLayout.setVisibility(View.GONE);
                                            setupViewPager();
                                        }

                                        @Override
                                        public void failed() {
                                            progressBar.setVisibility(View.GONE);
                                            errorLinearLayout.setVisibility(View.VISIBLE);
                                        }
                                    }).execute();
                                } else {
                                    progressBar.setVisibility(View.GONE);
                                    errorLinearLayout.setVisibility(View.VISIBLE);
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                                progressBar.setVisibility(View.GONE);
                                errorLinearLayout.setVisibility(View.VISIBLE);
                            }
                        });
                break;
            case IMGUR_TYPE_ALBUM:
                imgurRetrofit.create(ImgurAPI.class).getAlbumImages(APIUtils.IMGUR_CLIENT_ID, imgurId)
                        .enqueue(new Callback<String>() {
                            @Override
                            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                                if (response.isSuccessful()) {
                                    new ParseImgurImagesAsyncTask(response.body(), new ParseImgurImagesAsyncTask.ParseImgurImagesAsyncTaskListener() {
                                        @Override
                                        public void success(ArrayList<ImgurMedia> images) {
                                            ViewImgurMediaActivity.this.images = images;
                                            progressBar.setVisibility(View.GONE);
                                            errorLinearLayout.setVisibility(View.GONE);
                                            setupViewPager();
                                        }

                                        @Override
                                        public void failed() {
                                            progressBar.setVisibility(View.GONE);
                                            errorLinearLayout.setVisibility(View.VISIBLE);
                                        }
                                    }).execute();
                                } else {
                                    progressBar.setVisibility(View.GONE);
                                    errorLinearLayout.setVisibility(View.VISIBLE);
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                                progressBar.setVisibility(View.GONE);
                                errorLinearLayout.setVisibility(View.VISIBLE);
                            }
                        });
                break;
            case IMGUR_TYPE_IMAGE:
                imgurRetrofit.create(ImgurAPI.class).getImage(APIUtils.IMGUR_CLIENT_ID, imgurId)
                        .enqueue(new Callback<String>() {
                            @Override
                            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                                if (response.isSuccessful()) {
                                    new ParseImgurImageAsyncTask(response.body(), new ParseImgurImageAsyncTask.ParseImgurImageAsyncTaskListener() {
                                        @Override
                                        public void success(ImgurMedia image) {
                                            ViewImgurMediaActivity.this.images = new ArrayList<>();
                                            ViewImgurMediaActivity.this.images.add(image);
                                            progressBar.setVisibility(View.GONE);
                                            errorLinearLayout.setVisibility(View.GONE);
                                            setupViewPager();
                                        }

                                        @Override
                                        public void failed() {
                                            progressBar.setVisibility(View.GONE);
                                            errorLinearLayout.setVisibility(View.VISIBLE);
                                        }
                                    }).execute();
                                } else {
                                    progressBar.setVisibility(View.GONE);
                                    errorLinearLayout.setVisibility(View.VISIBLE);
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                                progressBar.setVisibility(View.GONE);
                                errorLinearLayout.setVisibility(View.VISIBLE);
                            }
                        });
                break;
        }
    }

    private void setupViewPager() {
        setToolbarTitle(0);
        sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                setToolbarTitle(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        viewPager.setAdapter(sectionsPagerAdapter);
    }

    private void setToolbarTitle(int position) {
        if (images.get(position).getType() == ImgurMedia.TYPE_VIDEO) {
            setTitle(getString(R.string.view_imgur_media_activity_video_label, position + 1, images.size()));
        } else {
            setTitle(getString(R.string.view_imgur_media_activity_image_label, position + 1, images.size()));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return false;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(IMGUR_IMAGES_STATE, images);
    }

    public void setAsWallpaper(String link, int setTo) {
        Toast.makeText(ViewImgurMediaActivity.this, R.string.save_image_first, Toast.LENGTH_SHORT).show();
        Glide.with(this).asBitmap().load(link).into(new CustomTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                WallpaperManager manager = WallpaperManager.getInstance(ViewImgurMediaActivity.this);

                DisplayMetrics metrics = new DisplayMetrics();
                WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

                Rect rect = null;

                if (windowManager != null) {
                    windowManager.getDefaultDisplay().getMetrics(metrics);
                    int height = metrics.heightPixels;
                    int width = metrics.widthPixels;

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                        resource = ThumbnailUtils.extractThumbnail(resource, width, height);
                    }

                    float imageAR = (float) resource.getWidth() / (float) resource.getHeight();
                    float screenAR = (float) width / (float) height;

                    if (imageAR > screenAR) {
                        int desiredWidth = (int) (resource.getHeight() * screenAR);
                        rect = new Rect((resource.getWidth() - desiredWidth) / 2, 0, resource.getWidth(), resource.getHeight());
                    } else {
                        int desiredHeight = (int) (resource.getWidth() / screenAR);
                        rect = new Rect(0, (resource.getHeight() - desiredHeight) / 2, resource.getWidth(), (resource.getHeight() + desiredHeight) / 2);
                    }
                }
                try {
                    switch (setTo) {
                        case 0:
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                manager.setBitmap(resource, rect, true, WallpaperManager.FLAG_SYSTEM);
                            }
                            break;
                        case 1:
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                manager.setBitmap(resource, rect, true, WallpaperManager.FLAG_LOCK);
                            }
                            break;
                        case 2:
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                manager.setBitmap(resource, rect, true, WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK);
                            } else {
                                manager.setBitmap(resource);
                            }
                            break;
                    }
                    Toast.makeText(ViewImgurMediaActivity.this, R.string.wallpaper_set, Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Toast.makeText(ViewImgurMediaActivity.this, R.string.error_set_wallpaper, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {

            }
        });
    }

    @Override
    public void setToHomeScreen(int viewPagerPosition) {
        setAsWallpaper(images.get(viewPagerPosition).getLink(), 0);
    }

    @Override
    public void setToLockScreen(int viewPagerPosition) {
        setAsWallpaper(images.get(viewPagerPosition).getLink(), 1);
    }

    @Override
    public void setToBoth(int viewPagerPosition) {
        setAsWallpaper(images.get(viewPagerPosition).getLink(), 2);
    }

    public int getCurrentPagePosition() {
        return viewPager.getCurrentItem();
    }

    private class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        SectionsPagerAdapter(@NonNull FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            ImgurMedia imgurMedia = images.get(position);
            if (imgurMedia.getType() == ImgurMedia.TYPE_VIDEO) {
                ViewImgurVideoFragment fragment = new ViewImgurVideoFragment();
                Bundle bundle = new Bundle();
                bundle.putParcelable(ViewImgurVideoFragment.EXTRA_IMGUR_VIDEO, imgurMedia);
                fragment.setArguments(bundle);
                return fragment;
            } else {
                ViewImgurImageFragment fragment = new ViewImgurImageFragment();
                Bundle bundle = new Bundle();
                bundle.putParcelable(ViewImgurImageFragment.EXTRA_IMGUR_IMAGES, imgurMedia);
                fragment.setArguments(bundle);
                return fragment;
            }
        }

        @Override
        public int getCount() {
            return images.size();
        }
    }

    private static class ParseImgurImagesAsyncTask extends AsyncTask<Void, Void, Void> {

        private String response;
        private ArrayList<ImgurMedia> images;
        private boolean parseFailed = false;
        private ParseImgurImagesAsyncTaskListener parseImgurImagesAsyncTaskListener;

        interface ParseImgurImagesAsyncTaskListener {
            void success(ArrayList<ImgurMedia> images);
            void failed();
        }

        ParseImgurImagesAsyncTask(String response, ParseImgurImagesAsyncTaskListener parseImgurImagesAsyncTaskListener) {
            this.response = response;
            this.parseImgurImagesAsyncTaskListener = parseImgurImagesAsyncTaskListener;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                JSONArray jsonArray = new JSONObject(response).getJSONObject(JSONUtils.DATA_KEY).getJSONArray(JSONUtils.IMAGES_KEY);
                images = new ArrayList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject image = jsonArray.getJSONObject(i);
                    String type = image.getString(JSONUtils.TYPE_KEY);
                    if (type.contains("gif")) {
                        images.add(new ImgurMedia(image.getString(JSONUtils.ID_KEY),
                                image.getString(JSONUtils.TITLE_KEY), image.getString(JSONUtils.DESCRIPTION_KEY),
                                "video/mp4", image.getString(JSONUtils.MP4_KEY)));
                    } else {
                        images.add(new ImgurMedia(image.getString(JSONUtils.ID_KEY),
                                image.getString(JSONUtils.TITLE_KEY), image.getString(JSONUtils.DESCRIPTION_KEY),
                                type, image.getString(JSONUtils.LINK_KEY)));
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
                parseFailed = true;
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (parseFailed) {
                parseImgurImagesAsyncTaskListener.failed();
            } else {
                parseImgurImagesAsyncTaskListener.success(images);
            }
        }
    }

    private static class ParseImgurImageAsyncTask extends AsyncTask<Void, Void, Void> {

        private String response;
        private ImgurMedia image;
        private boolean parseFailed = false;
        private ParseImgurImageAsyncTaskListener parseImgurImageAsyncTaskListener;

        interface ParseImgurImageAsyncTaskListener {
            void success(ImgurMedia image);
            void failed();
        }

        ParseImgurImageAsyncTask(String response, ParseImgurImageAsyncTaskListener parseImgurImageAsyncTaskListener) {
            this.response = response;
            this.parseImgurImageAsyncTaskListener = parseImgurImageAsyncTaskListener;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                JSONObject image = new JSONObject(response).getJSONObject(JSONUtils.DATA_KEY);
                String type = image.getString(JSONUtils.TYPE_KEY);
                if (type.contains("gif")) {
                    this.image = new ImgurMedia(image.getString(JSONUtils.ID_KEY),
                            image.getString(JSONUtils.TITLE_KEY), image.getString(JSONUtils.DESCRIPTION_KEY),
                            "video/mp4", image.getString(JSONUtils.MP4_KEY));
                } else {
                    this.image = new ImgurMedia(image.getString(JSONUtils.ID_KEY),
                            image.getString(JSONUtils.TITLE_KEY), image.getString(JSONUtils.DESCRIPTION_KEY),
                            type, image.getString(JSONUtils.LINK_KEY));
                }
            } catch (JSONException e) {
                e.printStackTrace();
                parseFailed = true;
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (parseFailed) {
                parseImgurImageAsyncTaskListener.failed();
            } else {
                parseImgurImageAsyncTaskListener.success(image);
            }
        }
    }
}