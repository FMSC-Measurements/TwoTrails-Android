package com.usda.fmsc.twotrails.utilities;

import android.content.Context;
import android.os.AsyncTask;

import com.esri.android.map.Layer;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISLocalTiledLayer;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.core.ags.FeatureServiceInfo;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.io.UserCredentials;
import com.esri.core.map.CallbackListener;
import com.esri.core.tasks.geodatabase.GenerateGeodatabaseParameters;
import com.esri.core.tasks.geodatabase.GeodatabaseStatusCallback;
import com.esri.core.tasks.geodatabase.GeodatabaseStatusInfo;
import com.esri.core.tasks.geodatabase.GeodatabaseSyncTask;
import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.objects.ArcGisMapLayer;

import java.util.Collection;
import java.util.HashMap;

public class ArcGISTools {
    private static SpatialReference LatLonSpatialReference = SpatialReference.create(4326);

    private static HashMap<Integer, ArcGisMapLayer> mapLayers;
    private static int idCounter = 0;


    private static void init() {
        mapLayers = new HashMap<>();
        for (ArcGisMapLayer layer : Global.Settings.DeviceSettings.getArcGisMayLayers()) {
            mapLayers.put(layer.getId(), layer);
        }

        if (mapLayers.size() < 1) {
            Context context = Global.getMainActivity().getApplicationContext();

            ArcGisMapLayer layer;

            layer = new ArcGisMapLayer(
                    0,
                    context.getString(R.string.agmap_World_Imagery_name),
                    context.getString(R.string.agmap_World_Imagery_desc),
                    context.getString(R.string.agmap_World_Imagery_url),
                    true);

            mapLayers.put(layer.getId(), layer);

            layer = new ArcGisMapLayer(
                getId(),
                context.getString(R.string.agmap_NatGeo_World_Map_name),
                context.getString(R.string.agmap_NatGeo_World_Map_desc),
                context.getString(R.string.agmap_NatGeo_World_Map_url),
                true);

            mapLayers.put(layer.getId(), layer);

            layer = new ArcGisMapLayer(
                    getId(),
                    context.getString(R.string.agmap_NGS_Topo_US_2D_name),
                    context.getString(R.string.agmap_NGS_Topo_US_2D_desc),
                    context.getString(R.string.agmap_NGS_Topo_US_2D_url),
                    true);

            mapLayers.put(layer.getId(), layer);

            layer = new ArcGisMapLayer(
                    getId(),
                    context.getString(R.string.agmap_NGS_Topo_US_2D_name),
                    context.getString(R.string.agmap_NGS_Topo_US_2D_desc),
                    context.getString(R.string.agmap_NGS_Topo_US_2D_url),
                    true);

            mapLayers.put(layer.getId(), layer);

            layer = new ArcGisMapLayer(
                    getId(),
                    context.getString(R.string.agmap_Ocean_Basemap_name),
                    context.getString(R.string.agmap_Ocean_Basemap_desc),
                    context.getString(R.string.agmap_Ocean_Basemap_url),
                    true);

            mapLayers.put(layer.getId(), layer);

            layer = new ArcGisMapLayer(
                    getId(),
                    context.getString(R.string.agmap_USA_Topo_Maps_name),
                    context.getString(R.string.agmap_USA_Topo_Maps_desc),
                    context.getString(R.string.agmap_USA_Topo_Maps_url),
                    true);

            mapLayers.put(layer.getId(), layer);

            layer = new ArcGisMapLayer(
                    getId(),
                    context.getString(R.string.agmap_World_Physical_Map_name),
                    context.getString(R.string.agmap_World_Physical_Map_desc),
                    context.getString(R.string.agmap_World_Physical_Map_url),
                    true);

            mapLayers.put(layer.getId(), layer);

            layer = new ArcGisMapLayer(
                    getId(),
                    context.getString(R.string.agmap_World_Shaded_Relief_name),
                    context.getString(R.string.agmap_World_Shaded_Relief_desc),
                    context.getString(R.string.agmap_World_Shaded_Relief_url),
                    true);

            mapLayers.put(layer.getId(), layer);

            layer = new ArcGisMapLayer(
                    getId(),
                    context.getString(R.string.agmap_World_Street_Map_name),
                    context.getString(R.string.agmap_World_Street_Map_desc),
                    context.getString(R.string.agmap_World_Street_Map_url),
                    true);

            mapLayers.put(layer.getId(), layer);

            layer = new ArcGisMapLayer(
                    getId(),
                    context.getString(R.string.agmap_World_Terrain_Base_name),
                    context.getString(R.string.agmap_World_Terrain_Base_desc),
                    context.getString(R.string.agmap_World_Terrain_Base_url),
                    true);

            mapLayers.put(layer.getId(), layer);

            layer = new ArcGisMapLayer(
                    getId(),
                    context.getString(R.string.agmap_World_Topo_Map_name),
                    context.getString(R.string.agmap_World_Topo_Map_desc),
                    context.getString(R.string.agmap_World_Topo_Map_url),
                    true);

            mapLayers.put(layer.getId(), layer);

            Global.Settings.DeviceSettings.setArcGisMayLayers(mapLayers.values());
        }
    }


    private static int getId() {
        if (idCounter == 0) {
            idCounter = Global.Settings.DeviceSettings.getArcGisIdCounter();
        }

        Global.Settings.DeviceSettings.setArcGisMapIdCounter(++idCounter);
        return idCounter;
    }



    public static Collection<ArcGisMapLayer> getLayers() {
        if (mapLayers == null) {
            init();
        }

        return mapLayers.values();
    }

    public static ArcGisMapLayer getMapLayer(int id) {
        if (mapLayers == null) {
            init();
        }

        return mapLayers.get(id);
    }

    public static Layer getBaseLayer(int id) {
        if (mapLayers == null) {
            init();
        }

        ArcGisMapLayer agml = mapLayers.get(id);
        Layer layer = null;

        UserCredentials credentials = null; //Global.Settings.DeviceSettings.getArcGisUserCredentials();

        if (agml.isOnline()) {
            layer = new ArcGISTiledMapServiceLayer(agml.getLocation(), credentials);
        } else {
            layer = new ArcGISLocalTiledLayer(agml.getLocation());
        }

        return layer;
    }


    public static void addOfflineLayer(String name, String description, String filePath) {
        addLayer(new ArcGisMapLayer(getId(), name, description, filePath, false));
    }

    public static void addOnlineLayer(String name, String description, String url) {
        addLayer(new ArcGisMapLayer(getId(), name, description, url, true));
    }

    private static void addLayer(ArcGisMapLayer layer) {
        if (mapLayers == null) {
            init();
        }

        mapLayers.put(layer.getId(), layer);
        Global.Settings.DeviceSettings.setArcGisMayLayers(mapLayers.values());
    }


    public static void removeLayer(int id) {
        if (mapLayers == null) {
            init();
        }

        mapLayers.remove(id);
        Global.Settings.DeviceSettings.setArcGisMayLayers(mapLayers.values());
    }



    public static Point pointToLatLng(double x, double y, MapView mapView) {
        return pointToLatLng(new Point(x, y), mapView);
    }

    public static Point pointToLatLng(Point point, MapView mapView) {
        return (Point) GeometryEngine.project(point, mapView.getSpatialReference(), LatLonSpatialReference);
    }

    public static Point latLngToMapSpatial(double lat, double lon, MapView mapView) {
        return latLngToMapSpatial(new Point(lon, lat), mapView);
    }

    public static Point latLngToMapSpatial(Point point, MapView mapView) {
        return (Point) GeometryEngine.project(point, LatLonSpatialReference, mapView.getSpatialReference());
    }

    public static Envelope getEnvelopFromLatLngExtents(Extent extents, MapView mapView) {
        return getEnvelopFromLatLngExtents(extents.getNorth(), extents.getEast(), extents.getSouth(), extents.getWest(), mapView);
    }

    public static Envelope getEnvelopFromLatLngExtents(double n, double e, double s, double w, MapView mapView) {

        Point ne = latLngToMapSpatial(n, e, mapView);
        Point sw = latLngToMapSpatial(s, w, mapView);

        return new Envelope(sw.getX(), sw.getY(), ne.getX(), ne.getY());
    }



    //static GeodatabaseSyncTask gdbSyncTask;

    /**
     * Create the GeodatabaseTask from the feature service URL w/o credentials.
     */
    public static void downloadOfflineMap(final String name, final String desc, String url, final String filePath,
                                          final Polygon extents, final SpatialReference spatialReference,
                                          final OfflineMapDownloadListener listener) {
        //Log.i(TAG, "Create GeoDatabase");
        // create a dialog to update user on progress
        //dialog = ProgressDialog.show(context, "Download Data", "Create local runtime geodatabase");
        //dialog.show();

        // create the GeodatabaseTask
        final GeodatabaseSyncTask gdbSyncTask = new GeodatabaseSyncTask(url, null);
        gdbSyncTask.fetchFeatureServiceInfo(new CallbackListener<FeatureServiceInfo>() {

            @Override
            public void onError(Throwable arg0) {
                if (listener != null) {
                    listener.onError("Error fetching FeatureServiceInfo");
                }
                //Log.e(TAG, "Error fetching FeatureServiceInfo");
            }

            @Override
            public void onCallback(FeatureServiceInfo fsInfo) {
                if (fsInfo.isSyncEnabled()) {
                    createGeodatabase(gdbSyncTask, fsInfo, name, desc, filePath, extents, spatialReference, listener);
                } else {
                    if (listener != null) {
                        listener.onError("Sync not enabled");
                    }
                }
            }
        });

    }

    /**
     * Set up parameters to pass the the submitTask() method. A
     * CallbackListener is used for the response.
     */
    private static void createGeodatabase(GeodatabaseSyncTask task, FeatureServiceInfo featureServerInfo, final String name, final String desc,
                                          final String filePath, Polygon extents, SpatialReference spatialReference, final OfflineMapDownloadListener listener) {
        // set up the parameters to generate a geodatabase
        GenerateGeodatabaseParameters params = new GenerateGeodatabaseParameters(featureServerInfo, extents, spatialReference);

        // a callback which fires when the task has completed or failed.
        CallbackListener<String> gdbResponseCallback = new CallbackListener<String>() {
            @Override
            public void onError(final Throwable e) {
                //Log.e(TAG, "Error creating geodatabase");
                //dialog.dismiss();

                if (listener != null) {
                    listener.onError("Error creating geodatabase");
                }
            }

            @Override
            public void onCallback(String path) {
                //Log.i(TAG, "Geodatabase is: " + path);
                //dialog.dismiss();
                // update map with local feature layer from geodatabase
                //updateFeatureLayer(path);

                addOfflineLayer(name, desc, path);

                if (listener != null) {
                    listener.onComplete(path);
                }
            }
        };

        // a callback which updates when the status of the task changes
        GeodatabaseStatusCallback statusCallback = new GeodatabaseStatusCallback() {
            @Override
            public void statusUpdated(GeodatabaseStatusInfo status) {
                //Log.i(TAG, status.getStatus().toString());

                if (listener != null) {
                    listener.onUpdate(status);
                }
            }
        };

        // create the fully qualified path for geodatabase file
        //localGdbFilePath = createGeodatabaseFilePath();

        // get geodatabase based on params
        //submitTask(params, filePath, statusCallback, gdbResponseCallback);
        task.generateGeodatabase(params, filePath, false, statusCallback, gdbResponseCallback);
    }


    /**
     * Request database, poll server to get status, and download the file
     */
//    private static void submitTask(GenerateGeodatabaseParameters params, String file,
//                                   GeodatabaseStatusCallback statusCallback, CallbackListener<String> gdbResponseCallback) {
//        // submit task
//        gdbSyncTask.generateGeodatabase(params, file, false, statusCallback, gdbResponseCallback);
//    }

    /**
     * Add feature layer from local geodatabase to map
     */
//        private void updateFeatureLayer(String featureLayerPath) {
//            // create a new geodatabase
//            Geodatabase localGdb = null;
//            try {
//                localGdb = new Geodatabase(featureLayerPath);
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            }
//
//            // Geodatabase contains GdbFeatureTables representing attribute data
//            // and/or spatial data. If GdbFeatureTable has geometry add it to
//            // the MapView as a Feature Layer
//            if (localGdb != null) {
//                for (GeodatabaseFeatureTable gdbFeatureTable : localGdb.getGeodatabaseTables()) {
//                    if (gdbFeatureTable.hasGeometry())
//                        mMapView.addLayer(new FeatureLayer(gdbFeatureTable));
//                }
//            }
//            // display the path to local geodatabase
//            //pathView.setText(featureLayerPath);
//
//        }



    public interface OfflineMapDownloadListener {
        void onError(String error);
        void onUpdate(GeodatabaseStatusInfo statusInfo);
        void onComplete(String filePath);
    }
}
