package com.usda.fmsc.twotrails.activities.base;

import android.support.v7.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.logic.PolygonAdjuster;
import com.usda.fmsc.twotrails.utilities.TtUtils;

public abstract class TtAjusterCustomToolbarActivity extends CustomToolbarActivity implements PolygonAdjuster.Listener {
    private ProgressDialog pd;
    private MenuItem miAdjust;
    private AnimationDrawable adAdjust;
    private boolean isAdjusting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        //create progress dialog
        pd = new ProgressDialog(this);
        pd.setMessage("Adjusting Polygons..");
        pd.setCancelable(false);
        pd.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                PolygonAdjuster.cancel();
            }
        });
    }


    @Override
    public final boolean onCreateOptionsMenu(Menu menu) {
        boolean result = onCreateOptionsMenuEx(menu);

        if (menu != null) {
            miAdjust = menu.findItem(R.id.miAdjPoly);
            setupMenu();
        }

        return result;
    }

    public abstract boolean onCreateOptionsMenuEx(Menu menu);

    private void setupMenu() {
        if (miAdjust != null) {
            adAdjust = (AnimationDrawable)miAdjust.getIcon();

            final Context context = this;

            miAdjust.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    AlertDialog.Builder alert = new AlertDialog.Builder(context);

                    alert.setTitle(R.string.diag_adjusting_cancel_title);
                    alert.setMessage(R.string.diag_adjusting_cancel);

                    alert.setPositiveButton("Keep Adjusting", null);

                    alert.setNegativeButton(R.string.str_cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            PolygonAdjuster.cancel();
                        }
                    });

                    alert.show();
                    return false;
                }
            });
        }
    }

    @Override
    public final void adjusterStarted() {
        if (adAdjust == null) {
            setupMenu();
        }

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (miAdjust != null) {
                    try {
                        miAdjust.setVisible(true);
                        adAdjust.start();
                        isAdjusting = true;
                    } catch (Exception ex) {
                        TtUtils.TtReport.writeError(ex.getMessage(), "adjusterStarted");
                    }
                } else {
                    pd.show();
                }
            }
        });
    }

    @Override
    public final void adjusterStopped(final PolygonAdjuster.AdjustResult result){
        final Context ctx = this;

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (pd.isShowing()) {
                    pd.dismiss();
                }

                if (miAdjust != null && isAdjusting) {
                    miAdjust.setVisible(false);
                    adAdjust.stop();
                }

                CharSequence text = null;

                switch (result) {
                    case ERROR:
                        text = "Polygons Failed to Adjust";
                        break;
                    case CANCELED:
                        text = "Adjusting Canceled";
                        break;
                    case ADJUSTING:
                    case STARTS_WITH_TRAV_TYPE:
                    case NO_POLYS:
                    case BAD_POINT:
                    case SUCCESSFUL:
                    default:
                        break;
                }

                if (text != null) {
                    Toast.makeText(ctx, text, Toast.LENGTH_SHORT).show();
                }
            }
        });

        onAdjusterStopped(result);
    }

    protected void onAdjusterStopped(PolygonAdjuster.AdjustResult result) {

    }

    @Override
    public void adjusterRunningSlow() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder alert = new AlertDialog.Builder(getBaseContext());

                alert.setTitle(R.string.diag_slow_adjusting_title);
                alert.setMessage(R.string.diag_slow_adjusting);

                alert.setPositiveButton("Wait", null);

                alert.setNegativeButton(R.string.str_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        PolygonAdjuster.cancel();
                    }
                });

                alert.show();
            }
        });

    }
}
