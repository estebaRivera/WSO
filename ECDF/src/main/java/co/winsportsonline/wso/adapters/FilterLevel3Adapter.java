package co.winsportsonline.wso.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.androidquery.AQuery;

import java.util.List;

import co.winsportsonline.wso.R;
import co.winsportsonline.wso.datamodel.Filter;

/**
 * Created by Franklin Cruz on 27-02-14.
 */
public class FilterLevel3Adapter extends ArrayAdapter<Filter> {

    private List<Filter> filters;
    private LayoutInflater inflater;

    public FilterLevel3Adapter(Context context, List<Filter> objects) {
        super(context, R.layout.filter_level_3_cell, objects);
        this.filters = objects;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View child = inflater.inflate(R.layout.filter_level_3_cell, parent, false);

        TextView titleText = (TextView)child.findViewById(R.id.title_label);
        titleText.setText(filters.get(position).getName());

        AQuery aq = new AQuery(child);
        aq.id(R.id.icon_image).image(filters.get(position).getImage());

        return child;
    }
}
