package com.dragovorn.dragonbot.user;

import com.dragovorn.dragonbot.api.user.IUser;
import com.google.common.collect.ImmutableMap;

public class User implements IUser {

    protected String username;

    protected long userId;

    @Override
    public boolean isMod() {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public boolean hasTag(String tag) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public long getUserId() {
        return this.userId;
    }

    @Override
    public String getLogin() {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public String getHostname() {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public String getTag(String tag) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public ImmutableMap<String, String> getTags() {
        throw new UnsupportedOperationException("Unimplemented");
    }
}