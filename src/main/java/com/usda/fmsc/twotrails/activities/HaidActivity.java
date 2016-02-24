package com.usda.fmsc.twotrails.activities;

import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.usda.fmsc.twotrails.activities.custom.CustomToolbarActivity;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.logic.HaidLogic;
import com.usda.fmsc.twotrails.objects.TtPolygon;

import java.util.ArrayList;

public class HaidActivity extends CustomToolbarActivity {
    private ActionBarDrawerToggle drawerToggle;
    private DrawerLayout drawerLayout;
    private PolyInfo[] polyinfo;
    private PolyInfo currentPoly;
    private String onWait;
    private ProgressBar progress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_haid);

        drawerLayout = (DrawerLayout)findViewById(R.id.haidNavDrawer);
        progress = (ProgressBar)findViewById(R.id.progress);

        ListView lvPolys = (ListView)findViewById(R.id.haidLvPolys);
        final TextView tvInfo = (TextView)findViewById(R.id.haidTxtInfo);

        ArrayList<TtPolygon> polys = Global.DAL.getPolygons();

        if (polys.size() < 1) {
            finish();
            return;
        }

        String[] polyNames = new String[polys.size()];
        polyinfo = new PolyInfo[polys.size()];

        int i = 0;
        for (TtPolygon poly : polys) {
            polyNames[i] = poly.getName();
            polyinfo[i] = new PolyInfo(poly);
            i++;
        }

        lvPolys.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, polyNames));

        lvPolys.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (i > -1) {
                    currentPoly = polyinfo[i];

                    if (currentPoly.getText() != null) {
                        onWait = null;

                        tvInfo.setText(currentPoly.getText());

                        progress.setVisibility(View.GONE);
                    } else {
                        onWait = currentPoly.getCN();
                        progress.setVisibility(View.VISIBLE);

                        currentPoly.setListener(new PolyInfo.Listener() {
                            @Override
                            public void onGenerated(String txt) {
                                if (onWait != null && onWait.equals(currentPoly.getCN())) {
                                    tvInfo.setText(currentPoly.getText());
                                    onWait = null;
                                    progress.setVisibility(View.GONE);
                                }
                            }
                        });
                    }

                    getToolbar().setTitle(currentPoly.getName());
                } else {
                    currentPoly = null;
                    tvInfo.setText("Invalid Option");
                    getToolbar().setTitle(getString(R.string.title_activity_haid));
                }

                drawerLayout.closeDrawers();
            }
        });

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, getToolbar(),
                R.string.str_open, R.string.str_close) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (currentPoly != null) {
                    getToolbar().setTitle(currentPoly.getName());
                } else {
                    getToolbar().setTitle(getString(R.string.haid_menu_name));
                }
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getToolbar().setTitle(getString(R.string.haid_menu_name));
            }
        };

        drawerLayout.setDrawerListener(drawerToggle);

        drawerLayout.openDrawer(GravityCompat.START);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_haid, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.menu_haid_help: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                builder.setTitle(R.string.haid_menu_item_help)
                        .setMessage(R.string.haid_help_text)
                        .setPositiveButton(R.string.str_ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User clicked OK button
                            }
                        });

                builder.create().show();
                return true;
            }
            /*
            case R.id.haid_action_increase_font:
            case R.id.haid_action_decrease_font:
                return true;
            */
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


    private static class PolyInfo extends AsyncTask<TtPolygon, Void, String> {
        private TtPolygon polygon;
        private String text;

        private Listener listener;

        public PolyInfo(TtPolygon polygon) {
            this.polygon = polygon;
            this.execute(polygon);
        }

        @Override
        protected String doInBackground(TtPolygon... params) {
            return HaidLogic.generatePolyStats(params[0], Global.DAL, false);
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

        public void setListener(Listener listener) {
            this.listener = listener;
        }

        public interface Listener {
            void onGenerated(String txt);
        }
    }
}