package co.winsportsonline.wso.fragments;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.androidquery.AQuery;

import co.winsportsonline.wso.R;
import co.winsportsonline.wso.datamodel.LiveStreamSchedule;

/**
 * Created by Franklin Cruz on 07-03-14.
 */
public class ShareFragment extends Fragment {

    private LiveStreamSchedule schedule;
    private View view;

    public ShareFragment() {

    }

    public ShareFragment(LiveStreamSchedule schedule) {
        this.schedule = schedule;
    }

    public void setSchedule() {
        if (view != null) {
            displayScheduleData();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.share_fragment, container, false);
        Typeface light = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Oswald-Light.otf");

        TextView t = (TextView) rootView.findViewById(R.id.share_text);
        t.setTypeface(light);

        this.view = rootView;

        if(schedule != null) {
            displayScheduleData();
        }

        return rootView;
    }


    private void displayScheduleData() {
        String[] splited = schedule.getCode().split("_vs_");
        AQuery aq = new AQuery(view);
        if (splited.length > 1) {
            View imageFull = view.findViewById(R.id.image_full);
            imageFull.setVisibility(View.GONE);

            View splitView = view.findViewById(R.id.split_image_container);
            splitView.setVisibility(View.VISIBLE);

            View vsLabel = view.findViewById(R.id.vs_label);
            vsLabel.setVisibility(View.VISIBLE);

            aq.id(R.id.image_left).image(String.format(LiveFragment.LIVE_THUMBNAIL_LOCAL_IMAGE_URL, splited[0]));
            aq.id(R.id.image_right).image(String.format(LiveFragment.LIVE_THUMBNAIL_VISIT_IMAGE_URL, splited[1]));
        }
        else {
            View imageFull = view.findViewById(R.id.image_full);
            imageFull.setVisibility(View.VISIBLE);

            View splitView = view.findViewById(R.id.split_image_container);
            splitView.setVisibility(View.GONE);

            View vsLabel = view.findViewById(R.id.vs_label);
            vsLabel.setVisibility(View.GONE);

            aq.id(R.id.image_full).image(String.format(LiveFragment.LIVE_LEFT_HEADER_URL_FORMATSTR, splited[0]));
        }
    }
}
