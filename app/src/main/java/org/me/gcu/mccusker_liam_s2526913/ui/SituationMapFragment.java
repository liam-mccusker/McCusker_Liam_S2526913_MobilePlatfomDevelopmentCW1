package org.me.gcu.mccusker_liam_s2526913.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.me.gcu.mccusker_liam_s2526913.R;
import org.me.gcu.mccusker_liam_s2526913.model.LocationPoint;
import org.me.gcu.mccusker_liam_s2526913.model.Situation;
import org.me.gcu.mccusker_liam_s2526913.viewmodel.SituationViewModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SituationMapFragment extends Fragment implements OnMapReadyCallback {

    private static final LatLng SCOTLAND = new LatLng(56.8, -4.2);

    private SituationViewModel viewModel;
    private GoogleMap map;
    private TextView countText;
    private boolean fitCameraToData;   // only zoom to the data when the map first opens

    // Marker images are made once per icon type and reused
    private final Map<Integer, BitmapDescriptor> markerIcons = new HashMap<>();

    public SituationMapFragment() {
        super(R.layout.fragment_situation_map);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(SituationViewModel.class);
        countText = view.findViewById(R.id.mapCount);

        // After rotation the map remembers its own position, so don't move it again
        fitCameraToData = (savedInstanceState == null);

        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.mapContainer);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);   // onMapReady is called when the map is ready
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setZoomControlsEnabled(true);

        if (fitCameraToData) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(SCOTLAND, 6f));
        }

        // Tapping a marker's popup opens that situation's details
        map.setOnInfoWindowClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof Situation) {
                openDetails((Situation) tag);
            }
        });

        viewModel.getVisibleSituations().observe(getViewLifecycleOwner(), this::showSituations);
    }

    private void showSituations(List<Situation> situations) {
        if (map == null) {
            return;
        }
        map.clear();

        LatLngBounds.Builder bounds = new LatLngBounds.Builder();
        LatLng lastPosition = null;
        int shown = 0;

        for (Situation s : situations) {
            LatLng from = toLatLng(s.getFrom());
            LatLng to = toLatLng(s.getTo());
            LatLng position = (from != null) ? from : to;
            if (position == null) {
                continue;   // no usable location, so it can't go on the map
            }

            Marker marker = map.addMarker(new MarkerOptions()
                    .position(position)
                    .title(s.getDisplayType())
                    .snippet(s.getDescription())
                    .icon(markerIcon(SituationStyle.iconFor(s)))
                    .anchor(0.5f, 0.5f));
            if (marker != null) {
                marker.setTag(s);   // so a tap knows which situation it belongs to
            }
            bounds.include(position);

            // Line along the affected stretch of road
            if (from != null && to != null) {
                map.addPolyline(new PolylineOptions()
                        .add(from, to)
                        .width(10f)
                        .color(lineColour(s)));
                bounds.include(to);
            }

            lastPosition = position;
            shown++;
        }

        countText.setText(getString(R.string.map_count, shown));

        if (fitCameraToData && shown > 0) {
            fitCameraToData = false;
            if (shown == 1) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(lastPosition, 13f));
            } else {
                fitCamera(bounds.build());
            }
        }
    }

    private void fitCamera(LatLngBounds bounds) {
        View mapView = requireView();
        mapView.post(() -> {   // wait until the view has its size
            if (map == null) return;
            int width = mapView.getWidth();
            int height = mapView.getHeight();
            if (width > 0 && height > 0) {
                map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, width, height, dp(48)));
            }
        });
    }

    private void openDetails(Situation situation) {
        viewModel.selectSituation(situation);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, new SituationDetailsFragment())
                .addToBackStack(null)
                .commit();
    }

    private LatLng toLatLng(LocationPoint point) {
        if (point == null || (point.getLatitude() == 0 && point.getLongitude() == 0)) {
            return null;
        }
        return new LatLng(point.getLatitude(), point.getLongitude());
    }

    // Red for incidents; roadworks use their delay colour, matching the list
    private int lineColour(Situation s) {
        int colourRes = "roadworks".equals(s.getCategory())
                ? SituationStyle.delayColour(s.getDelaysType())
                : R.color.incident_red;
        return ContextCompat.getColor(requireContext(), colourRes);
    }

    // Draws the situation's icon on a white circle, for use as a map marker
    private BitmapDescriptor markerIcon(int drawableId) {
        BitmapDescriptor cached = markerIcons.get(drawableId);
        if (cached != null) {
            return cached;
        }

        int size = dp(34);
        int iconSize = dp(24);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setColor(Color.WHITE);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, fill);

        Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(dp(1));
        border.setColor(Color.GRAY);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - dp(1), border);

        Drawable icon = ContextCompat.getDrawable(requireContext(), drawableId);
        if (icon != null) {
            int offset = (size - iconSize) / 2;
            icon.setBounds(offset, offset, offset + iconSize, offset + iconSize);
            icon.draw(canvas);
        }

        BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
        markerIcons.put(drawableId, descriptor);
        return descriptor;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}