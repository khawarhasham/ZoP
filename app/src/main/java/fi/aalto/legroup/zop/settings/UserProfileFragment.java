package fi.aalto.legroup.zop.settings;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.repackaged.com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.List;

import fi.aalto.legroup.zop.R;
import fi.aalto.legroup.zop.app.App;
import fi.aalto.legroup.zop.views.adapters.SSSEvent;
import fi.aalto.legroup.zop.views.adapters.SSSEventType;
import fi.aalto.legroup.zop.views.adapters.TagAction;


public final class UserProfileFragment extends Fragment implements SSSEvent {

    private List<String> interestTags;
    ArrayAdapter<String> tags;
    ProgressDialog progressDialog;
    private ListView interestList;

    public UserProfileFragment(){
        interestTags = new ArrayList<String>();
        /*interestTags.add("Distributed systems");
        interestTags.add("Cloud Computing");
        interestTags.add("Grid Computing");
        interestTags.add("Software Development");
        */
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //return super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_userprofile_layout, container, false);
        TextView userName = (TextView)view.findViewById(R.id.usernameText);
        TextView userEmail = (TextView)view.findViewById(R.id.userEmailText);
        TextView preferredName = (TextView)view.findViewById(R.id.preferredName);
        final EditText tagEdit = (EditText)view.findViewById(R.id.tagEdit);

        Button addTag = (Button)view.findViewById(R.id.addTag);
        addTag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String tagLabel = tagEdit.getText().toString();
                if (!Strings.isNullOrEmpty(tagLabel)){
                    tagLabel = tagLabel.trim();
                    if ( tagLabel.length() > 0){
                        //now add this tag value whatever it is
                        //using App User object for sssid, assuming that it has been set by the user tags call.
                        new TagAction(UserProfileFragment.this,
                                SSSEventType.Type.AddTag).execute(App.loginManager.getUser().getId(),
                                                                    tagLabel);
                    }else{
                        Toast.makeText(getActivity(), "Interest can not be empty.", Toast.LENGTH_LONG).show();
                    }
                }else{
                    Toast.makeText(getActivity(), "Interest can not be empty.", Toast.LENGTH_LONG).show();
                }
            }
        });

        TextView userInterests = (TextView)view.findViewById(R.id.userInterests);
        final ImageView imgIcon = (ImageView)view.findViewById(R.id.dropdownIcon);

        new TagAction(this, SSSEventType.Type.GetTag).execute(App.loginManager.getUserInfo().get("email").getAsString(),
                App.loginManager.getUserInfo().get("sub").getAsString());

        interestList = (ListView)view.findViewById(R.id.userInterestsList);
        //final ImageView imgView = (ImageView)view.findViewById(R.id.dropdownImg);

        tags = new ArrayAdapter<String>(view.getContext(), R.layout.interest_list, interestTags);
        interestList.setAdapter(tags);

        //set listview height for 5 items
        /*LinearLayout.LayoutParams lstParams = (LinearLayout.LayoutParams)interestList.getLayoutParams();
        View interestView = View.inflate(getActivity(), R.layout.interest_list, null);
        TextView listItem = (TextView) interestView.findViewById(R.id.interestItem);
        final float scale = this.getResources().getDisplayMetrics().density;
        int pixels = (int) ( Integer.parseInt(listItem.getTag().toString() ) * scale + 0.5f);
        int listHeigt = pixels  * 5 + 5;
        lstParams.height = listHeigt;
        */

        userInterests.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.out.println("toggle interests");

                if (interestList.getVisibility() == View.GONE) {
                    //make it appear again
                    interestList.setVisibility(View.VISIBLE);
                    //also update the imageview dropdown icon
                    int resid = getResources().getIdentifier("@android:drawable/arrow_up_float", null, null);
                    imgIcon.setImageResource(resid);

                } else {
                    interestList.setVisibility(View.GONE);
                    int resid = getResources().getIdentifier("@android:drawable/arrow_down_float", null, null);
                    imgIcon.setImageResource(resid);
                }
            }
        });

        userName.setText(App.loginManager.getUserInfo().get("name").getAsString());
        userEmail.setText(App.loginManager.getUserInfo().get("email").getAsString());
        preferredName.setText(App.loginManager.getUserInfo().get("preferred_username").getAsString());
        //given_name, family_name

        return view;
    }

    public void addInterest(String interest){
        this.interestTags.add(interest);
        tags.notifyDataSetChanged();
    }

    @Override
    public void processEvent(ArrayList<?> result) {

        if ( result == null ){
            //operation unsuccessful.
            Toast.makeText(getActivity(), "Operation Unsuccessful. Try again.", Toast.LENGTH_LONG).show();
        }else{
            Toast.makeText(getActivity(), "Operation Successful.", Toast.LENGTH_LONG).show();
            interestList.setVisibility(View.VISIBLE);
        }
        this.interestTags.addAll((ArrayList<String>)result);
        this.tags.notifyDataSetChanged();
        System.out.println("processEvent()");
    }

    @Override
    public void startEvent() {
        progressDialog = ProgressDialog.show(this.getActivity(), "User Profile", "Loading User data", true);
    }

    @Override
    public void stopEvent() {
        progressDialog.dismiss();
    }


}
