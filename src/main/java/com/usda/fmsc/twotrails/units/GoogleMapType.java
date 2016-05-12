package com.usda.fmsc.twotrails.units;

public enum GoogleMapType {
    MAP_TYPE_NONE(0),
    MAP_TYPE_NORMAL(1),
    MAP_TYPE_SATELLITE(2),
    MAP_TYPE_TERRAIN(3),
    MAP_TYPE_HYBRID(4);

    private final int value;

    private GoogleMapType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static GoogleMapType parse(int id) {
        GoogleMapType[] types = values();
        if(types.length > id && id > -1)
            return types[id];
        throw new IllegalArgumentException("Invalid GoogleMapType id: " + id);
    }

    @Override
    public String toString() {
        switch (this) {
            case MAP_TYPE_NORMAL: return "Normal";
            case MAP_TYPE_SATELLITE: return "Satellite";
            case MAP_TYPE_TERRAIN: return "Terrain";
            case MAP_TYPE_HYBRID: return "Hybrid";
            case MAP_TYPE_NONE:
            default: return "None";
        }
    }

    public String getName() {
        switch (this) {
            case MAP_TYPE_NORMAL: return "Street";
            case MAP_TYPE_SATELLITE: return "Satellite";
            case MAP_TYPE_TERRAIN: return "Terrain";
            case MAP_TYPE_HYBRID: return "Hybrid";
            case MAP_TYPE_NONE:
            default: return "None";
        }
    }

    public String getDescription() {
        switch (this) {
            case MAP_TYPE_NORMAL: return "Typical road map. Roads, some man-made features, and important natural features such as rivers are shown. Road and feature labels are also visible.";
            case MAP_TYPE_SATELLITE: return "Satellite photograph data. Road and feature labels are not visible.";
            case MAP_TYPE_TERRAIN: return "Topographic data. The map includes colors, contour lines and labels, and perspective shading. Some roads and labels are also visible.";
            case MAP_TYPE_HYBRID: return "Satellite photograph data with road maps added. Road and feature labels are also visible.";
            case MAP_TYPE_NONE:
            default: return "No tiles. The map will be rendered as an empty grid with no tiles loaded.";
        }
    }
}
