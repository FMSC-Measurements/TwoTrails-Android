package com.usda.fmsc.twotrails.fragments.map;

import android.os.Parcel;
import android.os.Parcelable;

import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.twotrails.objects.map.LineGraphicManager;
import com.usda.fmsc.twotrails.objects.map.PolygonDrawOptions;
import com.usda.fmsc.twotrails.objects.map.PolygonGraphicManager;
import com.usda.fmsc.twotrails.objects.map.TrailGraphicManager;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.units.MapType;

public interface IMultiMapFragment {
    String MAP_OPTIONS_EXTRA = "MapOptionsEx";

    void setMap(int mapType);

    void setLocationEnabled(boolean enabled);

    void setCompassEnabled(boolean enabled);

    void setMapPadding(int left, int top, int right, int bottom);

    void setGesturesEnabled(boolean enabled);


    void moveToMapMaxExtents(boolean animate);

    void moveToLocation(double lat, double lon, boolean animate);

    void moveToLocation(double lat, double lon, float zoomLevel, boolean animate);

    void moveToLocation(Extent extents, int padding, boolean animate);

    void updateLocation(Position position);

    void addPolygon(PolygonGraphicManager graphicManager, PolygonDrawOptions drawOptions);

    void removePolygon(PolygonGraphicManager graphicManager);

    void addTrail(TrailGraphicManager graphicManager);

    void removeTrail(TrailGraphicManager graphicManager);

    void addLine(LineGraphicManager graphicManager);

    void removeLine(LineGraphicManager graphicManager);

    void hideSelectedMarkerInfo();

    //Position getMapLatLonCenter();

    Extent getExtents();

    MarkerData getMarkerData(String id);

    boolean mapHasMaxExtents();


    interface MultiMapListener {
        void onMapReady();
        void onMapLoaded();
        void onMapTypeChanged(MapType mapType, int mapId, boolean isOnline);
        void onMapClick(Position position);
        void onMarkerClick(MarkerData markerData);
    }


    class MapOptions implements Parcelable {
        private int MapId;
        private double North = 0, East = 0, South = 0, West = 0;
        private double Latitude = 0, Longitude = 0;
        private float ZoomLevel = 0f;
        private int Padding = 0;

        public static final Parcelable.Creator<MapOptions> CREATOR = new Parcelable.Creator<MapOptions>() {
            @Override
            public MapOptions createFromParcel(Parcel source) {
                return new MapOptions(source);
            }

            @Override
            public MapOptions[] newArray(int size) {
                return new MapOptions[size];
            }
        };



        public MapOptions(Parcel in) {
            MapId = in.readInt();
            North = in.readDouble();
            East = in.readDouble();
            South = in.readDouble();
            West = in.readDouble();
            Padding = in.readInt();
            Latitude = in.readDouble();
            Longitude = in.readDouble();
            ZoomLevel = in.readFloat();
        }

        public MapOptions(int mapId, Extent extent) {
            this(mapId, extent, 0);
        }

        public MapOptions(int mapId, Extent extent, int padding) {
            this.MapId = mapId;

            this.North = extent.getNorth();
            this.East = extent.getEast();
            this.South = extent.getSouth();
            this.West = extent.getWest();
            this.Padding = padding;
        }

        public MapOptions(int mapId, Position northEast, Position southWest) {
            this(mapId, northEast, southWest, 0);
        }

        public MapOptions(int mapId, Position northEast, Position southWest, int padding) {
            this.MapId = mapId;

            this.North = northEast.getLatitudeSignedDecimal();
            this.East = northEast.getLongitudeSignedDecimal();
            this.South = southWest.getLatitudeSignedDecimal();
            this.West = southWest.getLongitudeSignedDecimal();
            this.Padding = padding;
        }

        public MapOptions(int mapId, Position position) {
            this(mapId, position.getLatitudeSignedDecimal(), position.getLongitudeSignedDecimal(), null);
        }

        public MapOptions(int mapId, Position position, Float zoomLevel) {
            this(mapId, position.getLatitudeSignedDecimal(), position.getLongitudeSignedDecimal(), zoomLevel);
        }

        public MapOptions(int mapId, Double latitude, Double longitude) {
            this(mapId, latitude, longitude, null);
        }

        public MapOptions(int mapId, Double latitude, Double longitude, Float zoomLevel) {
            this.MapId = mapId;

            this.Latitude = latitude;
            this.Longitude = longitude;
            this.ZoomLevel = zoomLevel;
        }


        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(MapId);
            dest.writeDouble(North);
            dest.writeDouble(East);
            dest.writeDouble(South);
            dest.writeDouble(West);
            dest.writeInt(Padding);
            dest.writeDouble(Latitude);
            dest.writeDouble(Longitude);
            dest.writeFloat(ZoomLevel);
        }


        public boolean hasExtents() {
            return North != 0 || South != 0 || West != 0 || East != 0;
        }

        public boolean hasLocation() {
            return Latitude != 0 || Longitude != 0;
        }


        public int getMapId() {
            return MapId;
        }

        public void setMapId(int mapId) {
            MapId = mapId;
        }

        public Double getNorth() {
            return North;
        }

        public void setNorth(Double north) {
            North = north;
        }

        public Double getEast() {
            return East;
        }

        public void setEast(Double east) {
            East = east;
        }

        public Double getSouth() {
            return South;
        }

        public void setSouth(Double south) {
            South = south;
        }

        public Double getWest() {
            return West;
        }

        public void setWest(Double west) {
            West = west;
        }

        public Integer getPadding() {
            return Padding;
        }

        public void setPadding(Integer padding) {
            Padding = padding;
        }

        public Double getLatitude() {
            return Latitude;
        }

        public void setLatitude(Double latitude) {
            Latitude = latitude;
        }

        public Double getLongitude() {
            return Longitude;
        }

        public void setLongitude(Double longitude) {
            Longitude = longitude;
        }

        public Float getZoomLevel() {
            return ZoomLevel;
        }

        public void setZoomLevel(Float zoomLevel) {
            ZoomLevel = zoomLevel;
        }

        public Extent getExtents() {
            return new Extent(North, East, South, West);
        }
    }

    class MarkerData {
        public static final String ATTR_KEY = "MDK";

        private final TtPoint _Point;
        private final TtMetadata _Metadata;
        private final boolean _Adjusted;
        private final String _Key;

        public MarkerData(TtPoint point, TtMetadata metadata, boolean adjusted) {
            _Point = point;
            _Metadata = metadata;
            _Adjusted = adjusted;
            _Key = _Point.getCN() + (_Adjusted ? "Adj" : "UnAdj");
        }

        public String getKey() {
            return _Key;
        }

        public TtPoint getPoint() {
            return _Point;
        }

        public TtMetadata getMetadata() {
            return _Metadata;
        }

        public boolean isAdjusted() {
            return _Adjusted;
        }
    }
}
