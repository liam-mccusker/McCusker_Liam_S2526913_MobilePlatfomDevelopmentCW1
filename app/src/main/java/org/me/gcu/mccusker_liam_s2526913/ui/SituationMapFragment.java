package org.me.gcu.mccusker_liam_s2526913.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.location.Location;
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
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapColorScheme;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;

import org.me.gcu.mccusker_liam_s2526913.R;
import org.me.gcu.mccusker_liam_s2526913.model.LocationPoint;
import org.me.gcu.mccusker_liam_s2526913.model.Situation;
import org.me.gcu.mccusker_liam_s2526913.viewmodel.SituationViewModel;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SituationMapFragment extends Fragment implements OnMapReadyCallback {

    private static final LatLng SCOTLAND = new LatLng(56.8, -4.2);
    private static final String ARG_SINGLE = "single";

    // Map for just the situation selected on the details screen
    public static SituationMapFragment forSelectedSituation() {
        SituationMapFragment fragment = new SituationMapFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_SINGLE, true);
        fragment.setArguments(args);
        return fragment;
    }

    private SituationViewModel viewModel;
    private GoogleMap map;
    private TextView countText;
    private boolean fitCameraToData;   // only zoom to the data when the map first opens
    private boolean singleMode;        // true = show only the selected situation

    // Marker images are made once per icon type and reused
    private final Map<Integer, BitmapDescriptor> markerIcons = new HashMap<>();

    public SituationMapFragment() {
        super(R.layout.fragment_situation_map);
    }

    public boolean isSingleMode() {
        return singleMode;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(SituationViewModel.class);
        countText = view.findViewById(R.id.mapCount);
        singleMode = getArguments() != null && getArguments().getBoolean(ARG_SINGLE);

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
        map.setMapColorScheme(MapColorScheme.FOLLOW_SYSTEM);   // dark map in dark mode
        map.getUiSettings().setZoomControlsEnabled(true);

        if (fitCameraToData) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(SCOTLAND, 6f));
        }

        if (singleMode) {
            // Show just the selected situation (its popup doesn't reopen details: back does that)
            viewModel.getSelectedSituation().observe(getViewLifecycleOwner(), s ->
                    showSituations(s == null ? Collections.emptyList() : Collections.singletonList(s)));
        } else {
            // Tapping a marker's popup opens that situation's details
            map.setOnInfoWindowClickListener(marker -> {
                Object tag = marker.getTag();
                if (tag instanceof Situation) {
                    openDetails((Situation) tag);
                }
            });
            viewModel.getVisibleSituations().observe(getViewLifecycleOwner(), this::showSituations);
        }
    }

    private void showSituations(List<Situation> situations) {
        if (map == null) {
            return;
        }
        map.clear();

        // During a distance search on the full map, work out where it's measured from
        LatLng centre = null;
        Integer miles = viewModel.getMaxDistanceMiles().getValue();
        LocationPoint reference = viewModel.getReferenceLocation().getValue();
        if (!singleMode && miles != null && miles > 0 && reference != null) {
            centre = new LatLng(reference.getLatitude(), reference.getLongitude());
        }

        LatLngBounds.Builder bounds = new LatLngBounds.Builder();
        LatLng lastPosition = null;
        boolean anyLine = false;
        int shown = 0;

        for (Situation s : situations) {
            LatLng from = toLatLng(s.getFrom());
            LatLng to = toLatLng(s.getTo());

            // Normally the marker goes at the start of the stretch; during a distance
            // search it goes at the end nearest the search centre (the same end the
            // filter measured), so it sits inside the search circle
            LatLng position = (from != null) ? from : to;
            if (centre != null && from != null && to != null) {
                position = (metresBetween(centre, to) < metresBetween(centre, from)) ? to : from;
            }
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

            // Line along the affected stretch of road (not shown during a distance search,
            // so the map only shows what's inside the search circle)
            if (from != null && to != null && centre == null) {
                anyLine = true;
                map.addPolyline(new PolylineOptions()
                        .add(from, to)
                        .width(10f)
                        .color(lineColour(s))
                        .startCap(new RoundCap())
                        .endCap(new RoundCap()));
                bounds.include(from);
                bounds.include(to);
            }

            lastPosition = position;
            shown++;
        }

        // Show the search centre and radius
        if (centre != null) {
            map.addMarker(new MarkerOptions()
                    .position(centre)
                    .title("Searching from " + reference.getName())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            map.addCircle(new CircleOptions()
                    .center(centre)
                    .radius(miles * 1609.344)          // miles to metres
                    .strokeColor(Color.argb(160, 33, 150, 243))
                    .strokeWidth(4f)
                    .fillColor(Color.argb(30, 33, 150, 243)));
            bounds.include(centre);
        }

        if (singleMode) {
            countText.setText(R.string.map_single);
        } else {
            // getQuantityString picks "situation" or "situations" based on the number
            countText.setText(getResources().getQuantityString(R.plurals.map_count, shown, shown));
        }

        if (fitCameraToData && (shown > 0 || centre != null)) {
            fitCameraToData = false;
            if (shown == 0) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(centre, 10f));
            } else if (shown == 1 && centre == null && !anyLine) {
                // A single point with no line: zoom in on it
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(lastPosition, 13f));
            } else {
                // Several situations, or one with a line: fit everything on screen
                fitCamera(bounds.build());
            }
        }
    }

    // Straight-line distance in metres between two map points
    private float metresBetween(LatLng a, LatLng b) {
        float[] result = new float[1];
        Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, result);
        return result[0];
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