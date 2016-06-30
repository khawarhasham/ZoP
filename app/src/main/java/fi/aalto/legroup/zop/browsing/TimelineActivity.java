package fi.aalto.legroup.zop.browsing;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;

import java.io.IOException;
import java.util.UUID;

import fi.aalto.legroup.zop.R;
import fi.aalto.legroup.zop.app.App;
import fi.aalto.legroup.zop.entities.Video;

/**
 * Created by khawar on 20/06/2016.
 */
public class TimelineActivity extends FragmentActivity {

    public static final String TIMELINE_VIDEO_ID = "TIMELINE_VIDEO_ID";
    private Video video;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        UUID videoId = (UUID) getIntent().getSerializableExtra(TIMELINE_VIDEO_ID);

        try {
            video = App.videoRepository.getVideo(videoId).inflate();
        } catch (IOException e) {
            e.printStackTrace();
            SnackbarManager.show(Snackbar.with(this).text(R.string.storage_error));
            finish();
            return;
        }

        setContentView(R.layout.video_timeline);
        ActionBar bar = getActionBar();

        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }
    }
}
