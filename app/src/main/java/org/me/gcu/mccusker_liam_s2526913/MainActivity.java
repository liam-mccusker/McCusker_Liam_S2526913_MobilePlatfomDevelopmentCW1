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
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import org.me.gcu.mccusker_liam_s2526913.ui.SituationListFragment;
import org.me.gcu.mccusker_liam_s2526913.viewmodel.SituationViewModel;

public class MainActivity extends AppCompatActivity {

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

        // Status line ("Loading…" / "Last updated 17:42")
        TextView statusText = findViewById(R.id.statusText);
        SituationViewModel viewModel = new ViewModelProvider(this).get(SituationViewModel.class);
        viewModel.getStatus().observe(this, statusText::setText);

        // Add the list only on first start; after rotation Android restores it automatically
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new SituationListFragment())
                    .commit();
        }
    }
}