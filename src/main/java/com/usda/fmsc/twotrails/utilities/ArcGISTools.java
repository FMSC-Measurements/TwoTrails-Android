package com.usda.fmsc.twotrails.utilities;

import android.content.Context;

import com.esri.android.map.Layer;
import com.esri.android.map.ags.ArcGISLocalTiledLayer;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.core.io.UserCredentials;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.R;
import com.usda.fmsc.twotrails.objects.ArcGisMapLayer;

import java.util.Collection;
import java.util.HashMap;

public class ArcGISTools {
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

    public static Layer getMapLayer(int id) {
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

    public static void addOnlineLayer(String url) {

        //get info from url
        //ArcGisMapLayer layer = new ArcGisMapLayer(name, description, url, true);
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
}
