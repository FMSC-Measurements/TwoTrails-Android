package com.usda.fmsc.twotrails.activities;

import android.os.Bundle;
import android.view.Menu;

import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.activities.custom.BaseMapActivity;
import com.usda.fmsc.twotrails.gps.GpsService;
import com.usda.fmsc.twotrails.objects.map.ArcGisMapLayer;

public class TestActivity extends BaseMapActivity {
    GpsService.GpsBinder binder;


    ArcGisMapLayer layer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        setLocationEnabled(false);
    }

    boolean d = true;

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        if (item.getItemId() == R.id.mmSelectMap) {
//            //selectMapType();
//
//            if (d) {
//                    ArcGISTools.getLayerFromUrl("https://sampleserver6.arcgisonline.com/arcgis/rest/services/World_Street_Map/MapServer", getBaseContext(), new ArcGISTools.IGetArcMapLayerListener() {
//                        @Override
//                        public void onComplete(ArcGisMapLayer l) {
//                            layer = l;
//
//                            levels = new double[layer.getNumberOfLevels()];
//
//                            // Specify all the Levels of details in an integer array
//                            for (int i = 0; i < layer.getNumberOfLevels(); i++) {
//                                levels[i] = layer.getLevelsOfDetail()[i].getResolution();
//                            }
//
//                            ((ArcGisMapFragment)mapFragment).changeBasemap(layer);
//                            d = false;
//                        }
//
//                        @Override
//                        public void onBadUrl() {
//
//                        }
//                    });
//                } else {
//
//                downloadBasemap();
//            }
//            }
//
//        return super.onOptionsItemSelected(item);
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_test, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (binder != null) {
            binder.removeListener(this);

            if (!Global.Settings.DeviceSettings.isGpsAlwaysOn()) {
                binder.stopGps();
            }
        }
    }



//
//
//
//    static final String TAG = "ExportTileCacheTaskSampleActivity";
//
//    // Map elements
//    ArcGISLocalTiledLayer localTiledLayer;
//
//    // action bar menu items
//    MenuItem selectLevels;
//    MenuItem download;
//    MenuItem switchMaps;
//
//    boolean isLocalLayerVisible = false;
//
//    // The generated tile cache will be a compact cache
//    boolean createAsTilePackage = false;
//
//    double[] levels;
//
//    final CharSequence[] items = { "Level ID:0", "Level ID:1", "Level ID:2",
//            "Level ID:3", "Level ID:4", "Level ID:5", "Level ID:6",
//            "Level ID:7", "Level ID:8", "Level ID:9", };
//
//    double[] mapResolution = { 156543.03392800014, 78271.51696399994,
//            39135.75848200009, 19567.87924099992, 9783.93962049996,
//            4891.96981024998, 2445.98490512499, 1222.992452562495,
//            611.4962262813797, 305.74811314055756 };
//
//    boolean[] itemsChecked = new boolean[items.length];
//    ArrayList<Double> levelsArraylist = new ArrayList<Double>();
//
//
//    @SuppressWarnings("boxing")
//    public void downloadBasemap() {
//
//        // Set the progressbar to VISIBLE
//        //setProgressBarIndeterminateVisibility(true);
//
//        // Get the the extent covered by generated tile cache, here we are using
//        // the area being displayed on screen
//        Envelope extentForTPK = ((ArcGisMapFragment)mapFragment).getArcExtents();
//
//
//        // If the user does not select the Level of details
//        // then give out the status message in a toast
////        if (levelsArraylist.size() == 0) {
////            Toast.makeText(this, "Please Select Levels of Detail",
////                    Toast.LENGTH_LONG).show();
////            // Hide the progress bar
////            setProgressBarIndeterminateVisibility(false);
////            return;
////        }
//
//        //levels = new double[levelsArraylist.size()];
//        final String tileCachePath = String.format("%s%s%s", TtUtils.getOfflineMapsDir(), File.separator, "test.tpk");
//
//        File m = new File(TtUtils.getOfflineMapsDir());
//        m.mkdirs();
//
//        // Create an instance of ExportTileCacheTask for the mapService that
//        // supports the exportTiles() operation
//        final ExportTileCacheTask exportTileCacheTask = new ExportTileCacheTask(
//                layer.getUri(), null);
//
//        // Set up GenerateTileCacheParameters
//        ExportTileCacheParameters params = new ExportTileCacheParameters(
//                createAsTilePackage, levels, ExportTileCacheParameters.ExportBy.ID, extentForTPK,
//                ((ArcGisMapFragment) mapFragment).getSpatialReference());
//
//        // create tile cache
//        createTileCache(params, exportTileCacheTask, tileCachePath);
//    }
//
//    /**
//     * Creates tile Cache locally by calling generateTileCache
//     *
//     * @param params
//     * @param exportTileCacheTask
//     * @param tileCachePath
//     */
//    private void createTileCache(ExportTileCacheParameters params,
//                                 final ExportTileCacheTask exportTileCacheTask,
//                                 final String tileCachePath) {
//
//        // estimate tile cache size
//        exportTileCacheTask.estimateTileCacheSize(params,
//                new CallbackListener<Long>() {
//
//                    @Override
//                    public void onError(Throwable e) {
//                        Log.d("*** tilecachesize error: ", "" + e);
//                    }
//
//                    @Override
//                    public void onCallback(Long objs) {
//                        Log.d("*** tilecachesize: ", "" + objs);
//                        final long tilecachesize = objs / 1000;
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                Toast.makeText(
//                                        getApplicationContext(),
//                                        "Approx. Tile Cache size to download : "
//                                                + tilecachesize + " KB",
//                                        Toast.LENGTH_LONG).show();
//                            }
//                        });
//
//                    }
//                });
//
//        // create status listener for generateTileCache
//        CallbackListener<ExportTileCacheStatus> statusListener = new CallbackListener<ExportTileCacheStatus>() {
//
//            @Override
//            public void onError(Throwable e) {
//                Log.d("*** tileCacheStatus error: ", "" + e);
//            }
//
//            @Override
//            public void onCallback(ExportTileCacheStatus objs) {
//                Log.d("*** tileCacheStatus : ", objs.getStatus().toString());
//                final String status = objs.getStatus().toString();
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Toast.makeText(getApplicationContext(), status,
//                                Toast.LENGTH_SHORT).show();
//                    }
//                });
//
//            }
//        };
//
//        // Submit tile cache job and download
//        exportTileCacheTask.generateTileCache(params, statusListener,
//                new CallbackListener<String>() {
//                    private boolean errored = false;
//
//                    @Override
//                    public void onError(Throwable e) {
//                        errored = true;
//                        // print out the error message and disable the progress
//                        // bar
//                        Log.d("*** generateTileCache error: ", "" + e);
//                        final String error = e.toString();
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                setProgressBarIndeterminateVisibility(false);
//                                Toast.makeText(getApplicationContext(),
//                                        "generateTileCache error: " + error,
//                                        Toast.LENGTH_LONG).show();
//                            }
//                        });
//                    }
//
//                    @Override
//                    public void onCallback(String path) {
//                        if (!errored) {
//                            Log.d("the Download Path = ", "" + path);
//
//                            // switch to the successfully downloaded local layer
//                            localTiledLayer = new ArcGISLocalTiledLayer(path);
//                            //mMapView.addLayer(localTiledLayer);
//                            // initially setting the visibility to false,
//                            // turning it back on in the switchToLocalLayer()
//                            // method
//                            //mMapView.getLayers()[1].setVisible(false);
//
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    // Hide the progress bar
//                                    setProgressBarIndeterminateVisibility(false);
//                                    Toast.makeText(
//                                            getApplicationContext(),
//                                            "TileCache successfully downloaded, Switching to Local Tiled Layer",
//                                            Toast.LENGTH_LONG).show();
//
//                                    //switchToLocalLayer();
//                                }
//                            });
//                        }
//                    }
//                }, tileCachePath);
//
//    }
}
