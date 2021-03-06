package fi.aalto.legroup.zop.browsing;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;

import fi.aalto.legroup.zop.R;
import fi.aalto.legroup.zop.app.App;
import fi.aalto.legroup.zop.entities.OptimizedVideo;

public final class SearchActivity extends ActionBarActivity {

    private static final String STATE_MATCHES = "STATE_MATCHES";

    private BrowserFragment browserFragment;
    private ArrayList<UUID> matches = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_search);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        this.browserFragment = (BrowserFragment)
                getSupportFragmentManager().findFragmentById(R.id.fragment_search_video);

        if (savedInstanceState == null) {
            handleIntent(getIntent());
        } else {
            restoreSavedState(savedInstanceState);
        }
    }

    @Override
    protected void onSaveInstanceState(@Nonnull Bundle savedInstanceState) {
        ArrayList<ParcelUuid> parcelableMatches = new ArrayList<>();

        for (UUID match : this.matches) {
            parcelableMatches.add(new ParcelUuid(match));
        }

        savedInstanceState.putParcelableArrayList(STATE_MATCHES, parcelableMatches);

        super.onSaveInstanceState(savedInstanceState);
    }

    private void restoreSavedState(@Nonnull Bundle savedInstanceState) {
        ArrayList<ParcelUuid> parcelableMatches =
                savedInstanceState.getParcelableArrayList(STATE_MATCHES);

        for (ParcelUuid match : parcelableMatches) {
            this.matches.add(match.getUuid());
        }

        this.browserFragment.setVideos(this.matches);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_search, menu);

        SearchManager manager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        String query = getIntent().getStringExtra(SearchManager.QUERY);

        MenuItem item = menu.findItem(R.id.action_search);
        SearchView view = (SearchView) item.getActionView();

        view.setSearchableInfo(manager.getSearchableInfo(getComponentName()));
        view.setQuery(query, false);
        view.setIconifiedByDefault(false);

        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        this.matches.clear();
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();

        switch (action) {
            case Intent.ACTION_SEARCH:
                String query = intent.getStringExtra(SearchManager.QUERY).toLowerCase();
                queryVideos(query);
                break;
        }
    }

    /**
     * Searches all videos for a match against the given query.
     */
    private void queryVideos(String query) {
        Collection<OptimizedVideo> allVideos;

        try {
            allVideos = App.videoInfoRepository.getAll();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        List<OptimizedVideo> matching = new ArrayList<>(allVideos.size());
        for (OptimizedVideo video : allVideos) {
            if (isMatch(video, query)) {
                matching.add(video);
            }
        }

        // TODO: Better sorting?
        Collections.sort(matching,
                Collections.reverseOrder(new OptimizedVideo.CreateTimeComparator()));

        // TODO: General toIds?
        for (OptimizedVideo match : matching) {
            this.matches.add(match.getId());
        }
        this.browserFragment.setVideos(this.matches);
    }

    /**
     * Returns if a video matches the given query. Tries to be as memory-efficient as possible by
     * checking cached information objects for matches first before loading entire videos and their
     * annotations.
     */
    private boolean isMatch(OptimizedVideo video, String query) {

        if (query.equals(video.getTag())) {
            return true;
        }

        if (video.getTitle().toLowerCase().contains(query)) {
            return true;
        }

        for (int i = 0; i < video.getAnnotationCount(); i++) {
            if (video.getAnnotationText(i).toLowerCase().contains(query)) {
                return true;
            }
        }

        return false;
    }

}
