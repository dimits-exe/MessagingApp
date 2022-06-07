package com.example.messagingapp.app.util.strategies;

import android.app.AlertDialog;
import android.content.Context;

/**
 * An error display implementation which shows a popup alert on the user's screen.
 *
 * @author Dimitris Tsirmpas
 */
public class SeriousErrorMessageStrategy implements IErrorMessageStrategy {

    private static final String TITLE = "Error";

    private final Context context;
    private final int okMessageResource;

    /**
     * Constructs a new DefaultErrorMessageStrategy.
     *
     * @param context           the context in which the error will be shown
     * @param okMessageResource the id of the string resource corresponding to the app's ok message
     */
    public SeriousErrorMessageStrategy(Context context, int okMessageResource) {
        this.context = context;
        this.okMessageResource = okMessageResource;
    }

    @Override
    public void showError(String errorMessage) {
        new AlertDialog.Builder(context)
                .setCancelable(true)
                .setTitle(SeriousErrorMessageStrategy.TITLE)
                .setMessage(errorMessage)
                .setPositiveButton(okMessageResource, null)
                .create()
                .show();
    }
}
