package com.usda.fmsc.twotrails.utilities;

import android.content.Context;

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
import com.usda.fmsc.android.utilities.WebRequest;
import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.objects.map.ArcGisMapLayer;
import com.usda.fmsc.utilities.StringEx;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class ArcGISTools {
    private static SpatialReference LatLonSpatialReference = SpatialReference.create(4326);

    private static HashMap<Integer, ArcGisMapLayer> mapLayers;
    private static int idCounter = 0;

    private static List<IArcToolsListener> listeners = new ArrayList<>();


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
                    context.getString(R.string.str_world_map),
                    context.getString(R.string.agmap_World_Imagery_url),
                    true);

            mapLayers.put(layer.getId(), layer);

            layer = new ArcGisMapLayer(
                getId(),
                context.getString(R.string.agmap_NatGeo_World_Map_name),
                context.getString(R.string.agmap_NatGeo_World_Map_desc),
                context.getString(R.string.str_world_map),
                context.getString(R.string.agmap_NatGeo_World_Map_url),
                true);

            mapLayers.put(layer.getId(), layer);

            layer = new ArcGisMapLayer(
                    getId(),
                    context.getString(R.string.agmap_NGS_Topo_US_2D_name),
                    context.getString(R.string.agmap_NGS_Topo_US_2D_desc),
                    context.getString(R.string.str_world_map),
                    context.getString(R.string.agmap_NGS_Topo_US_2D_url),
                    true);

            mapLayers.put(layer.getId(), layer);

            layer = new ArcGisMapLayer(
                    getId(),
                    context.getString(R.string.agmap_Ocean_Basemap_name),
                    context.getString(R.string.agmap_Ocean_Basemap_desc),
                    context.getString(R.string.str_world_map),
                    context.getString(R.string.agmap_Ocean_Basemap_url),
                    true);

            mapLayers.put(layer.getId(), layer);

            layer = new ArcGisMapLayer(
                    getId(),
                    context.getString(R.string.agmap_USA_Topo_Maps_name),
                    context.getString(R.string.agmap_USA_Topo_Maps_desc),
                    context.getString(R.string.str_world_map),
                    context.getString(R.string.agmap_USA_Topo_Maps_url),
                    true);

            mapLayers.put(layer.getId(), layer);

            layer = new ArcGisMapLayer(
                    getId(),
                    context.getString(R.string.agmap_World_Physical_Map_name),
                    context.getString(R.string.agmap_World_Physical_Map_desc),
                    context.getString(R.string.str_world_map),
                    context.getString(R.string.agmap_World_Physical_Map_url),
                    true);

            mapLayers.put(layer.getId(), layer);

            layer = new ArcGisMapLayer(
                    getId(),
                    context.getString(R.string.agmap_World_Shaded_Relief_name),
                    context.getString(R.string.agmap_World_Shaded_Relief_desc),
                    context.getString(R.string.str_world_map),
                    context.getString(R.string.agmap_World_Shaded_Relief_url),
                    true);

            mapLayers.put(layer.getId(), layer);

            layer = new ArcGisMapLayer(
                    getId(),
                    context.getString(R.string.agmap_World_Street_Map_name),
                    context.getString(R.string.agmap_World_Street_Map_desc),
                    context.getString(R.string.str_world_map),
                    context.getString(R.string.agmap_World_Street_Map_url),
                    true);

            mapLayers.put(layer.getId(), layer);

            layer = new ArcGisMapLayer(
                    getId(),
                    context.getString(R.string.agmap_World_Terrain_Base_name),
                    context.getString(R.string.agmap_World_Terrain_Base_desc),
                    context.getString(R.string.str_world_map),
                    context.getString(R.string.agmap_World_Terrain_Base_url),
                    true);

            mapLayers.put(layer.getId(), layer);

            layer = new ArcGisMapLayer(
                    getId(),
                    context.getString(R.string.agmap_World_Topo_Map_name),
                    context.getString(R.string.agmap_World_Topo_Map_desc),
                    context.getString(R.string.str_world_map),
                    context.getString(R.string.agmap_World_Topo_Map_url),
                    true);

            mapLayers.put(layer.getId(), layer);

            Global.Settings.DeviceSettings.setArcGisMayLayers(mapLayers.values());
        }
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
        if (idCounter == 0) {
            idCounter = Global.Settings.DeviceSettings.getArcGisIdCounter();
        }

        Global.Settings.DeviceSettings.setArcGisMapIdCounter(++idCounter);
        return idCounter;
    }


    public static ArcGisMapLayer createMapLayer(String name, String description, String location, String uri, boolean online) {
        return new ArcGisMapLayer(getId(), name, description, location, uri, online);
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

        return getBaseLayer(mapLayers.get(id));
    }

    public static Layer getBaseLayer(ArcGisMapLayer agml) {
        return getBaseLayer(agml, agml.isOnline());
    }

    public static Layer getBaseLayer(ArcGisMapLayer agml, boolean isOnline) {
        UserCredentials credentials = null; //Global.Settings.DeviceSettings.getArcGisUserCredentials();

        Layer layer = null;

        if (isOnline) {
            layer = new ArcGISTiledMapServiceLayer(agml.getUri(), credentials);
        } else {
            layer = new ArcGISLocalTiledLayer(agml.getUri());
        }

        return layer;
    }


    public static void addLayer(ArcGisMapLayer layer) {
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


    public static void getLayerFromUrl(final String url, Context context, final IGetArcMapLayerListener listener) {
        if (listener == null)
            throw new RuntimeException("Listener is null");

        String jUrl = url;
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
                                    response.optDouble("minScale", 0),
                                    response.optDouble("maxScale", 0),
                                    getDetailLevelsFromJson(response),
                                    false)
                    );
                } else {
                    listener.onBadUrl();
                }
            }
        },
        new Response.ErrorListener(){
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.onBadUrl();
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

                    }
                }
            }
        } catch (JSONException e) {

        }

        return detailLevels.toArray(new ArcGisMapLayer.DetailLevel[detailLevels.size()]);
    }



    private static HashMap<Integer, DownloadOfflineArcGISMapTask> tasks = new HashMap<>();

    public static void startOfflineMapDownload(DownloadOfflineArcGISMapTask task) {
        final ArcGisMapLayer layer = task.getLayer();

        tasks.put(task.getLayer().getId(), task);

        Global.TtNotifyManager.startMapDownload(layer.getId(), layer.getName());

        task.startDownload(new DownloadOfflineArcGISMapTask.DownloadListener() {
            @Override
            public void onMapDownloaded(ArcGisMapLayer layer) {
                addLayer(layer);

                Global.TtNotifyManager.endMapDownload(layer.getId());
                //Toast.makeText(Global.getApplicationContext(), String.format("%s Downloaded", layer.getName()), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onTaskUpdate(ExportTileCacheStatus status) {
                if (status.getStatus() == GPJobResource.JobStatus.EXECUTING) {
                    int progress = (int) (100 * status.getTotalBytesDownloaded() / status.getDownloadSize());

                    Global.TtNotifyManager.updateMapDownload(layer.getId(), progress);
                } else {
                    //Toast.makeText(Global.getApplicationContext(), status.getStatus().toString(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onStatusError(String message) {
                tasks.remove(layer.getId());
                Global.TtNotifyManager.endMapDownload(layer.getId());
                //Toast.makeText(Global.getApplicationContext(), "Error creating offline map", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onDownloadError(String message) {
                tasks.remove(layer.getId());
                Global.TtNotifyManager.endMapDownload(layer.getId());
                //Toast.makeText(Global.getApplicationContext(), "Error downloading offline map", Toast.LENGTH_LONG).show();
            }
        });
    }

    public interface IGetArcMapLayerListener {
        void onComplete(ArcGisMapLayer layer);
        void onBadUrl();
    }

    public interface IArcToolsListener {
        void arcLayerAdded(ArcGisMapLayer layer);
    }
}
