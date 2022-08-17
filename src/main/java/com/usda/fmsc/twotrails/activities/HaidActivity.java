package com.usda.fmsc.twotrails.activities;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;

import android.content.res.Configuration;
import android.os.Bundle;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.utilities.TaskRunner;
import com.usda.fmsc.twotrails.activities.base.TtCustomToolbarActivity;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.logic.HaidLogic;
import com.usda.fmsc.twotrails.objects.TtPolygon;

import java.util.ArrayList;


public class HaidActivity extends TtCustomToolbarActivity {
    private ActionBarDrawerToggle drawerToggle;
    private DrawerLayout drawerLayout;
    private final TaskRunner taskRunner = new TaskRunner();
    private HaidLogic _HaidLogic;
    private PolyInfoTask _PolyInfoTask;
    private PolyInfo[] _PolyInfos;
    private PolyInfo currentPoly;
    private String onWait;
    private ProgressBar progress;
    private boolean showPoints;
    private MenuItem miShowPoints, miTSInc, miTSDec;
    private TextView tvInfo;
    private int textSize = 18;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_haid);

        _HaidLogic = new HaidLogic(getTtAppCtx());
        _PolyInfoTask = new PolyInfoTask(_HaidLogic);


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
        _PolyInfos = new PolyInfo[polys.size()];

        _PolyInfoTask.setListener(polyInfo -> {
            if (currentPoly != null && polyInfo.getPolygon().getCN().equals(currentPoly.getPolygon().getCN())) {
                updateContent();
            }
        });

        int i = 0;
        for (TtPolygon poly : polys) {
            polyNames[i] = poly.getName();
            PolyInfo pi = new PolyInfo(poly, i);
            _PolyInfos[i] = pi;
            taskRunner.executeAsync(_PolyInfoTask, new PolyInfoTask.Params(pi, false));
            i++;
        }

        if (lvPolys != null) {
            lvPolys.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, polyNames));

            lvPolys.setOnItemClickListener((adapterView, view, i1, l) -> {
                if (i1 > -1) {
                    currentPoly = _PolyInfos[i1];

                    updateContent();

                    getToolbar().setTitle(currentPoly.getPolygon().getName());
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
                    getToolbar().setTitle(currentPoly.getPolygon().getName());
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
        if (currentPoly.getHaid() != null) {
            tvInfo.setText(currentPoly.getHaid());
            progress.setVisibility(View.GONE);
        } else {
            progress.setVisibility(View.VISIBLE);
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
            for (int i = 0; i < _PolyInfos.length; i++) {
                pi = _PolyInfos[i];
                taskRunner.executeAsync(_PolyInfoTask, new PolyInfoTask.Params(pi, showPoints));
            }
        } else if (id == R.id.haidMenuHelp) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);

            builder.setTitle(R.string.str_help)
                    .setMessage(R.string.haid_poly_info)
                    .setPositiveButton(R.string.str_ok, null);

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


    private static class PolyInfo {
        private final TtPolygon polygon;
        private String haid;
        private final int index;

        public PolyInfo(TtPolygon polygon, int index) {
            this.polygon = polygon;
            this.index = index;
        }

        public TtPolygon getPolygon() {
            return polygon;
        }

        public int getIndex() {
            return index;
        }

        public String getHaid() {
            return haid;
        }

        public void setHaid(String haid) {
            this.haid = haid;
        }
    }


    private static class PolyInfoTask extends TaskRunner.Task<PolyInfoTask.Params, PolyInfo> {
        private final HaidLogic haidLogic;
        private Listener listener;

        public PolyInfoTask(HaidLogic haidLogic) {
            this.haidLogic = haidLogic;
        }

        @Override
        protected PolyInfo onBackgroundWork(Params params) {
            PolyInfo polyInfo = params.polyInfo;
            polyInfo.setHaid(haidLogic.generatePolyStats(polyInfo.getPolygon(), params.isShowingPoints(), false));
            return polyInfo;
        }

        @Override
        protected void onComplete(PolyInfo result) {
            if (listener != null) {
                listener.onGenerated(result);
            }
        }

        @Override
        protected void onError(Exception exception) {

        }

        public void setListener(Listener listener) {
            this.listener = listener;
        }

        public interface Listener {
            void onGenerated(PolyInfo polyInfo);
        }


        public static class Params {
            private final PolyInfo polyInfo;
            private boolean showPoints;

            public Params(PolyInfo polyInfo, boolean showPoints) {
                this.polyInfo = polyInfo;
                this.showPoints = showPoints;
            }

            public PolyInfo getPolyInfo() {
                return polyInfo;
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
