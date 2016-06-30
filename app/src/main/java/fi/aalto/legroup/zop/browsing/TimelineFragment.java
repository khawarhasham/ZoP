package fi.aalto.legroup.zop.browsing;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.api.client.util.Strings;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;

import fi.aalto.legroup.zop.R;
import fi.aalto.legroup.zop.entities.Video;

public final class TimelineFragment extends Fragment {

    private TextView user;
    private TextView creatorField;
    private TextView qrCodeField;
    private TextView uploadedField;

    private Video video;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_information, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //user = (TextView) view.findViewById(R.id.annotate_user);
        creatorField = (TextView) view.findViewById(R.id.creatorField);
        qrCodeField = (TextView) view.findViewById(R.id.qrCodeField);
        uploadedField = (TextView) view.findViewById(R.id.uploadedField);

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        video = ((DetailActivity) getActivity()).getVideo();

        populateInformation();

    }

    private void populateInformation() {
        String authorName = video.getAuthor().getName();
        String qrCode = video.getId().toString();
        String uploaded;

        if (Strings.isNullOrEmpty(authorName)) {
            authorName = getString(R.string.semanticvideo_unknown_creator);
        }

        if (Strings.isNullOrEmpty(qrCode)) {
            qrCode = getString(R.string.semanticvideo_no_qr);
        }

        if (video.isRemote()) {
            uploaded = getString(R.string.yes);
        } else {
            uploaded = getString(R.string.no);
        }

        //titleField.setText(video.getTitle());
        creatorField.setText(authorName);
        qrCodeField.setText(qrCode);
        uploadedField.setText(uploaded);
    }

    private void showTitleEditDialog() {
        final EditText text = new EditText(getActivity());

        text.setSingleLine();
        text.setText(video.getTitle());

        new AlertDialog.Builder(getActivity())
                .setView(text)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        String title = text.getText().toString();

                        if (Strings.isNullOrEmpty(title)) return;

                        video.setTitle(title);

                        if (!video.save()) {
                            SnackbarManager.show(
                                    Snackbar.with(getActivity()).text(R.string.storage_error));
                        }

                        populateInformation();
                    }
                })
                .show();
    }

    private void showGenreEditDialog() {}

}
