package com.example.messagingapp.app.connect;

import java.util.regex.Pattern;

/**
 * A presenter handling the validation of the connect form's fields.
 *
 * @author Dimitris Tsirmpas
 */
class ConnectPresenter {

    private static final Pattern IP_PATTERN = Pattern.compile(
            "^(([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\\.(?!$)|$)){4}$");
    private static final int PORT_LOWER_LIMIT = 0;
    private static final int PORT_UPPER_LIMIT = 65535;

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
        try {
            int portNumber = Integer.parseInt(port);
            return portNumber >= PORT_LOWER_LIMIT && portNumber <= PORT_UPPER_LIMIT;
        } catch (NumberFormatException ne) {
            return false;
        }
    }
}
