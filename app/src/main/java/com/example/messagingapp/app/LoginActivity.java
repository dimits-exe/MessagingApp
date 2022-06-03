package com.example.messagingapp.app;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.auebds.yammiapp.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        com.auebds.yammiapp.databinding.ActivityLoginBinding binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_login);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_login);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    /**
     * Creates a new local account, and initiates the EDS system.
     * This method is automatically called by the Create New button.
     */
    public void createNewAccount() {
        startApp();
    }

    /**
     * Logins to an existing local account, and initiates the EDS System.
     * This method is automatically called by the Submit button.
     */
    public void loginToAccount() {
        startApp();
    }

    /**
     * Initiates the EDS System and switches the app to the main screen.
     */
    private void startApp() {

    }
}