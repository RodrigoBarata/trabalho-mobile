package com.example.trab_mobile;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class   SkyView extends View {

    private Paint skyPaint, horizonPaint, gridPaint, textPaint, satellitePaint;
    private List<Satellite> satellites = new ArrayList<>();

    private static final String PREFS_NAME = "SkyViewPrefs";
    private static final String KEY_CONSTELLATIONS = "constellations";
    private static final String KEY_SHOW_NOT_IN_FIX = "showNotInFix";

    private SharedPreferences sharedPreferences;

    private Set<String> visibleConstellations = new HashSet<>();
    private boolean showSatellitesNotInFix = true;


    public SkyView(Context context) {
        super(context);
        init(null);
    }

    public SkyView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public SkyView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    private void init(@Nullable AttributeSet attrs) {
        skyPaint = new Paint();
        skyPaint.setColor(Color.BLACK);
        skyPaint.setStyle(Paint.Style.FILL);

        horizonPaint = new Paint();
        horizonPaint.setStyle(Paint.Style.STROKE);
        horizonPaint.setStrokeWidth(2);

        gridPaint = new Paint();
        gridPaint.setColor(Color.GRAY);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1);
        gridPaint.setAlpha(100);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(20);

        satellitePaint = new Paint();

        if (attrs != null) {
            TypedArray a = getContext().getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.SkyView,
                    0, 0);
            try {
                int horizonColor = a.getColor(R.styleable.SkyView_horizonColor, Color.GREEN);
                horizonPaint.setColor(horizonColor);
            } finally {
                a.recycle();
            }
        }

        sharedPreferences = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadSettings();

        setOnClickListener(v -> showSettingsDialog());
    }

    private void loadSettings() {
        Set<String> defaultConstellations = new HashSet<>();
        defaultConstellations.add("GPS");
        defaultConstellations.add("Galileo");
        defaultConstellations.add("Glonass");
        defaultConstellations.add("Beidou");

        visibleConstellations = sharedPreferences.getStringSet(KEY_CONSTELLATIONS, defaultConstellations);
        showSatellitesNotInFix = sharedPreferences.getBoolean(KEY_SHOW_NOT_IN_FIX, true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int radius = Math.min(width, height) / 2 - 40; // Add some padding
        int centerX = width / 2;
        int centerY = height / 2;

        // Draw sky
        canvas.drawCircle(centerX, centerY, radius, skyPaint);

        // Draw horizon and grid lines
        canvas.drawCircle(centerX, centerY, radius, horizonPaint);
        canvas.drawCircle(centerX, centerY, radius * 2 / 3f, gridPaint); // 30 deg elevation
        canvas.drawCircle(centerX, centerY, radius / 3f, gridPaint);     // 60 deg elevation

        // Draw cardinal points
        canvas.drawText(getContext().getString(R.string.north), centerX - 10, centerY - radius - 10, textPaint);
        canvas.drawText(getContext().getString(R.string.south), centerX - 10, centerY + radius + 30, textPaint);
        canvas.drawText(getContext().getString(R.string.east), centerX + radius + 10, centerY + 10, textPaint);
        canvas.drawText(getContext().getString(R.string.west), centerX - radius - 30, centerY + 10, textPaint);

        int visibleCount = 0;
        int usedInFixCount = 0;

        for (Satellite sat : satellites) {
            if (!shouldDisplaySatellite(sat)) continue;

            visibleCount++;
            if (sat.isUsedInFix()) {
                usedInFixCount++;
            }

            float r = (90 - sat.getElevation()) / 90f * radius;
            float x = (float) (centerX + r * Math.sin(Math.toRadians(sat.getAzimuth())));
            float y = (float) (centerY - r * Math.cos(Math.toRadians(sat.getAzimuth())));

            drawSatellite(canvas, x, y, sat);
        }

        // Draw satellite counts
        canvas.drawText(getContext().getString(R.string.visible_satellites, visibleCount), 20, 40, textPaint);
        canvas.drawText(getContext().getString(R.string.in_use_satellites, usedInFixCount), 20, 70, textPaint);
    }

    private void drawSatellite(Canvas canvas, float x, float y, Satellite sat) {
        satellitePaint.setColor(getConstellationColor(sat.getConstellationType()));

        if (sat.isUsedInFix()) {
            satellitePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        } else {
            satellitePaint.setStyle(Paint.Style.STROKE);
            satellitePaint.setStrokeWidth(2);
        }

        // Different shape for different constellations
        switch (sat.getConstellationType()) {
            case 1: // GPS
                canvas.drawCircle(x, y, 12, satellitePaint);
                break;
            case 3: // GLONASS
                canvas.drawRect(x - 10, y - 10, x + 10, y + 10, satellitePaint);
                break;
            case 5: // BEIDOU
                Rect rect = new Rect((int) (x - 12), (int) (y - 8), (int) (x + 12), (int) (y + 8));
                canvas.drawRect(rect, satellitePaint);
                break;
            case 6: // GALILEO
                canvas.save();
                canvas.rotate(45, x, y);
                canvas.drawRect(x - 10, y - 10, x + 10, y + 10, satellitePaint);
                canvas.restore();
                break;
            default:
                canvas.drawCircle(x, y, 10, satellitePaint);
        }

        canvas.drawText(String.valueOf(sat.getSvid()), x + 15, y + 15, textPaint);
    }

    private int getConstellationColor(int constellationType) {
        switch (constellationType) {
            case 1: return Color.BLUE;    // GPS
            case 3: return Color.RED;     // GLONASS
            case 5: return Color.MAGENTA; // BEIDOU
            case 6: return Color.CYAN;    // GALILEO
            default: return Color.WHITE;
        }
    }

    private String getConstellationName(int constellationType) {
        switch (constellationType) {
            case 1: return "GPS";
            case 3: return "Glonass";
            case 5: return "Beidou";
            case 6: return "Galileo";
            default: return "Unknown";
        }
    }

    private boolean shouldDisplaySatellite(Satellite satellite) {
        String constellationName = getConstellationName(satellite.getConstellationType());
        if (!visibleConstellations.contains(constellationName)) {
            return false;
        }
        if (!showSatellitesNotInFix && !satellite.isUsedInFix()) {
            return false;
        }
        return true;
    }

    private void showSettingsDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_sky_view_settings, null);

        CheckBox checkboxGps = dialogView.findViewById(R.id.checkbox_gps);
        CheckBox checkboxGlonass = dialogView.findViewById(R.id.checkbox_glonass);
        CheckBox checkboxBeidou = dialogView.findViewById(R.id.checkbox_beidou);
        CheckBox checkboxGalileo = dialogView.findViewById(R.id.checkbox_galileo);
        SwitchMaterial switchNotInFix = dialogView.findViewById(R.id.switch_not_in_fix);

        checkboxGps.setChecked(visibleConstellations.contains("GPS"));
        checkboxGlonass.setChecked(visibleConstellations.contains("Glonass"));
        checkboxBeidou.setChecked(visibleConstellations.contains("Beidou"));
        checkboxGalileo.setChecked(visibleConstellations.contains("Galileo"));
        switchNotInFix.setChecked(showSatellitesNotInFix);

        new MaterialAlertDialogBuilder(getContext())
                .setTitle(R.string.dialog_title)
                .setView(dialogView)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    visibleConstellations.clear();
                    if (checkboxGps.isChecked()) visibleConstellations.add("GPS");
                    if (checkboxGlonass.isChecked()) visibleConstellations.add("Glonass");
                    if (checkboxBeidou.isChecked()) visibleConstellations.add("Beidou");
                    if (checkboxGalileo.isChecked()) visibleConstellations.add("Galileo");

                    showSatellitesNotInFix = switchNotInFix.isChecked();

                    saveSettings();
                    invalidate(); // Redraw with new settings
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }


    private void saveSettings() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet(KEY_CONSTELLATIONS, visibleConstellations);
        editor.putBoolean(KEY_SHOW_NOT_IN_FIX, showSatellitesNotInFix);
        editor.apply();
    }


    public void setSatellites(List<Satellite> satellites) {
        this.satellites.clear();
        if (satellites != null) {
            this.satellites.addAll(satellites);
        }
        invalidate();
    }
}
