/*  Mobile Platform Development - Coursework 1 (26/27)
    Scottish Road Situations: shows Traffic Scotland unplanned events and
    current roadworks from the DATEX II feeds.
    MainActivity is the single Activity. It hosts the fragments (list, details, map)
    and the toolbar; all data comes from the shared SituationViewModel (MVVM).
*/

//
// Name                 Liam McCusker
// Student ID           S2526913
// Programme of Study   Software Development - Mobile Platform Development
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.me.gcu.mccusker_liam_s2526913.ui.SituationListFragment;
import org.me.gcu.mccusker_liam_s2526913.ui.SituationMapFragment;
import org.me.gcu.mccusker_liam_s2526913.viewmodel.SituationViewModel;

public class MainActivity extends AppCompatActivity {

    private SituationViewModel viewModel;

    // Matches the order of the "interval_options" list in strings.xml
    private static final long[] INTERVAL_MINUTES = {1, 15, 30, 60};

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
        } else if (id == R.id.action_interval) {
            showIntervalDialog();
            return true;
        } else if (id == R.id.action_about) {
            showAboutDialog();
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


    private void showIntervalDialog() {
        String[] labels = getResources().getStringArray(R.array.interval_options);
        long current = viewModel.getUpdateIntervalMinutes();
        int checked = INTERVAL_MINUTES.length - 1;
        for (int i = 0; i < INTERVAL_MINUTES.length; i++) {
            if (INTERVAL_MINUTES[i] == current) {
                checked = i;
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.interval_title)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    viewModel.setUpdateIntervalMinutes(INTERVAL_MINUTES[which]);
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showAboutDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.about_title)
                .setMessage(R.string.about_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}