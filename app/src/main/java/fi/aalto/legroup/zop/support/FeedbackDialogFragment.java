package fi.aalto.legroup.zop.support;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;

import java.util.HashMap;
import java.util.Map;

import fi.aalto.legroup.zop.R;
import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;

public final class FeedbackDialogFragment extends DialogFragment implements Callback<String> {

    public final static String ARG_EMAIL = "email";
    public final static String ARG_NAME = "name";

    private Activity activity;

    private TextView bodyText;
    private TextView summaryText;
    private TextView userText;
    private TextView emailText;

    /**
     * Builds a new instance of this fragment.
     *
     * @param name  Name that should be pre-filled.
     * @param email Email address that should be pre-filled.
     */
    public static FeedbackDialogFragment newInstance(String name, String email) {
        FeedbackDialogFragment fragment = new FeedbackDialogFragment();
        Bundle args = new Bundle();

        args.putString(ARG_EMAIL, email);
        args.putString(ARG_NAME, name);

        fragment.setArguments(args);

        return fragment;
    }

    @Override
    @SuppressLint("InflateParams")
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        this.activity = getActivity();

        LayoutInflater inflater = LayoutInflater.from(this.activity);

        // Building dialogs with views is one of the rare cases where null as the root is valid.
        View view = inflater.inflate(R.layout.fragment_feedback, null);

        this.bodyText = (TextView) view.findViewById(R.id.feedback_body);
        this.summaryText = (TextView) view.findViewById(R.id.feedback_summary);
        this.userText = (TextView) view.findViewById(R.id.feedback_user);
        this.emailText = (TextView) view.findViewById(R.id.feedback_email);

        // Pre-fill email and name fields
        String email = getArguments().getString(ARG_EMAIL);
        this.emailText.setText(email);

        String name = getArguments().getString(ARG_NAME);
        this.userText.setText(name);

        // Don't close the dialog if the user taps the background
        setCancelable(false);

        return new MaterialDialog.Builder(this.activity)
                .title(R.string.feedback)
                .positiveText(R.string.feedback_send)
                .negativeText(R.string.cancel)
                .callback(new ButtonCallback())
                .customView(view, true)
                .build();
    }

    /**
     * Sends the contents to OsTicket.
     */
    private void sendFeedback() {
        String endPoint = getString(R.string.feedbackServerUrl);
        String apiKey = getString(R.string.feedbackServerKey);

        new RestAdapter.Builder()
                .setEndpoint(endPoint)
                .build()
                .create(OsTicketService.class)
                .sendFeedback(apiKey, buildBody(), this);
    }

    /**
     * Builds the request body.
     */
    private Map<String, String> buildBody() {
        Map<String, String> map = new HashMap<>();

        map.put("message", this.bodyText.getText().toString());
        map.put("subject", this.summaryText.getText().toString());
        map.put("name", this.userText.getText().toString());
        map.put("email", this.emailText.getText().toString());

        // Some IP address needs to be sent, doesn't have to be real.
        map.put("ip", "10.0.0.0");
        map.put("source", "Android");

        return map;
    }

    @Override
    public void success(String body, Response response) {
        SnackbarManager.show(Snackbar.with(activity).text(R.string.feedback_sent));
        dismiss();
    }

    @Override
    public void failure(RetrofitError error) {
        error.printStackTrace();
        SnackbarManager.show(Snackbar.with(activity).text(R.string.feedback_error));
    }

    private class ButtonCallback extends MaterialDialog.ButtonCallback {

        /**
         * Called when the positive button is tapped.
         */
        @Override
        public void onPositive(MaterialDialog dialog) {
            sendFeedback();
        }

    }

}
