package com.usda.fmsc.twotrails.utilities;

import android.util.Log;

import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.io.UserCredentials;
import com.esri.core.map.CallbackListener;
import com.esri.core.tasks.tilecache.ExportTileCacheParameters;
import com.esri.core.tasks.tilecache.ExportTileCacheStatus;
import com.esri.core.tasks.tilecache.ExportTileCacheTask;
import com.usda.fmsc.twotrails.Global;
import com.usda.fmsc.twotrails.objects.map.ArcGisMapLayer;
import com.usda.fmsc.utilities.StringEx;

public class DownloadOfflineArcGISMapTask {
    //true create as tile package, false to create as compact cache
    private static final boolean CREATE_AS_TILE_PACKAGE = false;
    private static final int MAX_DETAIL_LEVELS = 8;


    private ExportTileCacheTask exportTileCacheTask;
    private ExportTileCacheParameters params;

    private ArcGisMapLayer layer;
    private String downloadLocation;

    public DownloadOfflineArcGISMapTask(ArcGisMapLayer layer, Envelope extents, SpatialReference spatialReference, String downloadLocation, UserCredentials credentials) {
        this.layer = layer;
        this.downloadLocation = downloadLocation;

        double[] detailLevels = new double[layer.getLevelsOfDetail().length];

        for (int i = 0; i < layer.getLevelsOfDetail().length && i < MAX_DETAIL_LEVELS; i++) {
            detailLevels[i] = layer.getLevelsOfDetail()[i].getLevel();
        }

        exportTileCacheTask = new ExportTileCacheTask(layer.getUrl(), credentials);

        exportTileCacheTask.setRecoveryDir(Global.getOfflineMapsRecoveryDir());

        params = new ExportTileCacheParameters(CREATE_AS_TILE_PACKAGE, detailLevels, ExportTileCacheParameters.ExportBy.ID, extents, spatialReference);
    }


    public void estimateMapSize(final EstimateListener listener) {
        exportTileCacheTask.estimateTileCacheSize(params, new CallbackListener<Long>() {
            @Override
            public void onCallback(Long aLong) {
                if (listener != null) {
                    listener.onSizeEstimated(aLong);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                if (listener != null) {
                    listener.onEstimateError(throwable.getMessage());
                }
            }
        });
    }

    public void startDownload(final DownloadListener listener) {
        exportTileCacheTask.generateTileCache(
                params,
                //status callback
                new CallbackListener<ExportTileCacheStatus>() {
                    @Override
                    public void onCallback(ExportTileCacheStatus exportTileCacheStatus) {
                        Log.d("*** tileCacheStatus : ", exportTileCacheStatus.getStatus().toString());
                        if (listener != null) {
                            listener.onTaskUpdate(exportTileCacheStatus);
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.d("*** tileCacheStatus error: ", "" + throwable);
                        if (listener != null) {
                            listener.onStatusError(throwable.getMessage());
                        }
                    }
                },
                //download callback
                new CallbackListener<String>() {
                    @Override
                    public void onCallback(String s) {
                        if (!StringEx.isEmpty(s)) {
                            layer.setFilePath(s);

                            if (listener != null) {
                                listener.onMapDownloaded(layer);
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.d("*** tileCacheStatus error: ", "" + throwable);
                        if (listener != null) {
                            listener.onDownloadError(throwable.getMessage());
                        }
                    }
                },
                downloadLocation
        );
    }


    public ArcGisMapLayer getLayer() {
        return layer;
    }



    public interface EstimateListener {
        void onSizeEstimated(Long bytes);
        void onEstimateError(String message);
    }

    public interface DownloadListener {
        void onMapDownloaded(ArcGisMapLayer layer);
        void onTaskUpdate(ExportTileCacheStatus status);
        void onStatusError(String message);
        void onDownloadError(String message);
    }
}
