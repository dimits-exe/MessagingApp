package com.example.messagingapp.app.util.strategies;

import android.app.Activity;
import android.widget.Toast;

/**
 * An error display implementation which shows a small {@link android.widget.Toast Toast}
 * alert on the user's screen.
 *
 * @author Dimitris Tsirmpas
 */
public class MinorErrorMessageStrategy implements IErrorMessageStrategy {
    private final Activity context;

    public MinorErrorMessageStrategy(Activity context) {
        this.context = context;
    }

    @Override
    public void showError(String errorMessage) {
        context.runOnUiThread(()-> Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show());
    }
}
