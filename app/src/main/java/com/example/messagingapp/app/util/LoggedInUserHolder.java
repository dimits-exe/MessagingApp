package com.example.messagingapp.app.util;

import com.example.messagingapp.app.login.LoggedInUser;

/**
 * A singleton object holding the current {@link LoggedInUser}.
 *
 * @author Dimitris Tsirmpas
 */
public final class LoggedInUserHolder {
    private static final LoggedInUser user = new LoggedInUser();

    /**
     * Get the held {@link LoggedInUser}.
     * @return the logged-in user
     */
    public static LoggedInUser getInstance() {
        return user;
    }

    private LoggedInUserHolder(){}
}
