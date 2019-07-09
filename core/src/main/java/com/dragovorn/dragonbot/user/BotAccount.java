package com.dragovorn.dragonbot.user;

import com.dragovorn.dragonbot.api.config.IConfiguration;
import com.dragovorn.dragonbot.api.config.IConfigurationSerializable;

// This class isn't exposed in the API, should make storing the bot account a little more secure.
public final class BotAccount extends User implements IConfigurationSerializable {

    private String accessToken;

    public void setUsername(String username) {
        this.username = username;
    }

    public void setAccessToken(String token) {
        this.accessToken = token;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getAccessToken() {
        return this.accessToken;
    }

    @Override
    public void save(IConfiguration configuration) {
        configuration.set("account.username", this.username);
        configuration.set("account.access_token", this.accessToken);
    }
}