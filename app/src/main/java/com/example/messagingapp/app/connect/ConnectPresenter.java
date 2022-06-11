package com.example.messagingapp.app.connect;

import java.util.regex.Pattern;

/**
 * A presenter handling the validation of the connect form's fields.
 *
 * @author Alex Mandelias
 * @author Dimitris Tsirmpas
 */
class ConnectPresenter {

    private static final Pattern IP_PATTERN = Pattern.compile(
            "^(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\\.(?!$)|$)){4}$");

    private static final Pattern PORT_PATTERN = Pattern.compile(
            "^([1-9][0-9]{0,3}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])$");

    /**
     * Returns whether the given IP address is a syntactically correct IPv4 address.
     *
     * @param ip the IP to check
     *
     * @return {@code true} if the IP is valid, {@code false} otherwise
     */
    public boolean checkIP(String ip) {
        return IP_PATTERN.matcher(ip).matches();
    }

    /**
     * Returns whether the given port number is a valid port number.
     *
     * @param port the port number to check
     *
     * @return {@code true} if the port number is a valid port number, {@code false} otherwise
     */
    public boolean checkPort(String port) {
        return PORT_PATTERN.matcher(port).matches();
    }
}
