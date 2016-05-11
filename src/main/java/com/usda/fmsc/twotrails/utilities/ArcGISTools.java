package com.usda.fmsc.twotrails.utilities;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.util.Base64;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.esri.android.map.Layer;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISLocalTiledLayer;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.io.UserCredentials;
import com.esri.core.tasks.ags.geoprocessing.GPJobResource;
import com.esri.core.tasks.tilecache.ExportTileCacheStatus;
import com.usda.fmsc.android.AndroidUtils;
import com.usda.fmsc.android.utilities.WebRequest;
import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.objects.map.ArcGisMapLayer;
import com.usda.fmsc.utilities.Encryption;
import com.usda.fmsc.utilities.FileUtils;
import com.usda.fmsc.utilities.IListener;
import com.usda.fmsc.utilities.SerializationTools;
import com.usda.fmsc.utilities.StringEx;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class ArcGISTools {
    private static SpatialReference LatLonSpatialReference = SpatialReference.create(4326);

    private static UserCredentials userCredentials;

    private static HashMap<Integer, ArcGisMapLayer> mapLayers;
    private static int idCounter = 0;

    private static List<IArcToolsListener> listeners = new ArrayList<>();

    private static HashMap<Integer, DownloadOfflineArcGISMapTask> tasks = new HashMap<>();
    private static long lastUpdate = System.currentTimeMillis();


    private static void init() {
        mapLayers = new HashMap<>();
        for (ArcGisMapLayer layer : Global.Settings.DeviceSettings.getArcGisMayLayers()) {
            mapLayers.put(layer.getId(), layer);
        }

        if (mapLayers.size() < 1) {
            createDefaultMaps();
        }
    }

    private static void createDefaultMaps() {
        final Context context = Global.getApplicationContext();

        ArcGisMapLayer layer;

        layer = new ArcGisMapLayer(
                0,
                context.getString(R.string.agmap_World_Imagery_name),
                context.getString(R.string.agmap_World_Imagery_desc),
                context.getString(R.string.str_world_map),
                context.getString(R.string.agmap_World_Imagery_url),
                null,
                true);

        mapLayers.put(layer.getId(), layer);

        layer = new ArcGisMapLayer(
                getId(),
                context.getString(R.string.agmap_NatGeo_World_Map_name),
                context.getString(R.string.agmap_NatGeo_World_Map_desc),
                context.getString(R.string.str_world_map),
                context.getString(R.string.agmap_NatGeo_World_Map_url),
                null,
                true);

        mapLayers.put(layer.getId(), layer);

        layer = new ArcGisMapLayer(
                getId(),
                context.getString(R.string.agmap_NGS_Topo_US_2D_name),
                context.getString(R.string.agmap_NGS_Topo_US_2D_desc),
                context.getString(R.string.str_world_map),
                context.getString(R.string.agmap_NGS_Topo_US_2D_url),
                null,
                true);

        mapLayers.put(layer.getId(), layer);

        layer = new ArcGisMapLayer(
                getId(),
                context.getString(R.string.agmap_Ocean_Basemap_name),
                context.getString(R.string.agmap_Ocean_Basemap_desc),
                context.getString(R.string.str_world_map),
                context.getString(R.string.agmap_Ocean_Basemap_url),
                null,
                true);

        mapLayers.put(layer.getId(), layer);

        layer = new ArcGisMapLayer(
                getId(),
                context.getString(R.string.agmap_USA_Topo_Maps_name),
                context.getString(R.string.agmap_USA_Topo_Maps_desc),
                context.getString(R.string.str_world_map),
                context.getString(R.string.agmap_USA_Topo_Maps_url),
                null,
                true);

        mapLayers.put(layer.getId(), layer);

        layer = new ArcGisMapLayer(
                getId(),
                context.getString(R.string.agmap_World_Physical_Map_name),
                context.getString(R.string.agmap_World_Physical_Map_desc),
                context.getString(R.string.str_world_map),
                context.getString(R.string.agmap_World_Physical_Map_url),
                null,
                true);

        mapLayers.put(layer.getId(), layer);

        layer = new ArcGisMapLayer(
                getId(),
                context.getString(R.string.agmap_World_Shaded_Relief_name),
                context.getString(R.string.agmap_World_Shaded_Relief_desc),
                context.getString(R.string.str_world_map),
                context.getString(R.string.agmap_World_Shaded_Relief_url),
                null,
                true);

        mapLayers.put(layer.getId(), layer);

        layer = new ArcGisMapLayer(
                getId(),
                context.getString(R.string.agmap_World_Street_Map_name),
                context.getString(R.string.agmap_World_Street_Map_desc),
                context.getString(R.string.str_world_map),
                context.getString(R.string.agmap_World_Street_Map_url),
                null,
                true);

        mapLayers.put(layer.getId(), layer);

        layer = new ArcGisMapLayer(
                getId(),
                context.getString(R.string.agmap_World_Terrain_Base_name),
                context.getString(R.string.agmap_World_Terrain_Base_desc),
                context.getString(R.string.str_world_map),
                context.getString(R.string.agmap_World_Terrain_Base_url),
                null,
                true);

        mapLayers.put(layer.getId(), layer);

        layer = new ArcGisMapLayer(
                getId(),
                context.getString(R.string.agmap_World_Topo_Map_name),
                context.getString(R.string.agmap_World_Topo_Map_desc),
                context.getString(R.string.str_world_map),
                context.getString(R.string.agmap_World_Topo_Map_url),
                null,
                true);

        mapLayers.put(layer.getId(), layer);

        Global.Settings.DeviceSettings.setArcGisMayLayers(mapLayers.values());
    }

    public static void reset() {
        idCounter = 0;
        Global.Settings.DeviceSettings.setArcGisMapIdCounter(idCounter);
        mapLayers = new HashMap<>();
        createDefaultMaps();
    }



    public static void addListener(IArcToolsListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static void removeListener(IArcToolsListener listener) {
        if (listeners.contains(listener)) {
            listeners.remove(listener);
        }
    }



    private static int getId() {
        if (mapLayers == null) {
            init();
        }

        if (idCounter == 0) {
            idCounter = Global.Settings.DeviceSettings.getArcGisIdCounter();
        }

        if (idCounter < mapLayers.size()) {
            idCounter = mapLayers.size();
        }

        while (mapLayers.containsKey(idCounter)) {
            idCounter++;
        }

        Global.Settings.DeviceSettings.setArcGisMapIdCounter(++idCounter);
        return idCounter;
    }


    public static ArcGisMapLayer createMapLayer(String name, String description, String location, String uri, String filePath, boolean online) {
        return new ArcGisMapLayer(getId(), name, description, location, uri, filePath, online);
    }


    public static Collection<ArcGisMapLayer> getMapLayers() {
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


    public static Layer getBaseLayer(Context context, int id) throws FileNotFoundException {
        if (mapLayers == null) {
            init();
        }

        return getBaseLayer(context, mapLayers.get(id));
    }

    public static Layer getBaseLayer(Context context, ArcGisMapLayer agml) throws FileNotFoundException {
        return getBaseLayer(context, agml, agml.isOnline());
    }

    public static Layer getBaseLayer(Context context, ArcGisMapLayer agml, boolean isOnline) throws FileNotFoundException {
        Layer layer = null;

        if (isOnline) {
            layer = new ArcGISTiledMapServiceLayer(agml.getUrl(),
                    agml.getUrl().contains("services.arcgisonline") ?
                            null: getCredentials(context)
            );
        } else {
            if (FileUtils.fileOrFolderExists(agml.getFilePath())) {
                try {
                    layer = new ArcGISLocalTiledLayer(agml.getFilePath());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                throw new FileNotFoundException("Tile Package Not Found");
            }
        }

        return layer;
    }


    public static void addMapLayer(ArcGisMapLayer layer) {
        if (mapLayers == null) {
            init();
        }

        mapLayers.put(layer.getId(), layer);
        Global.Settings.DeviceSettings.setArcGisMayLayers(mapLayers.values());

        for (IArcToolsListener l : listeners) {
            if (l != null)
                l.arcLayerAdded(layer);
        }
    }

    public static void deleteMapLayer(Context context, int id) {
        deleteMapLayer(context, id, false, null);
    }

    public static void deleteMapLayer(Context context, int id, boolean askDeleteFile, final IListener event) {
        if (mapLayers == null) {
            init();
        }

        final ArcGisMapLayer layer = mapLayers.remove(id);
        Global.Settings.DeviceSettings.setArcGisMayLayers(mapLayers.values());

        if (askDeleteFile && layer != null && !layer.isOnline()) {
            new AlertDialog.Builder(context)
                    .setMessage("Would you like to delete the offline map file as well?")
                    .setPositiveButton(R.string.str_yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String filename = layer.getFilePath();

                            if (!StringEx.isEmpty(filename)) {
                                FileUtils.delete(filename);
                            }

                            if (event != null)
                                event.onEventTriggerd(null);
                        }
                    })
                    .setNegativeButton(R.string.str_no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (event != null)
                                event.onEventTriggerd(null);
                        }
                    })
                    .show();

        } else {
            if (event != null)
                event.onEventTriggerd(null);
        }
    }

    public static void updateMapLayer(ArcGisMapLayer layer) {
        if (mapLayers == null) {
            init();
        }

        if (mapLayers.containsKey(layer.getId())) {
            mapLayers.put(layer.getId(), layer);
            Global.Settings.DeviceSettings.setArcGisMayLayers(mapLayers.values());
        } else {
            throw new RuntimeException("Map does not exist to update.");
        }
    }



    public static boolean offlineMapsAvailable() {
        if (mapLayers == null) {
            init();
        }

        for (ArcGisMapLayer agml : mapLayers.values()) {
            if (!agml.isOnline())
                return true;
        }

        return false;
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



    public static String getOfflineUrlFromOnlineUrl(String onlineUrl) {
        if (onlineUrl.contains("services.arcgisonline")) {
            onlineUrl = onlineUrl.replace("services.arcgisonline", "tiledbasemaps.arcgis");
        }

        return onlineUrl;
    }

    public static void getLayerFromUrl(final String url, Context context, final IGetArcMapLayerListener listener) {
        if (listener == null)
            throw new RuntimeException("Listener is null");

        String jUrl = url;

        if (jUrl.contains("tiledbasemaps.arcgis")) {
            jUrl = url.replace("tiledbasemaps.arcgis", "services.arcgisonline");
        }

        if (!jUrl.endsWith("?f=pjson")) {
            jUrl = String.format("%s?f=pjson", jUrl);
        }

        WebRequest.getJson(jUrl, context, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                if (response.has("currentVersion")) {
                    listener.onComplete(new ArcGisMapLayer(
                                    getId(),
                                    null,
                                    response.optString("description", StringEx.Empty),
                                    null,
                                    url,
                                    null,
                                    response.optDouble("minScale", 0),
                                    response.optDouble("maxScale", 0),
                                    getDetailLevelsFromJson(response),
                                    null,
                                    false
                            )
                    );
                } else {
                    String message = "invalid json";

                    if (response.has("message")) {
                        message = response.optString("message", "error");
                    }

                    listener.onBadUrl(message);
                }
            }
        },
        new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.onBadUrl(error.getMessage());
            }
        });
    }

    private static ArcGisMapLayer.DetailLevel[] getDetailLevelsFromJson(JSONObject jobj) {
        ArrayList<ArcGisMapLayer.DetailLevel> detailLevels = new ArrayList<>();

        try {
            if (jobj.has("tileInfo")) {
                jobj = jobj.getJSONObject("tileInfo");

                if (jobj.has("lods")) {
                    try {
                        JSONArray array = jobj.getJSONArray("lods");
                        JSONObject o;

                        int level;
                        double res, scale;

                        for (int i = 0; i < array.length(); i++) {
                            o = array.getJSONObject(i);

                            level = o.getInt("level");
                            res = o.getDouble("resolution");
                            scale = o.getDouble("scale");

                            detailLevels.add(new ArcGisMapLayer.DetailLevel(level, res, scale));
                        }
                    } catch (JSONException e) {
                        //
                    }
                }
            }
        } catch (JSONException e) {
            //
        }

        return detailLevels.toArray(new ArcGisMapLayer.DetailLevel[detailLevels.size()]);
    }



    public static void startOfflineMapDownload(DownloadOfflineArcGISMapTask task) {
        final ArcGisMapLayer layer = task.getLayer();

        if (tasks.containsKey(task.getLayer().getId())) {
            throw new RuntimeException("DownloadOfflineArcGISMapTask already submitted.");
        }

        tasks.put(task.getLayer().getId(), task);

        Global.TtNotifyManager.startMapDownload(layer.getId(), layer.getName());

        task.startDownload(new DownloadOfflineArcGISMapTask.DownloadListener() {
            @Override
            public void onMapDownloaded(final ArcGisMapLayer layer) {
                addMapLayer(layer);

                Global.getMainActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Global.TtNotifyManager.endMapDownload(layer.getId());
                    }
                });

                final Activity activity = Global.getCurrentActivity();
                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(Global.getApplicationContext(), String.format("%s Downloaded", layer.getName()), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }

            @Override
            public void onTaskUpdate(final ExportTileCacheStatus status) {
                if (status.getStatus() == GPJobResource.JobStatus.EXECUTING) {
                    long now = System.currentTimeMillis();

                    if (now > lastUpdate + 250) {
                        final int progress = status.getDownloadSize() > 0 ? (int) (100 * status.getTotalBytesDownloaded() / status.getDownloadSize()) : 0;

                        Global.getMainActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Global.TtNotifyManager.updateMapDownload(layer.getId(), progress);
                            }
                        });

                        lastUpdate = now;
                    }
                } else if (status.getStatus() == GPJobResource.JobStatus.FAILED) {
                    Global.getMainActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Global.TtNotifyManager.endMapDownload(layer.getId());
                        }
                    });

                    final Activity activity = Global.getCurrentActivity();

                    if (activity != null) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(activity, "Failed to download offline map", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                } //else {
//                    final Activity activity = Global.getCurrentActivity();
//
//                    if (activity != null) {
//                        activity.runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                Toast.makeText(activity, status.getStatus().toString(), Toast.LENGTH_LONG).show();
//                            }
//                        });
//                    }
//                }
            }

            @Override
            public void onStatusError(String message) {
                tasks.remove(layer.getId());

                Global.getMainActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Global.TtNotifyManager.endMapDownload(layer.getId());
                    }
                });

                final Activity activity = Global.getCurrentActivity();

                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(activity, "Error creating offline map", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }

            @Override
            public void onDownloadError(String message) {
                tasks.remove(layer.getId());
                Global.getMainActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Global.TtNotifyManager.endMapDownload(layer.getId());
                    }
                });

                final Activity activity = Global.getCurrentActivity();

                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(Global.getApplicationContext(), "Error downloading offline map", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }





    public static boolean hasValidCredentials(Context context) {
        UserCredentials creds = getCredentials(context);

        if (creds != null) {
            long expire = creds.getTokenExpiry();
            long now = System.currentTimeMillis();

            return expire == 0 || expire > now;
        }
        return false;
    }

    public static boolean hasCredentials(Context context) {
        return getCredentials(context) != null;
    }

    public static UserCredentials getCredentials(Context context) {
        String credStr = Global.Settings.DeviceSettings.getArcCredentials();

        if (userCredentials != null)
            return userCredentials;

        userCredentials = new UserCredentials();

        if (!StringEx.isEmpty(credStr)) {
            try {
                byte[] data = Base64.decode(credStr, Base64.DEFAULT);

                byte[] decoded = Encryption.decodeFile(AndroidUtils.Device.getDeviceID(context), data);

                userCredentials = (UserCredentials) SerializationTools.bytesToObject(decoded);
                return userCredentials;
            } catch (Exception e) {
                TtUtils.TtReport.writeError("ArcGISTools:getCredentials", e.getMessage(), e.getStackTrace());
            }
        }

        return null;
    }

    public static boolean saveCredentials(Context context, UserCredentials credentials) {
        if (credentials == null)
            throw new NullPointerException();

        try {
            byte[] data = SerializationTools.objectToBytes(credentials);

            byte[] encoded = Encryption.encodeFile(AndroidUtils.Device.getDeviceID(context), data);

            userCredentials = credentials;

            Global.Settings.DeviceSettings.setArcCredentials(Base64.encodeToString(encoded, Base64.DEFAULT));

            return true;
        } catch (Exception e) {
            TtUtils.TtReport.writeError("ArcGISTools:setCredentials", e.getMessage(), e.getStackTrace());
        }

        return false;
    }

    public static void deleteCredentials() {
        Global.Settings.DeviceSettings.setArcCredentials(StringEx.Empty);
        userCredentials = null;
    }

    public static boolean areCredentialsOutOfDate(Context context) {
        UserCredentials creds = getCredentials(context);

        return creds != null && creds.getTokenExpiry() < System.currentTimeMillis();
    }






    public interface IGetArcMapLayerListener {
        void onComplete(ArcGisMapLayer layer);
        void onBadUrl(String error);
    }

    public interface IArcToolsListener {
        void arcLayerAdded(ArcGisMapLayer layer);
    }
}
