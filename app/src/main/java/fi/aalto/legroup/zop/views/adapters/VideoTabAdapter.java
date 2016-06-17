package fi.aalto.legroup.zop.views.adapters;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import com.melnykov.fab.ScrollDirectionListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import fi.aalto.legroup.zop.R;
import fi.aalto.legroup.zop.app.App;
import fi.aalto.legroup.zop.browsing.BrowserFragment;
import fi.aalto.legroup.zop.entities.Group;
import fi.aalto.legroup.zop.entities.OptimizedVideo;
import fi.aalto.legroup.zop.storage.VideoRepository;
import fi.aalto.legroup.zop.views.utilities.ScrollDirectionListenable;

public final class VideoTabAdapter extends FragmentStatePagerAdapter implements
        ScrollDirectionListenable {

    private Map<Integer, Object> activeItems = new HashMap<>();
    private List<String> tabNames = new ArrayList<>();
    private String allVideosText;
    private List<List<UUID>> tabVideoIds;

    @Nullable
    private ScrollDirectionListener scrollListener;

    @Nullable
    private ScrollDirectionListenable scrollDelegate;

    public VideoTabAdapter(Context context, FragmentManager manager) {
        super(manager);

        allVideosText = context.getString(R.string.my_videos);

        tabNames = new ArrayList<>();
        tabNames.add(allVideosText);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        if (position < tabNames.size()) {
            return tabNames.get(position);
        }

        return String.valueOf(position);
    }

    @Override
    public Fragment getItem(int position) {
        List<UUID> videos = getVideosForPosition(position);
        return BrowserFragment.newInstance(videos);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Object item = super.instantiateItem(container, position);

        activeItems.put(position, item);

        return item;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object item) {
        activeItems.remove(position);
        super.destroyItem(container, position, item);
    }

    /**
     * Forward the scroll direction listener to the currently active item.
     */
    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        if (scrollDelegate != null) {
            scrollDelegate.setScrollDirectionListener(null);
        }

        if (object instanceof ScrollDirectionListenable) {
            scrollDelegate = ((ScrollDirectionListenable) object);
            scrollDelegate.setScrollDirectionListener(scrollListener);
        }

        // The new fragment is scrolled to the top
        if (scrollListener != null) {
            scrollListener.onScrollUp();
        }

        super.setPrimaryItem(container, position, object);
    }

    @Override
    public int getCount() {
        return tabNames.size();
    }

    private static List<UUID> toIds(Collection<OptimizedVideo> videos) {
        List<UUID> list = new ArrayList<>(videos.size());
        for (OptimizedVideo video : videos) {
            list.add(video.getId());
        }
        return list;
    }

    @Override
    public void notifyDataSetChanged() {

        // Fetch the videos here
        List<OptimizedVideo> allVideos;
        List<Group> allGroups;

        try {
            allVideos = new ArrayList<>(App.videoInfoRepository.getAll());
            allGroups = new ArrayList<>(App.videoInfoRepository.getGroups());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }


        List<String> newTabNames = new ArrayList<String>();
        newTabNames.add(allVideosText);
        for (Group group : allGroups) {
            // TODO: Truncate.
            newTabNames.add(group.getName());
        }

        List<List<UUID>> newTabVideoIds = new ArrayList<>(newTabNames.size());

        // First tab has all the videos
        newTabVideoIds.add(toIds(allVideos));

        for (int i = 0; i < allGroups.size(); i++) {
            newTabVideoIds.add(allGroups.get(i).getVideos());
        }

        Comparator<OptimizedVideo> videoComparator = Collections.reverseOrder(
                new OptimizedVideo.CreateTimeComparator());
        Comparator<UUID> idComparator = new VideoIDComparator(App.videoRepository, videoComparator);
        for (int i = 0; i < newTabVideoIds.size(); i++) {
            Collections.sort(newTabVideoIds.get(i), idComparator);
        }

        tabNames = newTabNames;
        tabVideoIds = newTabVideoIds;

        for (Map.Entry<Integer, Object> entry : activeItems.entrySet()) {
            Object item = entry.getValue();

            if (item instanceof BrowserFragment) {
                int position = entry.getKey();
                List<UUID> videos = getVideosForPosition(position);

                ((BrowserFragment) item).setVideos(videos);
            }
        }

        super.notifyDataSetChanged();
    }

    private List<UUID> getVideosForPosition(int position) {
        if (tabVideoIds == null || position >= tabVideoIds.size()) {
            return Collections.emptyList();
        }
        return tabVideoIds.get(position);
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

    private static class VideoIDComparator implements Comparator<UUID> {

        private final VideoRepository videoRepository;
        private final Comparator<OptimizedVideo> comparator;

        public VideoIDComparator(VideoRepository videoRepository, Comparator<OptimizedVideo> comparator) {
            this.videoRepository = videoRepository;
            this.comparator = comparator;
        }

        @Override
        public int compare(UUID lhs, UUID rhs) {
            OptimizedVideo a = null, b = null;
            try {
                a = videoRepository.getVideo(lhs);
            } catch (IOException ignored) {
            }
            try {
                b = videoRepository.getVideo(rhs);
            } catch (IOException ignored) {
            }

            if (a == null && b == null) {
                return 0;
            } else if (a == null) {
                return 1;
            } else if (b == null) {
                return -1;
            }

            return comparator.compare(a, b);
        }
    }
}
