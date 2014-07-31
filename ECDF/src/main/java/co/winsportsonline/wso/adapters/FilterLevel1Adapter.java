package co.winsportsonline.wso.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

import co.winsportsonline.wso.R;
import co.winsportsonline.wso.datamodel.Filter;

/**
 * Created by Franklin Cruz on 27-02-14.
 */
public class FilterLevel1Adapter extends BaseExpandableListAdapter {

    private List<Filter> filters;
    private Context context;
    private LayoutInflater inflater;

    public FilterLevel1Adapter(Context context, List<Filter> objects) {
        this.filters = objects;
        this.context = context;

        this.inflater = LayoutInflater.from(context);
    }


    @Override
    public int getGroupCount() {
        return filters.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return filters.get(groupPosition).getFilters().size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return filters.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return filters.get(groupPosition).getFilters().get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return Long.parseLong(String.valueOf(groupPosition) + String.valueOf(childPosition));
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {

        View group = inflater.inflate(R.layout.filter_level_1_cell, parent, false);

        TextView titleText = (TextView)group.findViewById(R.id.title_label);

        titleText.setText(filters.get(groupPosition).getName());
        Button moreButton = (Button)group.findViewById(R.id.more_button);
        if(isExpanded) {
            moreButton.setText("-");
        }
        else {
            moreButton.setText("+");
        }

        return group;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        View child = inflater.inflate(R.layout.filter_level_2_cell, parent, false);

        TextView titleText = (TextView)child.findViewById(R.id.title_label);

        titleText.setText(filters.get(groupPosition).getFilters().get(childPosition).getName());

        return child;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
