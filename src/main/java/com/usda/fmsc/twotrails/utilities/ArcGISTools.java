package com.usda.fmsc.twotrails.utilities;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import com.esri.arcgisruntime.data.TileCache;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.GeographicTransformation;
import com.esri.arcgisruntime.geometry.GeographicTransformationStep;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.usda.fmsc.android.utilities.WebRequest;
import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.TwoTrailsApp;
import com.usda.fmsc.twotrails.objects.map.ArcGisMapLayer;
import com.usda.fmsc.utilities.FileUtils;
import com.usda.fmsc.utilities.IListener;
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

    private TwoTrailsApp TtAppCtx;

    private SpatialReference LatLonSpatialReference = SpatialReferences.getWgs84();
    private GeographicTransformation LatLonSpatialTransformation = GeographicTransformation.create(GeographicTransformationStep.create(15855));

    //private UserCredentials userCredentials;

    private HashMap<Integer, ArcGisMapLayer> mapLayers;
    private int idCounter = 0;

    private List<IArcToolsListener> listeners = new ArrayList<>();

    //private HashMap<Integer, DownloadOfflineArcGISMapTask> tasks = new HashMap<>();
    private long lastUpdate = System.currentTimeMillis();

    private WebRequest webRequest;



    public ArcGISTools(TwoTrailsApp context) {
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
        listeners.remove(listener);
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


    public ArcGISMap getBaseLayer(Context context, int id) throws FileNotFoundException {
        return getBaseLayer(context, mapLayers.get(id));
    }

    public ArcGISMap getBaseLayer(Context context, ArcGisMapLayer agml) throws FileNotFoundException {
        return getBaseLayer(context, agml, agml.isOnline());
    }

    public ArcGISMap getBaseLayer(Context context, ArcGisMapLayer agml, boolean isOnline) throws FileNotFoundException {
        ArcGISTiledLayer tiledLayer = null;

        if (isOnline) {
            tiledLayer = new ArcGISTiledLayer(agml.getUrl());
        } else {
            if (FileUtils.fileOrFolderExists(agml.getFilePath())) {
                try {
                    TileCache tileCache = new TileCache(agml.getFilePath());
                    tiledLayer = new ArcGISTiledLayer(tileCache);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                throw new FileNotFoundException("Tile Package Not Found");
            }
        }

        return new ArcGISMap(new Basemap(tiledLayer));
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
                    .setPositiveButton(R.string.str_yes, (dialog, which) -> {
                        String filename = layer.getFilePath();

                        if (!StringEx.isEmpty(filename)) {
                            FileUtils.delete(filename);
                        }

                        if (event != null)
                            event.onEventTriggered(null);
                    })
                    .setNegativeButton(R.string.str_no, (dialog, which) -> {
                        if (event != null)
                            event.onEventTriggered(null);
                    })
                    .show();

        } else {
            if (event != null)
                event.onEventTriggered(null);
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


    public Point mapPointToLatLng(int x, int y, MapView mapView) {
        return mapPointToLatLng(new android.graphics.Point(x, y), mapView);
    }

    public Point mapPointToLatLng(android.graphics.Point screenPoint, MapView mapView) {
        // create a map point from screen point
        // convert to WGS84 for lat/lon format
        return (Point) GeometryEngine.project(mapView.screenToLocation(screenPoint), SpatialReferences.getWgs84());
    }

//    public Point latLngToMapSpatial(double lat, double lon, MapView mapView) {
//        return latLngToMapSpatial(new Point(lon, lat), mapView);
//    }
//
//    public Point latLngToMapSpatial(Point point, MapView mapView) {
//        //android.graphics.Point p = mapView.locationToScreen(point);
//        //return new Point(p.x, p.y);
//        return (Point) GeometryEngine.project(point, mapView.getSpatialReference(), LatLonSpatialTransformation);
//        //return (Point) GeometryEngine.project(mapView.locationToScreen(point), SpatialReferences.getWgs84());
//    }

    public Envelope getEnvelopFromLatLngExtents(Extent extents, MapView mapView) {
        return getEnvelopFromLatLngExtents(extents.getNorth(), extents.getEast(), extents.getSouth(), extents.getWest(), mapView);
    }

    public Envelope getEnvelopFromLatLngExtents(double n, double e, double s, double w, MapView mapView) {

        //Point ne = latLngToMapSpatial(n, e, mapView);
        //Point sw = latLngToMapSpatial(s, w, mapView);

        return new Envelope(w, s, e, n, LatLonSpatialReference);
//        return new Envelope(sw.getX(), sw.getY(), ne.getX(), ne.getY(), LatLonSpatialReference);
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

        webRequest.getJson(jUrl, response -> {
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
        },
        error -> listener.onBadUrl(error.getMessage()));
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

        return detailLevels.toArray(new ArcGisMapLayer.DetailLevel[0]);
    }



//    public void startOfflineMapDownload(DownloadOfflineArcGISMapTask task) {
//        final ArcGisMapLayer layer = task.getLayer();
//
//        if (tasks.containsKey(task.getLayer().getId())) {
//            throw new RuntimeException("DownloadOfflineArcGISMapTask already submitted.");
//        }
//
//        tasks.put(task.getLayer().getId(), task);
//
//        TtAppCtx.getTtNotifyManager().startMapDownload(layer.getId(), layer.getName());
//
//        task.startDownload(new DownloadOfflineArcGISMapTask.DownloadListener() {
//            @Override
//            public void onMapDownloaded(final ArcGisMapLayer layer) {
//                addMapLayer(layer);
//                TtAppCtx.getTtNotifyManager().endMapDownload(layer.getId());
//                Toast.makeText(TtAppCtx, String.format("%s Downloaded", layer.getName()), Toast.LENGTH_LONG).show();
//            }
//
//            @Override
//            public void onTaskUpdate(final ExportTileCacheStatus status) {
//                if (status.getStatus() == GPJobResource.JobStatus.EXECUTING) {
//                    long now = System.currentTimeMillis();
//
//                    if (now > lastUpdate + 250) {
//                        final int progress = status.getDownloadSize() > 0 ? (int) (100 * status.getTotalBytesDownloaded() / status.getDownloadSize()) : 0;
//                        TtAppCtx.getTtNotifyManager().updateMapDownload(layer.getId(), progress);
//
//                        lastUpdate = now;
//                    }
//                } else if (status.getStatus() == GPJobResource.JobStatus.FAILED) {
//                    TtAppCtx.getTtNotifyManager().endMapDownload(layer.getId());
//                    Toast.makeText(TtAppCtx, "Failed to download offline map", Toast.LENGTH_LONG).show();
//                }
//            }
//
//            @Override
//            public void onStatusError(String message) {
//                tasks.remove(layer.getId());
//                TtAppCtx.getTtNotifyManager().endMapDownload(layer.getId());
//                Toast.makeText(TtAppCtx, "Error creating offline map", Toast.LENGTH_LONG).show();
//            }
//
//            @Override
//            public void onDownloadError(String message) {
//                tasks.remove(layer.getId());
//                TtAppCtx.getTtNotifyManager().endMapDownload(layer.getId());
//                Toast.makeText(TtAppCtx, "Error downloading offline map", Toast.LENGTH_LONG).show();
//            }
//        });
//    }





//    public boolean hasValidCredentials(Context context) {
//        UserCredentials creds = getCredentials(context);
//
//        if (creds != null) {
//            long expire = creds.getTokenExpiry();
//            long now = System.currentTimeMillis();
//
//            return expire == 0 || expire > now;
//        }
//        return false;
//    }

//    public boolean hasCredentials(Context context) {
//        return getCredentials(context) != null;
//    }

//    public UserCredentials getCredentials(Context context) {
//        String credStr = TtAppCtx.getDeviceSettings().getArcCredentials();
//
//        if (userCredentials != null)
//            return userCredentials;
//
//        userCredentials = new UserCredentials();
//
//        if (!StringEx.isEmpty(credStr)) {
//            try {
//                byte[] data = Base64.decode(credStr, Base64.DEFAULT);
//
//                byte[] decoded = Encryption.decodeFile(AndroidUtils.Device.getDeviceID(context), data);
//
//                userCredentials = (UserCredentials) SerializationTools.bytesToObject(decoded);
//                return userCredentials;
//            } catch (Exception e) {
//                TtAppCtx.getReport().writeError("ArcGISTools:getCredentials", e.getMessage(), e.getStackTrace());
//            }
//        }
//
//        return null;
//    }

//    public boolean saveCredentials(Context context, UserCredentials credentials) {
//        if (credentials == null)
//            throw new NullPointerException();
//
//        try {
//            byte[] data = SerializationTools.objectToBytes(credentials);
//
//            byte[] encoded = Encryption.encodeFile(AndroidUtils.Device.getDeviceID(context), data);
//
//            userCredentials = credentials;
//
//            TtAppCtx.getDeviceSettings().setArcCredentials(Base64.encodeToString(encoded, Base64.DEFAULT));
//
//            return true;
//        } catch (Exception e) {
//            TtAppCtx.getReport().writeError("ArcGISTools:setCredentials", e.getMessage(), e.getStackTrace());
//        }
//
//        return false;
//    }

//    public void deleteCredentials() {
//        TtAppCtx.getDeviceSettings().setArcCredentials(StringEx.Empty);
//        userCredentials = null;
//    }

//    public boolean areCredentialsOutOfDate(Context context) {
//        UserCredentials creds = getCredentials(context);
//
//        return creds != null && creds.getTokenExpiry() < System.currentTimeMillis();
//    }






    public interface IGetArcMapLayerListener {
        void onComplete(ArcGisMapLayer layer);
        void onBadUrl(String error);
    }

    public interface IArcToolsListener {
        void arcLayerAdded(ArcGisMapLayer layer);
    }
}
