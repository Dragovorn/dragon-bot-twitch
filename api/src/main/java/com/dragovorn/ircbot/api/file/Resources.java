package com.dragovorn.ircbot.api.file;

import java.net.URL;

public final class Resources {

    public static URL getResource(String path) {
        return Resources.class.getClassLoader().getResource(path);
    }
}
