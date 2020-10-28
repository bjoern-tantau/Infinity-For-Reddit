package ml.docilealligator.infinityforreddit.Activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import ml.docilealligator.infinityforreddit.Adapter.SubredditFilterRecyclerViewAdapter;
import ml.docilealligator.infinityforreddit.CustomTheme.CustomThemeWrapper;
import ml.docilealligator.infinityforreddit.Infinity;
import ml.docilealligator.infinityforreddit.R;
import ml.docilealligator.infinityforreddit.RedditDataRoomDatabase;
import ml.docilealligator.infinityforreddit.SubredditFilter.DeleteSubredditFilter;
import ml.docilealligator.infinityforreddit.SubredditFilter.InsertSubredditFilter;
import ml.docilealligator.infinityforreddit.SubredditFilter.SubredditFilter;
import ml.docilealligator.infinityforreddit.SubredditFilter.SubredditFilterViewModel;

public class SubredditFilterPopularAndAllActivity extends BaseActivity {

    @BindView(R.id.coordinator_layout_subreddit_filter_popular_and_all_activity)
    CoordinatorLayout coordinatorLayout;
    @BindView(R.id.appbar_layout_subreddit_filter_popular_and_all_activity)
    AppBarLayout appBarLayout;
    @BindView(R.id.toolbar_subreddit_filter_popular_and_all_activity)
    Toolbar toolbar;
    @BindView(R.id.recycler_view_subreddit_filter_popular_and_all_activity)
    RecyclerView recyclerView;
    @BindView(R.id.fab_subreddit_filter_popular_and_all_activity)
    FloatingActionButton fab;
    @Inject
    @Named("default")
    SharedPreferences sharedPreferences;
    @Inject
    RedditDataRoomDatabase redditDataRoomDatabase;
    @Inject
    CustomThemeWrapper customThemeWrapper;
    SubredditFilterViewModel subredditFilterViewModel;
    private SubredditFilterRecyclerViewAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((Infinity) getApplication()).getAppComponent().inject(this);

        setImmersiveModeNotApplicable();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_subreddit_filter_popular_and_all);

        ButterKnife.bind(this);

        applyCustomTheme();

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        adapter = new SubredditFilterRecyclerViewAdapter(subredditFilter -> DeleteSubredditFilter.deleteSubredditFilter(redditDataRoomDatabase, subredditFilter, () -> {}));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    fab.hide();
                } else if (dy < 0) {
                    fab.show();
                }
            }
        });

        subredditFilterViewModel = new ViewModelProvider(this,
                new SubredditFilterViewModel.Factory(redditDataRoomDatabase))
                .get(SubredditFilterViewModel.class);
        subredditFilterViewModel.getSubredditFilterLiveData().observe(this, subredditFilters -> adapter.updateSubredditsName(subredditFilters));

        fab.setOnClickListener(view -> {
            EditText thingEditText = (EditText) getLayoutInflater().inflate(R.layout.dialog_go_to_thing_edit_text, null);
            thingEditText.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            }
            new MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialogTheme)
                    .setTitle(R.string.choose_a_subreddit)
                    .setView(thingEditText)
                    .setPositiveButton(R.string.ok, (dialogInterface, i)
                            -> {
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(thingEditText.getWindowToken(), 0);
                        }
                        SubredditFilter subredditFilter = new SubredditFilter(thingEditText.getText().toString(), SubredditFilter.TYPE_POPULAR_AND_ALL);
                        InsertSubredditFilter.insertSubredditFilter(redditDataRoomDatabase, subredditFilter,
                                () -> {});
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .setNeutralButton(R.string.search, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (imm != null) {
                                imm.hideSoftInputFromWindow(thingEditText.getWindowToken(), 0);
                            }
                        }
                    })
                    .setOnDismissListener(dialogInterface -> {
                        if (imm != null) {
                            imm.hideSoftInputFromWindow(thingEditText.getWindowToken(), 0);
                        }
                    })
                    .show();
        });
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
    protected SharedPreferences getDefaultSharedPreferences() {
        return sharedPreferences;
    }

    @Override
    protected CustomThemeWrapper getCustomThemeWrapper() {
        return customThemeWrapper;
    }

    @Override
    protected void applyCustomTheme() {
        applyAppBarLayoutAndToolbarTheme(appBarLayout, toolbar);
    }
}