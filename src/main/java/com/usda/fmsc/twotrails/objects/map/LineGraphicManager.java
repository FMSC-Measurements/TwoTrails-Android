package com.usda.fmsc.twotrails.objects.map;


import android.location.Location;

import com.usda.fmsc.geospatial.Extent;
import com.usda.fmsc.geospatial.Position;
import com.usda.fmsc.twotrails.objects.TtMetadata;
import com.usda.fmsc.twotrails.objects.points.TtPoint;
import com.usda.fmsc.twotrails.utilities.TtUtils;

import java.util.HashMap;
import java.util.UUID;

public class LineGraphicManager implements IGraphicManager {

   private ILineGraphic lineGraphic;
   private LineGraphicOptions graphicOptions;
   private Position point1, point2;
   private boolean visible = true;
   private final String _CN;


   public LineGraphicManager(Position point1, Position point2, LineGraphicOptions graphicOptions) {
      this(point1, point2, null, graphicOptions);
   }

   public LineGraphicManager(Position point1, Position point2, ILineGraphic lineGraphic, LineGraphicOptions graphicOptions) {
      this.graphicOptions = graphicOptions;
      this.point1 = point1;
      this.point2 = point2;

      if (lineGraphic != null) {
         setGraphic(lineGraphic);
      }

      _CN = UUID.randomUUID().toString();
   }

   public void setGraphic(ILineGraphic lineGraphic) {
      setGraphic(lineGraphic, graphicOptions);
   }

   public void setGraphic(ILineGraphic lineGraphic, LineGraphicOptions graphicOptions) {
      this.lineGraphic = lineGraphic;
      this.graphicOptions = graphicOptions;

      if (this.point1 == null) {
         this.point1 = new Position(0,0);
      }

      if (this.point2 == null) {
         this.point2 = new Position(0,0);
      }

      this.lineGraphic.build(point1, point2, this.graphicOptions);
      this.lineGraphic.setVisible(visible);
   }

   public void updateGraphic(Position point1, Position point2) {
      if (point1 == null) {
         this.point1 = new Position(0,0);
      } else {
         this.point1 = point1;
      }

      if (point2 == null) {
         this.point2 = new Position(0,0);
      } else {
         this.point2 = point2;
      }

      if (this.lineGraphic != null) {
         this.lineGraphic.update(this.point1, this.point2);
      }
   }

   public void updateGraphic(TtPoint point1, TtPoint point2, HashMap<String, TtMetadata> metadata) {
      Location loc = TtUtils.Points.getPointLocation(point1, true, metadata);
      this.point1 = new Position(loc.getLatitude(), loc.getLongitude());
      loc = TtUtils.Points.getPointLocation(point2, true, metadata);
      this.point2 = new Position(loc.getLatitude(), loc.getLongitude());

      if (this.lineGraphic != null) {
         this.lineGraphic.update(this.point1, this.point2);
      }
   }


   @Override
   public String getCN() {
      return _CN;
   }

   @Override
   public Position getPosition() {
      return this.lineGraphic != null ? this.lineGraphic.getPosition() : null;
   }

   @Override
   public Extent getExtents() {
      return this.lineGraphic != null ? this.lineGraphic.getExtents() : null;
   }


   public void setVisible(boolean visible) {
      this.visible = visible;

      if (this.lineGraphic != null) {
         this.lineGraphic.setVisible(visible);
      }
   }

   public boolean isVisible() {
      return this.visible;
   }
}
