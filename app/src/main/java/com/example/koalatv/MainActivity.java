package com.example.koalatv;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;


// Casting Wire Up
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.toolbox.JsonArrayRequest;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.CastStateListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private AppCompatButton playButton;
    private CastContext castContext;
    private MenuItem mMediaRouteMenuItem;

    private String jsonURL = "http://147.135.114.195/fubo.json";
    private Button button;

    private ArrayList<Channel> channelsList;
    private ChannelsAdapter mAdapter;
    private SearchView searchView;

    ListView lv;
    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        castContext = CastContext.getSharedInstance(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        Button to remove
//        playButton = findViewById(R.ooid.playButton);
//        playButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                switchActivities();
//            }
//        });

        lv= (ListView) findViewById(R.id.lv);
        channelsList = new ArrayList<>();
        mAdapter = new ChannelsAdapter(getApplicationContext(), channelsList);

        lv.setOnItemClickListener((adapterView, view, i, l) -> {
            Toast.makeText(getApplicationContext(), mAdapter.channelsList.get(i).getChannelName(), Toast.LENGTH_SHORT).show();

//            Intent intent = new  Intent(MainActivity.this, VideoActivity.class);
//            intent.putExtra("channelHeader", mAdapter.channelsList.get(i).getChannelName());
//            intent.putExtra(VideoActivity.ARG_VIDEO_URL,mAdapter.channelsList.get(i).getStreamUrl().toString());
//            startActivity(intent);

//            switchActivities();

            Intent intent = new Intent(this, PlayerActivity.class);
            intent.putExtra(VideoActivity.ARG_VIDEO_URL,mAdapter.channelsList.get(i).getStreamUrl().toString());
            startActivity(intent);
        });

        lv.setAdapter(mAdapter);
        fetchChannels();

    }

    private void fetchChannels() {

        ProgressBar mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mProgressBar.setVisibility(View.VISIBLE);
        JsonArrayRequest request = new JsonArrayRequest(jsonURL,
                response -> {
                    if (response == null) {
                        Toast.makeText(getApplicationContext(), "Couldn't fetch the contacts! Pleas try again.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    List<Channel> items = new Gson().fromJson(response.toString(), new TypeToken<List<Channel>>() {}.getType());
                    // adding contacts to contacts list
                    channelsList.clear();
                    channelsList.addAll(items);
                    mProgressBar.setVisibility(View.INVISIBLE);

                    // refreshing recycler view
                    mAdapter.notifyDataSetChanged();
                }, error -> {
            // error in getting json
            Log.e(TAG, "Error: " + error.getMessage());
            Toast.makeText(getApplicationContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            mProgressBar.setVisibility(View.INVISIBLE);
        });
        MyApplication.getInstance().addToRequestQueue(request);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_main, menu);

        mMediaRouteMenuItem = CastButtonFactory.setUpMediaRouteButton(getApplicationContext(),menu,R.id.media_route_menu_item);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.action_search)
                .getActionView();
        searchView.setSearchableInfo(searchManager
                .getSearchableInfo(getComponentName()));
        searchView.setMaxWidth(Integer.MAX_VALUE);

        // listening to search query text change
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // filter recycler view when query submitted
                mAdapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                // filter recycler view when text is changed
                mAdapter.getFilter().filter(query);
                return false;
            }
        });

        return true;
    }

    public void switchActivities() {
        Intent intent = new Intent(this, PlayerActivity.class);
        startActivity(intent);
    }
}