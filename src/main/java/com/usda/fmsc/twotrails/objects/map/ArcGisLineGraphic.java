package com.usda.fmsc.twotrails.objects.map;


import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.PointCollection;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.usda.fmsc.geospatial.gnss.Extent;
import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.geospatial.gnss.GeoTools;

public class ArcGisLineGraphic implements ILineGraphic, LineGraphicOptions.Listener {
   private final MapView map;
   private Extent polyBounds;

   private GraphicsOverlay _LineLayer;
   private Graphic _LineGraphic;
   private SimpleLineSymbol _LineOutline;
   private Polyline _Polyline;

   LineGraphicOptions _GraphicOptions;

   private boolean visible = true;


   public ArcGisLineGraphic(MapView mapView) {
      this.map = mapView;
   }

   @Override
   public void build(Position point1, Position point2, LineGraphicOptions graphicOptions) {
      _GraphicOptions = graphicOptions;
      _LineLayer = new GraphicsOverlay();

      int drawSize = _GraphicOptions.getLineWidth() / 2;

      _LineOutline = new SimpleLineSymbol(getLineSymbolStyle(_GraphicOptions.getLineStyle()), _GraphicOptions.getLineColor(), drawSize);

      updateGeometry(point1, point2);

      map.getGraphicsOverlays().add(_LineLayer);

      graphicOptions.addListener(this);
   }

   @Override
   public void updateGeometry(Position point1, Position point2) {
      Extent.Builder eBuilder = new Extent.Builder();
      PointCollection linePoints = new PointCollection(SpatialReferences.getWgs84());

      Point posLL = new Point(point1.getLongitude(), point1.getLatitude(), SpatialReferences.getWgs84());
      linePoints.add(posLL);

      posLL = new Point(point2.getLongitude(), point2.getLatitude(), SpatialReferences.getWgs84());
      linePoints.add(posLL);

      _Polyline = new Polyline(linePoints);

      if (_LineGraphic != null) {
         _LineGraphic.setGeometry(_Polyline);
      } else {
         _LineGraphic = new Graphic(_Polyline, _LineOutline);
         _LineLayer.getGraphics().add(_LineGraphic);
      }

      eBuilder.include(point1);
      eBuilder.include(point2);
      polyBounds = eBuilder.build();
   }

   @Override
   public Extent getExtents() {
      return polyBounds;
   }

   @Override
   public Position getPosition() {
      return GeoTools.getMidPoint(polyBounds);
   }

   @Override
   public void setVisible(boolean visible) {
      this.visible = visible;
      _LineLayer.setVisible(visible);
   }

   @Override
   public boolean isVisible() {
      return this.visible;
   }


   private SimpleLineSymbol.Style getLineSymbolStyle(LineGraphicOptions.LineStyle style) {
      switch (style) {
         case Dashed: return SimpleLineSymbol.Style.DASH;
         case Dotted: return SimpleLineSymbol.Style.DOT;
         case Solid:
         default: return SimpleLineSymbol.Style.SOLID;
      }
   }

   @Override
   public void onOptionChanged(LineGraphicOptions lgo, LineGraphicOptions.LineGraphicCode code, int value) {
      if (_LineGraphic != null) {
         _LineLayer.getGraphics().remove(_LineGraphic);
      }

      _LineOutline = new SimpleLineSymbol(getLineSymbolStyle(lgo.getLineStyle()), lgo.getLineColor(), (int)(lgo.getLineWidth() / 2));

      _LineGraphic = new Graphic(_Polyline, _LineOutline);
      _LineLayer.getGraphics().add(_LineGraphic);
   }
}
