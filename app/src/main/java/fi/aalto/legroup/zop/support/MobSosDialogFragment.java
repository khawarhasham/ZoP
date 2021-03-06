package fi.aalto.legroup.zop.support;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RatingBar;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.rollbar.android.Rollbar;

import java.util.HashMap;
import java.util.Map;

import fi.aalto.legroup.zop.R;
import fi.aalto.legroup.zop.app.App;
import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * TODO: Authentication using OpenID Connect
 * TODO: Don't hard-code the survey and response IDs.
 */
public final class MobSosDialogFragment extends DialogFragment implements Callback<Response>,
        DialogInterface.OnClickListener {

    private final static String RESPONSE_KEY_RATING = "SFQ.OS";
    private final static String RESPONSE_KEY_COMMENTS = "SFQ.C";

    private Activity activity;

    private EditText commentsField;
    private RatingBar ratingBar;

    public static MobSosDialogFragment newInstance() {
        return new MobSosDialogFragment();
    }

    @Override
    @SuppressLint("InflateParams")
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        this.activity = getActivity();

        LayoutInflater inflater = LayoutInflater.from(this.activity);

        // Building dialogs with views is one of the rare cases where null as the root is valid.
        View view = inflater.inflate(R.layout.dialog_mobsos, null);

        this.commentsField = (EditText) view.findViewById(R.id.commentsField);
        this.ratingBar = (RatingBar) view.findViewById(R.id.ratingBar);

        // Don't close the dialog if the user taps the background
        setCancelable(false);

        return new AlertDialog.Builder(this.activity)
                .setTitle(R.string.feedback)
                .setPositiveButton(R.string.feedback_send, this)
                .setNegativeButton(R.string.cancel, null)
                .setView(view)
                .create();
    }

    /**
     * Invoked when a button in the dialog is clicked.
     */
    @Override
    public void onClick(DialogInterface dialog, int which) {
        int rating = this.ratingBar.getNumStars();
        String comments = this.commentsField.getText().toString();

        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                sendResponse(rating, comments);
                break;
        }
    }

    private void sendResponse(int rating, String comments) {
        String servicePath = getString(R.string.mobSosUrl);
        String endpointUrl = App.getLayersServiceUrl(servicePath).toString();

        MobSosService service = new RestAdapter.Builder()
                .setEndpoint(endpointUrl)
                .build()
                .create(MobSosService.class);

        int surveyId = 1;

        Map<String, String> responses = new HashMap<>();

        responses.put(RESPONSE_KEY_RATING, Integer.toString(rating));
        responses.put(RESPONSE_KEY_COMMENTS, comments);

        service.postResponses(surveyId, responses, this);
    }

    @Override
    public void success(Response body, Response response) {
        SnackbarManager.show(Snackbar.with(activity).text(R.string.feedback_sent));
    }

    @Override
    public void failure(RetrofitError error) {
        SnackbarManager.show(Snackbar.with(activity).text(R.string.feedback_error));
        Rollbar.reportException(error);
        error.printStackTrace();
    }

}
