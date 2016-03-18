package com.usda.fmsc.twotrails.objects;

import java.io.Serializable;

public class ArcGisMapLayer implements Serializable {
    private int id;
    private String name;
    private String description;
    private String location;
    boolean online;

    public ArcGisMapLayer(int id, String name, String description, String location, boolean online) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.location = location;
        this.online = online;
    }

    public int getId() {
        return id;
    }

    public boolean isOnline() {
        return online;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }


//    public enum LayerType {
//        Standard(0),
//        Online(1),
//        Offline(2),;
//
//        private final int value;
//
//        private LayerType(int value) {
//            this.value = value;
//        }
//
//        public int getValue() {
//            return value;
//        }
//
//        public static LayerType parse(int id) {
//            LayerType[] types = values();
//            if(types.length > id && id > -1)
//                return types[id];
//            throw new IllegalArgumentException("Invalid LayerType id: " + id);
//        }
//
//        @Override
//        public String toString() {
//            switch (this) {
//                case Standard: return "Standard";
//                case Online: return "Online";
//                case Offline: return "Offline";
//                default: return "None";
//            }
//        }
//    }
}
