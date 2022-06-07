package com.example.messagingapp.app.connect;

import java.util.regex.Pattern;

/**
 * A presenter handling the checking of the connect form's fields.
 *
 * @author Dimitris Tsirmpas
 */
class ConnectPresenter {
    private static final Pattern IP_PATTERN = Pattern.compile(
            "^(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\\.(?!$)|$)){4}$");

    /**
     * Check if the provided IP is a syntactically correct IPv4 address.
     * @param providedIP the user provided IP
     * @return true if the IP is valid, false otherwise
     */
    public boolean checkIP(String providedIP) {
        return IP_PATTERN.matcher(providedIP).matches();
    }

    /**
     * Check if the provided port is a valid integer.
     * @param providedPort the user provided port
     * @return true if the port is a valid integer, false otherwise
     */
    public boolean checkPort(String providedPort){
        try {
            int result = Integer.parseInt(providedPort);
            return result > 0;
        } catch (NumberFormatException ne){
            return false;
        }
    }
}
