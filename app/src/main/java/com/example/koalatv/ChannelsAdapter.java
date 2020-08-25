package com.example.koalatv;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;


public class ChannelsAdapter extends BaseAdapter implements Filterable {

    private Context mContext;
    public List<Channel> channelsList;
    private List<Channel> channelsListFiltered;


    public ChannelsAdapter(Context context, ArrayList<Channel> list) {
        this.mContext = context;
        this.channelsList = list;
        this.channelsListFiltered = list;
    }

    @Override
    public int getCount() {
        return channelsList.size();
    }

    @Override
    public Object getItem(int i) {
        return channelsList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return  i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View row = inflater.inflate(R.layout.row_data, null);

        ImageView image = (ImageView) row.findViewById(R.id.imageView_poster);
        Glide.with(mContext).load(channelsList.get(position).getIcon()).into(image);

        TextView name = (TextView) row.findViewById(R.id.textView_name);
        name.setText(channelsList.get(position).getChannelName());

        return row;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                channelsList = channelsListFiltered;
                FilterResults results = new FilterResults();
                String charString = charSequence.toString();
                if (!charString.isEmpty()) {
                    ArrayList<Channel> filters = new ArrayList<>();
                    for (Channel row : channelsList) {
                        if (row.getChannelName().toLowerCase().contains(charString.toLowerCase())) {
                            filters.add(row);
                        }
                    }
                    results.count = filters.size();
                    results.values = filters;
                }
                else {
                    results.count = channelsListFiltered.size();
                    results.values = channelsListFiltered;
                }
                return results;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                channelsList = (ArrayList<Channel>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }
}