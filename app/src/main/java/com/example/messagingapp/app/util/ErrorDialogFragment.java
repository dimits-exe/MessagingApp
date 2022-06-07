package com.example.messagingapp.app.util;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.messagingapp.app.R;

public class ErrorDialogFragment extends DialogFragment {

    private static final String EXCEPTION = "EXCEPTION";

    public ErrorDialogFragment() {}

    public static void startFromException(FragmentManager fm, Exception e) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(ErrorDialogFragment.EXCEPTION, e);

        DialogFragment edf = new ErrorDialogFragment();
        edf.setArguments(bundle);
        edf.show(fm, "errorDialog");
        Log.wtf("EDF", "show doesn't block");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Exception e = (Exception) savedInstanceState.getSerializable(ErrorDialogFragment.EXCEPTION);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Fatal Error Occurred")
                .setMessage(e.getMessage())
                .setCancelable(false)
                .setIcon(R.drawable.error_icon)
                .setNeutralButton("Close the Application", (dialog, which) -> {
                    ExitActivity.exit(getActivity());
                });

        return builder.create();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        getActivity().finishAffinity();
    }

    // https://stackoverflow.com/questions/6330200/how-to-quit-android-application-programmatically#answer-50857534
    public static class ExitActivity extends AppCompatActivity {

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            finish();
        }

        public static void exit(Context context) {
            Intent intent = new Intent(context, ExitActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
        }
    }
}
