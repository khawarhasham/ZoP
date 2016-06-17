package fi.aalto.legroup.zop.browsing;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.melnykov.fab.ScrollDirectionListener;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import fi.aalto.legroup.zop.R;
import fi.aalto.legroup.zop.app.App;
import fi.aalto.legroup.zop.authoring.QRHelper;
import fi.aalto.legroup.zop.authoring.VideoDeletionFragment;
import fi.aalto.legroup.zop.entities.OptimizedVideo;
import fi.aalto.legroup.zop.playback.PlayerActivity;
import fi.aalto.legroup.zop.sharing.SharingActivity;
import fi.aalto.legroup.zop.storage.local.ExportService;
import fi.aalto.legroup.zop.storage.remote.UploadErrorEvent;
import fi.aalto.legroup.zop.storage.remote.UploadService;
import fi.aalto.legroup.zop.storage.remote.UploadStateEvent;
import fi.aalto.legroup.zop.views.RecyclerItemClickListener;
import fi.aalto.legroup.zop.views.adapters.VideoGridAdapter;
import fi.aalto.legroup.zop.views.utilities.DimensionUnits;
import fi.aalto.legroup.zop.views.utilities.ScrollDirectionListenable;

public final class BrowserFragment extends Fragment implements ActionMode.Callback,
        RecyclerItemClickListener.OnItemClickListener, ScrollDirectionListenable {

    private Bus bus;

    private List<UUID> videos = Collections.emptyList();

    private TextView placeHolder;

    private VideoGridAdapter adapter;
    private ActionMode actionMode;

    @Nullable
    private ScrollDirectionListener scrollListener;

    public static BrowserFragment newInstance(List<UUID> videos) {
        BrowserFragment fragment = new BrowserFragment();

        fragment.setVideos(videos);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // FIXME: Retaining the instance just to keep the videos is a bit questionable
        setRetainInstance(true);

        // TODO: Inject instead
        this.bus = App.bus;
    }

    @Override
    public void onResume() {
        super.onResume();
        bus.register(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedState) {
        return inflater.inflate(R.layout.fragment_browser, parent, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        this.placeHolder = (TextView) view.findViewById(R.id.place_holder);
        RecyclerView grid = (RecyclerView) view.findViewById(R.id.video_list);

        // Default span count is 1
        GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 1);

        this.adapter = new VideoGridAdapter(getActivity(), App.videoInfoRepository);
        this.adapter.registerAdapterDataObserver(new PlaceholderDataObserver());
        this.adapter.setItems(videos);

        grid.setHasFixedSize(true);
        grid.setAdapter(this.adapter);
        grid.setLayoutManager(layoutManager);
        grid.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), this));
        grid.setOnScrollListener(new RecyclerScrollListener());

        // This listener sets the span count on layout and then stops listening.
        new GridOnLayoutChangeListener(grid);
    }

    @Override
    public void onPause() {
        if (this.actionMode != null) {
            this.actionMode.finish();
        }

        bus.unregister(this);

        super.onPause();
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.activity_browser_action_mode, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {

        MenuItem upload_menu_item = menu.findItem(R.id.action_upload);
        MenuItem share_menu_item = menu.findItem(R.id.action_share_to_group);
        if (App.loginManager.isLoggedIn()) {
            upload_menu_item.setEnabled(true);
            share_menu_item.setEnabled(true);
            upload_menu_item.getIcon().setAlpha(255);
            share_menu_item.getIcon().setAlpha(255);
        } else {
            upload_menu_item.setEnabled(false);
            share_menu_item.setEnabled(false);
            upload_menu_item.getIcon().setAlpha(130);
            share_menu_item.getIcon().setAlpha(130);
        }
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_qr_to_video:
                QRHelper.readQRCodeForVideos(getActivity(), getSelection(), mode);
                mode.finish();
                return true;

            case R.id.action_share_video:
                ExportService.export(getActivity(), getSelection());
                mode.finish();
                return true;

            case R.id.action_delete:
                VideoDeletionFragment.newInstance(getSelection())
                        .show(getFragmentManager(), "DeletionFragment");
                mode.finish();
                return true;

            case R.id.action_upload:
                {
                    List<UUID> selection = getSelection();
                    UploadService.upload(getActivity(), selection);
                    mode.finish();
                    return true;
                }
            case R.id.action_share_to_group:
            {
                // do the check again just in case that videos have changed state while the menu
                // has been visible
                boolean hasLocal = false;
                List<UUID> selection = getSelection();
                for (UUID id : selection) {
                    try {
                        OptimizedVideo video = App.videoRepository.getVideo(id);
                        if (video.isLocal()) {
                            hasLocal = true;
                            break;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (!hasLocal) {
                    SharingActivity.openShareActivity(getActivity(), selection);
                }
                mode.finish();
                return true;
            }


            case R.id.action_view_video_info:
                Intent informationIntent = new Intent(getActivity(), DetailActivity.class);
                informationIntent.putExtra(DetailActivity.ARG_VIDEO_ID, getSelection().get(0));
                startActivity(informationIntent);
                mode.finish();
                return true;
        }

        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        this.actionMode = null;
        clearSelection();
    }

    @Override
    public void onItemClick(View childView, int position) {
        if (actionMode == null) {
            showVideo(position);
        } else {
            toggleSelection(position);
        }
    }

    @Override
    public void onItemLongPress(View childView, int position) {
        if (actionMode == null) {
            startActionMode();
        }

        toggleSelection(position);
    }

    /**
     * FIXME: Juggling the videos like this is a bit icky but better than creating an adapter here.
     */
    public void setVideos(List<UUID> videos) {
        this.videos = videos;

        if (this.adapter != null) {
            this.adapter.setItems(videos);
        }
    }

    @Subscribe
    public void onUploadState(UploadStateEvent event) {
        UUID videoId = event.getVideoId();

        switch (event.getType()) {
            case STARTED:
                this.adapter.showProgress(videoId);
                break;

            case SUCCEEDED:
            case FAILED:
                this.adapter.hideProgress(videoId);
                break;
        }
    }

    @Subscribe
    public void onUploadError(UploadErrorEvent event) {
        UUID videoId = event.getVideoId();
        String message = event.getErrorMessage();

        this.adapter.hideProgress(videoId);

        if (message == null) {
            message = getString(R.string.upload_error);
        }

        SnackbarManager.show(Snackbar.with(getActivity()).text(message));
    }

    private List<UUID> getSelection() {
        List<Integer> positions = this.adapter.getSelectedItems();
        List<UUID> items = new ArrayList<>();

        for (int position : positions) {
            UUID item = this.adapter.getItem(position);

            if (item != null) {
                items.add(item);
            }
        }

        return items;
    }

    private void clearSelection() {
        this.adapter.clearSelectedItems();
    }

    private void toggleSelection(int position) {
        this.adapter.toggleSelection(position);

        int count = this.adapter.getSelectedItemCount();

        if (count == 0) {
            this.actionMode.finish();
        } else {
            String title = getResources().getQuantityString(R.plurals.select_count, count, count);
            this.actionMode.setTitle(title);
            updateMenuItems();
        }
    }

    private void updateMenuItems() {
        Menu menu = this.actionMode.getMenu();
        boolean hasLocal = false;
        List<UUID> selection = getSelection();

        for (UUID id : selection) {
            try {
                OptimizedVideo video = App.videoRepository.getVideo(id);
                if (video.isLocal()) {
                    hasLocal = true;
                    break;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        MenuItem upload_menu_item = menu.findItem(R.id.action_upload);
        MenuItem share_menu_item = menu.findItem(R.id.action_share_to_group);
        if (hasLocal) {
            share_menu_item.setVisible(false);
            upload_menu_item.setVisible(true);
        } else {
            share_menu_item.setVisible(true);
            upload_menu_item.setVisible(false);
        }
    }

    private void showVideo(int position) {
        Intent detailIntent = new Intent(getActivity(), PlayerActivity.class);
        UUID id = this.adapter.getItem(position);

        detailIntent.putExtra(PlayerActivity.ARG_VIDEO_ID, id);

        startActivity(detailIntent);
    }

    private void startActionMode() {
        this.actionMode = ((ActionBarActivity) getActivity()).startSupportActionMode(this);
    }

    /**
     * Sets the scroll direction listener.
     *
     * @param listener Scroll direction listener, or null to remove a previously set listener.
     */
    @Override
    public void setScrollDirectionListener(@Nullable ScrollDirectionListener listener) {
        this.scrollListener = listener;
    }

    /**
     * Shows or hides the placeholder text appropriately when the adapter items change.
     */
    private class PlaceholderDataObserver extends RecyclerView.AdapterDataObserver {

        @Override
        public void onChanged() {
            super.onChanged();

            int itemCount = BrowserFragment.this.adapter.getItemCount();
            View placeHolder = BrowserFragment.this.placeHolder;

            if (itemCount == 0) {
                placeHolder.setVisibility(View.VISIBLE);
            } else {
                placeHolder.setVisibility(View.GONE);
            }
        }

    }

    /**
     * Calculates the number of columns when the grid's layout bounds change.
     */
    private static class GridOnLayoutChangeListener
            implements ViewTreeObserver.OnGlobalLayoutListener {

        private static final int MINIMUM_ITEM_WIDTH_DP = 250;

        private RecyclerView grid;

        private GridOnLayoutChangeListener(RecyclerView grid) {
            this.grid = grid;
            grid.getViewTreeObserver().addOnGlobalLayoutListener(this);
        }

        @Override
        public void onGlobalLayout() {
            int gridWidth = grid.getWidth();
            float itemWidth = DimensionUnits.dpToPx(grid.getContext(), MINIMUM_ITEM_WIDTH_DP);

            int spanCount = (int) Math.floor(gridWidth / itemWidth);

            if (spanCount < 1) {
                spanCount = 1;
            }

            RecyclerView.LayoutManager layoutManager = grid.getLayoutManager();

            if (layoutManager instanceof GridLayoutManager) {
                ((GridLayoutManager) layoutManager).setSpanCount(spanCount);

                // Workaround for an upstream GridLayoutManager issue:
                // https://code.google.com/p/android/issues/detail?id=93710
                grid.requestLayout();
            }

            // This listener needs to be removed to avoid an infinite loop due to the workaround.
            grid.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        }

    }

    private class RecyclerScrollListener extends RecyclerView.OnScrollListener {

        private static final int THRESHOLD = 100;

        private int scrolledDistance = 0;

        @Override
        public void onScrolled(RecyclerView recyclerView, int deltaX, int deltaY) {
            if (scrollListener == null) {
                return;
            }

            scrolledDistance += deltaY;

            if (Math.abs(scrolledDistance) < THRESHOLD) {
                return;
            }

            if (scrolledDistance > 0) {
                scrollListener.onScrollDown();
            } else {
                scrollListener.onScrollUp();
            }

            scrolledDistance = 0;
        }

    }

}
