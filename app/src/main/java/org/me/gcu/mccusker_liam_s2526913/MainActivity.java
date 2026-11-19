/*  Starter project for Mobile Platform Development - 1st diet 26/27
    You should use this project as the starting point for your assignment.
    This project simply reads the data from the required source and displays the
    raw data in a TextField
*/

//
// Name                 Liam McCusker
// Student ID           S2526913
// Programme of Study   _________________
//

package org.me.gcu.mccusker_liam_s2526913;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;

import org.me.gcu.mccusker_liam_s2526913.ui.SituationListFragment;
import org.me.gcu.mccusker_liam_s2526913.ui.SituationMapFragment;
import org.me.gcu.mccusker_liam_s2526913.viewmodel.SituationViewModel;

public class MainActivity extends AppCompatActivity {

    private SituationViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Toolbar acts as the app bar: title, back arrow and menu actions
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.app_title);

        // Status line ("Live data · updated 14:05", "Offline · ...")
        viewModel = new ViewModelProvider(this).get(SituationViewModel.class);
        TextView statusText = findViewById(R.id.statusText);
        viewModel.getStatus().observe(this, statusText::setText);

        // Add the list only on first start; after rotation Android restores it automatically
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new SituationListFragment())
                    .commit();
        }

        // Show the back arrow whenever there is a screen to go back to
        getSupportFragmentManager().addOnBackStackChangedListener(this::updateBackArrow);
        updateBackArrow();
    }

    private void updateBackArrow() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            boolean canGoBack = getSupportFragmentManager().getBackStackEntryCount() > 0;
            actionBar.setDisplayHomeAsUpEnabled(canGoBack);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_map) {
            showMap();
            return true;
        } else if (id == R.id.action_refresh) {
            viewModel.refresh();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Called when the toolbar's back arrow is tapped
    @Override
    public boolean onSupportNavigateUp() {
        getSupportFragmentManager().popBackStack();
        return true;
    }

    private void showMap() {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
        if (!(current instanceof SituationMapFragment)) {   // don't open the map twice
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new SituationMapFragment())
                    .addToBackStack(null)
                    .commit();
        }
    }
}