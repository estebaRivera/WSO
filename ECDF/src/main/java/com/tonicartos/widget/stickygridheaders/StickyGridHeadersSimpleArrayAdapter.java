/*
 Copyright 2013 Tonic Artos

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package com.tonicartos.widget.stickygridheaders;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

import co.winsportsonline.wso.R;
import co.winsportsonline.wso.datamodel.Media;

/**
 * @author Tonic Artos
 * @param <T>
 */
public class StickyGridHeadersSimpleArrayAdapter extends BaseAdapter implements
        StickyGridHeadersSimpleAdapter {
    protected static final String TAG = StickyGridHeadersSimpleArrayAdapter.class.getSimpleName();

    protected int mHeaderResId;

    protected LayoutInflater mInflater;

    protected int mItemResId;

    private List<Media> mItems;

    public StickyGridHeadersSimpleArrayAdapter(Context context, List<Media> items, int headerResId,
            int itemResId) {
        init(context, items, headerResId, itemResId);
    }

    public StickyGridHeadersSimpleArrayAdapter(Context context, Media[] items, int headerResId,
            int itemResId) {
        init(context, Arrays.asList(items), headerResId, itemResId);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public long getHeaderId(int position) {
        Media item = getItem(position);
        CharSequence value;
        if (item instanceof CharSequence) {
            value = (CharSequence)item;
        } else {
            value = item.toString();
        }

        return value.subSequence(0, 1).charAt(0);
    }

    @Override
    @SuppressWarnings("unchecked")
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        HeaderViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(mHeaderResId, parent, false);
            holder = new HeaderViewHolder();
            holder.textView = (TextView)convertView.findViewById(android.R.id.text1);
            convertView.setTag(holder);
        } else {
            holder = (HeaderViewHolder)convertView.getTag();
        }

        Media item = getItem(position);
        CharSequence string;
        if (item instanceof CharSequence) {
            string = (CharSequence)item;
        } else {
            string = item.toString();
        }

        // set header text as first char in string
        holder.textView.setText(string.subSequence(0, 1));

        return convertView;
    }

    @Override
    public Media getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    @SuppressWarnings("unchecked")
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(mItemResId, parent, false);
            holder = new ViewHolder();

            holder.titleTextView = (TextView)convertView.findViewById(R.id.title_label);
            holder.timeTextView = (TextView)convertView.findViewById(R.id.time_label);
            holder.shareButton = (ImageButton)convertView.findViewById(R.id.share_button);
            holder.previewImage = (ImageView)convertView.findViewById(R.id.preview_image);
            holder.shareView = convertView.findViewById(R.id.share_container);
            holder.showView = convertView.findViewById(R.id.show_container);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder)convertView.getTag();
        }

        Media item = getItem(position);

        holder.titleTextView.setText(item.getTitle().replace(" - ", "\n"));

        return convertView;
    }

    private void init(Context context, List<Media> items, int headerResId, int itemResId) {
        this.mItems = items;
        this.mHeaderResId = headerResId;
        this.mItemResId = itemResId;
        mInflater = LayoutInflater.from(context);
    }

    public class HeaderViewHolder {
        public TextView textView;
    }

    public class ViewHolder {
        public TextView titleTextView;
        public TextView timeTextView;
        public ImageView previewImage;
        public ImageButton shareButton;
        public TextView compartir;

        public View shareView;
        public View showView;

    }
}
