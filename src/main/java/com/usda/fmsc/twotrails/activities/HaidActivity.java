package com.usda.fmsc.twotrails.activities;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.twotrails.TwoTrailsApp;
import com.usda.fmsc.twotrails.activities.base.CustomToolbarActivity;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.data.DataAccessLayer;
import com.usda.fmsc.twotrails.logic.HaidLogic;
import com.usda.fmsc.twotrails.objects.TtPolygon;

import java.util.ArrayList;

@SuppressLint("DefaultLocale")
public class HaidActivity extends CustomToolbarActivity {
    private ActionBarDrawerToggle drawerToggle;
    private DrawerLayout drawerLayout;
    private PolyInfo[] polyinfo;
    private PolyInfo currentPoly;
    private String onWait;
    private ProgressBar progress;
    private boolean showPoints;
    private MenuItem miShowPoints, miTSInc, miTSDec;
    private TextView tvInfo;
    private int textSize = 18;
    private HaidLogic _HaidLogic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_haid);

        _HaidLogic = new HaidLogic(getTtAppCtx());

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }

        drawerLayout = findViewById(R.id.haidNavDrawer);
        progress = findViewById(R.id.progress);

        ListView lvPolys = findViewById(R.id.haidLvPolys);
        tvInfo = findViewById(R.id.haidTxtInfo);

        ArrayList<TtPolygon> polys = getTtAppCtx().getDAL().getPolygons();

        if (polys.size() < 1) {
            finish();
            return;
        }

        String[] polyNames = new String[polys.size()];
        polyinfo = new PolyInfo[polys.size()];

        int i = 0;
        for (TtPolygon poly : polys) {
            polyNames[i] = poly.getName();
            polyinfo[i] = new PolyInfo(_HaidLogic, poly, false);
            i++;
        }

        if (lvPolys != null) {
            lvPolys.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, polyNames));

            lvPolys.setOnItemClickListener((adapterView, view, i1, l) -> {
                if (i1 > -1) {
                    currentPoly = polyinfo[i1];

                    updateContent();

                    getToolbar().setTitle(currentPoly.getName());
                } else {
                    currentPoly = null;
                    tvInfo.setText("Invalid Option");
                    getToolbar().setTitle(getString(R.string.title_activity_haid));
                }

                drawerLayout.closeDrawers();
            });
        }

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, getToolbar(),
                R.string.str_open, R.string.str_close) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (currentPoly != null) {
                    getToolbar().setTitle(currentPoly.getName());
                } else {
                    getToolbar().setTitle(getString(R.string.str_polygons));
                }
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getToolbar().setTitle(getString(R.string.str_polygons));
            }
        };

        drawerLayout.addDrawerListener(drawerToggle);

        drawerLayout.openDrawer(GravityCompat.START);
    }

    private void updateContent() {
        if (currentPoly.getText() != null) {
            onWait = null;

            tvInfo.setText(currentPoly.getText());

            progress.setVisibility(View.GONE);
        } else {
            onWait = currentPoly.getCN();
            progress.setVisibility(View.VISIBLE);

            currentPoly.setListener(txt -> {
                if (onWait != null && onWait.equals(currentPoly.getCN())) {
                    tvInfo.setText(currentPoly.getText());
                    onWait = null;
                    progress.setVisibility(View.GONE);
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        inflateMenu(R.menu.menu_haid, menu);

        miShowPoints = menu.findItem(R.id.haidMenuShowPoints);
        miTSInc = menu.findItem(R.id.haidMenuIncreaseTextSize);
        miTSDec = menu.findItem(R.id.haidMenuDecreaseTextSize);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.haidMenuShowPoints) {
            miShowPoints.setTitle(showPoints ? R.string.haid_menu_show_points : R.string.haid_menu_hide_points);
            miShowPoints.setIcon(showPoints ? R.drawable.ic_action_location_on_black : R.drawable.ic_action_location_off_black);

            showPoints = !showPoints;

            PolyInfo pi;
            for (int i = 0; i < polyinfo.length; i++) {
                pi = new PolyInfo(_HaidLogic, polyinfo[i].getPolygon(), showPoints);
                polyinfo[i] = pi;

                if (pi.getCN().equals(currentPoly.getCN())) {
                    currentPoly = pi;
                    updateContent();
                }
            }
        } else if (id == R.id.haidMenuHelp) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setTitle(R.string.str_help)
                    .setMessage(R.string.haid_help_text)
                    .setPositiveButton(R.string.str_ok, (dialog, id1) -> {
                        // User clicked OK button
                    });

            builder.create().show();
        } else if (id == R.id.haidMenuIncreaseTextSize) {
            if (textSize < 48) {
                textSize *= 1.2;

                if (textSize >= 48) {
                    AndroidUtils.UI.disableMenuItem(miTSInc);
                }

                if (!miTSDec.isEnabled()) {
                    AndroidUtils.UI.enableMenuItem(miTSDec);
                }

                tvInfo.setTextSize(textSize);
            }
        } else if (id == R.id.haidMenuDecreaseTextSize) {
            if (textSize > 8) {
                textSize /= 1.2;

                if (textSize <= 8) {
                    AndroidUtils.UI.disableMenuItem(miTSDec);
                }

                if (!miTSInc.isEnabled()) {
                    AndroidUtils.UI.enableMenuItem(miTSInc);
                }

                tvInfo.setTextSize(textSize);
            }
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }


    private static class PolyInfo extends AsyncTask<PolyInfo.PolyInfoParams, Void, String> {
        private final TtPolygon polygon;
        private final HaidLogic haidLogic;
        private String text;

        private Listener listener;

        public PolyInfo(HaidLogic haidLogic, TtPolygon polygon, boolean showPoints) {
            this.haidLogic = haidLogic;
            this.polygon = polygon;
            this.execute(new PolyInfoParams(polygon, showPoints));
        }

        @Override
        protected String doInBackground(PolyInfoParams... params) {
            return haidLogic.generatePolyStats(params[0].polygon, params[0].isShowingPoints(), false);
        }

        @Override
        protected void onPostExecute(String s) {
            this.text = s;

            if (listener != null) {
                listener.onGenerated(text);
            }
        }

        public String getText() {
            return text;
        }

        public String getName() {
            return polygon.getName();
        }

        public String getCN() {
            return polygon.getCN();
        }

        public TtPolygon getPolygon() {
            return polygon;
        }

        public void setListener(Listener listener) {
            this.listener = listener;
        }

        public interface Listener {
            void onGenerated(String txt);
        }


        public static class PolyInfoParams {
            private TtPolygon polygon;
            private boolean showPoints;

            public PolyInfoParams(TtPolygon polygon, boolean showPoints) {
                this.polygon = polygon;
                this.showPoints = showPoints;
            }

            public TtPolygon getPolygon() {
                return polygon;
            }

            public void setPolygon(TtPolygon polygon) {
                this.polygon = polygon;
            }

            public boolean isShowingPoints() {
                return showPoints;
            }

            public void setShowPoints(boolean showPoints) {
                this.showPoints = showPoints;
            }
        }
    }
}
