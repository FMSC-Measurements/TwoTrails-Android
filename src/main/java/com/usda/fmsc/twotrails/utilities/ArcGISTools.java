package com.usda.fmsc.twotrails.utilities;

import android.content.Context;
import android.content.DialogInterface;
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
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.TwoTrailApp;
import com.usda.fmsc.twotrails.objects.map.ArcGisMapLayer;
import com.usda.fmsc.utilities.Encryption;
import com.usda.fmsc.utilities.FileUtils;
import com.usda.fmsc.utilities.IListener;
import com.usda.fmsc.utilities.SerializationTools;
import com.usda.fmsc.utilities.StringEx;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class ArcGISTools {
    private static ArcGISTools _ArcGISTools;

    private TwoTrailApp TtAppCtx;
    
    private SpatialReference LatLonSpatialReference = SpatialReference.create(4326);

    private UserCredentials userCredentials;

    private HashMap<Integer, ArcGisMapLayer> mapLayers;
    private int idCounter = 0;

    private List<IArcToolsListener> listeners = new ArrayList<>();

    private HashMap<Integer, DownloadOfflineArcGISMapTask> tasks = new HashMap<>();
    private long lastUpdate = System.currentTimeMillis();

    private WebRequest webRequest;



    public ArcGISTools(TwoTrailApp context) {
        TtAppCtx = context;
        
        mapLayers = new HashMap<>();
        for (ArcGisMapLayer layer : TtAppCtx.getDeviceSettings().getArcGisMayLayers()) {
            mapLayers.put(layer.getId(), layer);
        }

        if (mapLayers.size() < 1) {
            createDefaultMaps();
        }
    }

    private void createDefaultMaps() {
        ArcGisMapLayer layer;

        layer = new ArcGisMapLayer(
                0,
                TtAppCtx.getString(R.string.agmap_World_Imagery_name),
                TtAppCtx.getString(R.string.agmap_World_Imagery_desc),
                TtAppCtx.getString(R.string.str_world_map),
                TtAppCtx.getString(R.string.agmap_World_Imagery_url),
                null,
                true);

        mapLayers.put(layer.getId(), layer);

        layer = new ArcGisMapLayer(
                getId(),
                TtAppCtx.getString(R.string.agmap_NatGeo_World_Map_name),
                TtAppCtx.getString(R.string.agmap_NatGeo_World_Map_desc),
                TtAppCtx.getString(R.string.str_world_map),
                TtAppCtx.getString(R.string.agmap_NatGeo_World_Map_url),
                null,
                true);

        mapLayers.put(layer.getId(), layer);

        layer = new ArcGisMapLayer(
                getId(),
                TtAppCtx.getString(R.string.agmap_NGS_Topo_US_2D_name),
                TtAppCtx.getString(R.string.agmap_NGS_Topo_US_2D_desc),
                TtAppCtx.getString(R.string.str_world_map),
                TtAppCtx.getString(R.string.agmap_NGS_Topo_US_2D_url),
                null,
                true);

        mapLayers.put(layer.getId(), layer);

        layer = new ArcGisMapLayer(
                getId(),
                TtAppCtx.getString(R.string.agmap_Ocean_Basemap_name),
                TtAppCtx.getString(R.string.agmap_Ocean_Basemap_desc),
                TtAppCtx.getString(R.string.str_world_map),
                TtAppCtx.getString(R.string.agmap_Ocean_Basemap_url),
                null,
                true);

        mapLayers.put(layer.getId(), layer);

        layer = new ArcGisMapLayer(
                getId(),
                TtAppCtx.getString(R.string.agmap_USA_Topo_Maps_name),
                TtAppCtx.getString(R.string.agmap_USA_Topo_Maps_desc),
                TtAppCtx.getString(R.string.str_world_map),
                TtAppCtx.getString(R.string.agmap_USA_Topo_Maps_url),
                null,
                true);

        mapLayers.put(layer.getId(), layer);

        layer = new ArcGisMapLayer(
                getId(),
                TtAppCtx.getString(R.string.agmap_World_Physical_Map_name),
                TtAppCtx.getString(R.string.agmap_World_Physical_Map_desc),
                TtAppCtx.getString(R.string.str_world_map),
                TtAppCtx.getString(R.string.agmap_World_Physical_Map_url),
                null,
                true);

        mapLayers.put(layer.getId(), layer);

        layer = new ArcGisMapLayer(
                getId(),
                TtAppCtx.getString(R.string.agmap_World_Shaded_Relief_name),
                TtAppCtx.getString(R.string.agmap_World_Shaded_Relief_desc),
                TtAppCtx.getString(R.string.str_world_map),
                TtAppCtx.getString(R.string.agmap_World_Shaded_Relief_url),
                null,
                true);

        mapLayers.put(layer.getId(), layer);

        layer = new ArcGisMapLayer(
                getId(),
                TtAppCtx.getString(R.string.agmap_World_Street_Map_name),
                TtAppCtx.getString(R.string.agmap_World_Street_Map_desc),
                TtAppCtx.getString(R.string.str_world_map),
                TtAppCtx.getString(R.string.agmap_World_Street_Map_url),
                null,
                true);

        mapLayers.put(layer.getId(), layer);

        layer = new ArcGisMapLayer(
                getId(),
                TtAppCtx.getString(R.string.agmap_World_Terrain_Base_name),
                TtAppCtx.getString(R.string.agmap_World_Terrain_Base_desc),
                TtAppCtx.getString(R.string.str_world_map),
                TtAppCtx.getString(R.string.agmap_World_Terrain_Base_url),
                null,
                true);

        mapLayers.put(layer.getId(), layer);

        layer = new ArcGisMapLayer(
                getId(),
                TtAppCtx.getString(R.string.agmap_World_Topo_Map_name),
                TtAppCtx.getString(R.string.agmap_World_Topo_Map_desc),
                TtAppCtx.getString(R.string.str_world_map),
                TtAppCtx.getString(R.string.agmap_World_Topo_Map_url),
                null,
                true);

        mapLayers.put(layer.getId(), layer);

        TtAppCtx.getDeviceSettings().setArcGisMayLayers(mapLayers.values());
    }

    public void reset() {
        idCounter = 0;
        TtAppCtx.getDeviceSettings().setArcGisMapIdCounter(idCounter);
        mapLayers = new HashMap<>();
        createDefaultMaps();
    }


    public void addListener(IArcToolsListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(IArcToolsListener listener) {
        if (listeners.contains(listener)) {
            listeners.remove(listener);
        }
    }



    private int getId() {
        if (idCounter == 0) {
            idCounter = TtAppCtx.getDeviceSettings().getArcGisIdCounter();
        }

        if (idCounter < mapLayers.size()) {
            idCounter = mapLayers.size();
        }

        while (mapLayers.containsKey(idCounter)) {
            idCounter++;
        }

        TtAppCtx.getDeviceSettings().setArcGisMapIdCounter(++idCounter);
        return idCounter;
    }


    public ArcGisMapLayer createMapLayer(String name, String description, String location, String uri, String filePath, boolean online) {
        return new ArcGisMapLayer(getId(), name, description, location, uri, filePath, online);
    }


    public Collection<ArcGisMapLayer> getMapLayers() {
        return mapLayers.values();
    }

    public ArcGisMapLayer getMapLayer(int id) {
        return mapLayers.get(id);
    }


    public Layer getBaseLayer(Context context, int id) throws FileNotFoundException {
        return getBaseLayer(context, mapLayers.get(id));
    }

    public Layer getBaseLayer(Context context, ArcGisMapLayer agml) throws FileNotFoundException {
        return getBaseLayer(context, agml, agml.isOnline());
    }

    public Layer getBaseLayer(Context context, ArcGisMapLayer agml, boolean isOnline) throws FileNotFoundException {
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


    public void addMapLayer(ArcGisMapLayer layer) {
        mapLayers.put(layer.getId(), layer);
        TtAppCtx.getDeviceSettings().setArcGisMayLayers(mapLayers.values());

        for (IArcToolsListener l : listeners) {
            if (l != null)
                l.arcLayerAdded(layer);
        }
    }

    public void deleteMapLayer(Context context, int id) {
        deleteMapLayer(context, id, false, null);
    }

    public void deleteMapLayer(Context context, int id, boolean askDeleteFile, final IListener event) {
        final ArcGisMapLayer layer = mapLayers.remove(id);
        TtAppCtx.getDeviceSettings().setArcGisMayLayers(mapLayers.values());

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

    public void updateMapLayer(ArcGisMapLayer layer) {
        if (mapLayers.containsKey(layer.getId())) {
            mapLayers.put(layer.getId(), layer);
            TtAppCtx.getDeviceSettings().setArcGisMayLayers(mapLayers.values());
        } else {
            throw new RuntimeException("Map does not exist to update.");
        }
    }



    public boolean offlineMapsAvailable() {
        for (ArcGisMapLayer agml : mapLayers.values()) {
            if (!agml.isOnline())
                return true;
        }

        return false;
    }


    public Point pointToLatLng(double x, double y, MapView mapView) {
        return pointToLatLng(new Point(x, y), mapView);
    }

    public Point pointToLatLng(Point point, MapView mapView) {
        return (Point) GeometryEngine.project(point, mapView.getSpatialReference(), LatLonSpatialReference);
    }

    public Point latLngToMapSpatial(double lat, double lon, MapView mapView) {
        return latLngToMapSpatial(new Point(lon, lat), mapView);
    }

    public Point latLngToMapSpatial(Point point, MapView mapView) {
        return (Point) GeometryEngine.project(point, LatLonSpatialReference, mapView.getSpatialReference());
    }

    public Envelope getEnvelopFromLatLngExtents(Extent extents, MapView mapView) {
        return getEnvelopFromLatLngExtents(extents.getNorth(), extents.getEast(), extents.getSouth(), extents.getWest(), mapView);
    }

    public Envelope getEnvelopFromLatLngExtents(double n, double e, double s, double w, MapView mapView) {

        Point ne = latLngToMapSpatial(n, e, mapView);
        Point sw = latLngToMapSpatial(s, w, mapView);

        return new Envelope(sw.getX(), sw.getY(), ne.getX(), ne.getY());
    }



    public String getOfflineUrlFromOnlineUrl(String onlineUrl) {
        if (onlineUrl.contains("services.arcgisonline")) {
            onlineUrl = onlineUrl.replace("services.arcgisonline", "tiledbasemaps.arcgis");
        }

        return onlineUrl;
    }

    public void getLayerFromUrl(final String url, Context context, final IGetArcMapLayerListener listener) {
        if (listener == null)
            throw new RuntimeException("PointMediaListener is null");

        String jUrl = url;

        if (jUrl.contains("tiledbasemaps.arcgis")) {
            jUrl = url.replace("tiledbasemaps.arcgis", "services.arcgisonline");
        }

        if (!jUrl.endsWith("?f=pjson")) {
            jUrl = String.format("%s?f=pjson", jUrl);
        }

        if (webRequest == null)
            webRequest = new WebRequest(context);

        webRequest.getJson(jUrl, new Response.Listener<JSONObject>() {
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

    private ArcGisMapLayer.DetailLevel[] getDetailLevelsFromJson(JSONObject jobj) {
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



    public void startOfflineMapDownload(DownloadOfflineArcGISMapTask task) {
        final ArcGisMapLayer layer = task.getLayer();

        if (tasks.containsKey(task.getLayer().getId())) {
            throw new RuntimeException("DownloadOfflineArcGISMapTask already submitted.");
        }

        tasks.put(task.getLayer().getId(), task);

        TtAppCtx.getTtNotifyManager().startMapDownload(layer.getId(), layer.getName());

        task.startDownload(new DownloadOfflineArcGISMapTask.DownloadListener() {
            @Override
            public void onMapDownloaded(final ArcGisMapLayer layer) {
                addMapLayer(layer);

//                Global.getMainActivity().runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Global.TtNotifyManager.endMapDownload(layer.getId());
//                    }
//                });
                TtAppCtx.getTtNotifyManager().endMapDownload(layer.getId());

//                final Activity activity = Global.getCurrentActivity();
//                if (activity != null) {
//                    activity.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Toast.makeText(Global.getApplicationContext(), String.format("%s Downloaded", layer.getName()), Toast.LENGTH_LONG).show();
//                        }
//                    });
//                }
                Toast.makeText(TtAppCtx, String.format("%s Downloaded", layer.getName()), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onTaskUpdate(final ExportTileCacheStatus status) {
                if (status.getStatus() == GPJobResource.JobStatus.EXECUTING) {
                    long now = System.currentTimeMillis();

                    if (now > lastUpdate + 250) {
                        final int progress = status.getDownloadSize() > 0 ? (int) (100 * status.getTotalBytesDownloaded() / status.getDownloadSize()) : 0;

//                        Global.getMainActivity().runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                Global.TtNotifyManager.updateMapDownload(layer.getId(), progress);
//                            }
//                        });
                        TtAppCtx.getTtNotifyManager().updateMapDownload(layer.getId(), progress);

                        lastUpdate = now;
                    }
                } else if (status.getStatus() == GPJobResource.JobStatus.FAILED) {
//                    Global.getMainActivity().runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Global.TtNotifyManager.endMapDownload(layer.getId());
//                        }
//                    });
                    TtAppCtx.getTtNotifyManager().endMapDownload(layer.getId());

//                    final Activity activity = Global.getCurrentActivity();

//                    if (activity != null) {
//                        activity.runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                Toast.makeText(activity, "Failed to download offline map", Toast.LENGTH_LONG).show();
//                            }
//                        });
//                    }
                    Toast.makeText(TtAppCtx, "Failed to download offline map", Toast.LENGTH_LONG).show();
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

//                Global.getMainActivity().runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Global.TtNotifyManager.endMapDownload(layer.getId());
//                    }
//                });
                TtAppCtx.getTtNotifyManager().endMapDownload(layer.getId());

//                final Activity activity = Global.getCurrentActivity();
//
//                if (activity != null) {
//                    activity.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Toast.makeText(activity, "Error creating offline map", Toast.LENGTH_LONG).show();
//                        }
//                    });
//                }
                Toast.makeText(TtAppCtx, "Error creating offline map", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onDownloadError(String message) {
                tasks.remove(layer.getId());
//                Global.getMainActivity().runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Global.TtNotifyManager.endMapDownload(layer.getId());
//                    }
//                });
                TtAppCtx.getTtNotifyManager().endMapDownload(layer.getId());

//                final Activity activity = Global.getCurrentActivity();
//
//                if (activity != null) {
//                    activity.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Toast.makeText(activity, "Error downloading offline map", Toast.LENGTH_LONG).show();
//                        }
//                    });
//                }
                Toast.makeText(TtAppCtx, "Error downloading offline map", Toast.LENGTH_LONG).show();
            }
        });
    }





    public boolean hasValidCredentials(Context context) {
        UserCredentials creds = getCredentials(context);

        if (creds != null) {
            long expire = creds.getTokenExpiry();
            long now = System.currentTimeMillis();

            return expire == 0 || expire > now;
        }
        return false;
    }

    public boolean hasCredentials(Context context) {
        return getCredentials(context) != null;
    }

    public UserCredentials getCredentials(Context context) {
        String credStr = TtAppCtx.getDeviceSettings().getArcCredentials();

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
                TtAppCtx.getReport().writeError("ArcGISTools:getCredentials", e.getMessage(), e.getStackTrace());
            }
        }

        return null;
    }

    public boolean saveCredentials(Context context, UserCredentials credentials) {
        if (credentials == null)
            throw new NullPointerException();

        try {
            byte[] data = SerializationTools.objectToBytes(credentials);

            byte[] encoded = Encryption.encodeFile(AndroidUtils.Device.getDeviceID(context), data);

            userCredentials = credentials;

            TtAppCtx.getDeviceSettings().setArcCredentials(Base64.encodeToString(encoded, Base64.DEFAULT));

            return true;
        } catch (Exception e) {
            TtAppCtx.getReport().writeError("ArcGISTools:setCredentials", e.getMessage(), e.getStackTrace());
        }

        return false;
    }

    public void deleteCredentials() {
        TtAppCtx.getDeviceSettings().setArcCredentials(StringEx.Empty);
        userCredentials = null;
    }

    public boolean areCredentialsOutOfDate(Context context) {
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
