package com.example.koalatv;

import java.net.URL;

public class Channel {


    private String id;
    private String channelName;
    private URL icon;
    private String channelDesc;
    private URL streamUrl;


    public String getId() {
        return id;
    }

    public String getChannelName() {
        return channelName;
    }

    public URL getIcon() {
        return icon;
    }

    public String getChannelDesc() {
        return channelDesc;
    }

    public URL getStreamUrl() {
        return streamUrl;
    }
}


