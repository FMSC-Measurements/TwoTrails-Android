package com.usda.fmsc.twotrails.objects.points;


import android.os.Parcel;
import android.os.Parcelable;

import com.usda.fmsc.twotrails.objects.TtPolygon;
import com.usda.fmsc.twotrails.units.OpType;
import com.usda.fmsc.twotrails.utilities.TtUtils;

public class InertialPoint extends GpsPoint implements TtPoint.IAzimuth {
   public static final Parcelable.Creator<InertialPoint> CREATOR = new Parcelable.Creator<InertialPoint>() {
      @Override
      public InertialPoint createFromParcel(Parcel source) {
         return new InertialPoint(source);
      }

      @Override
      public InertialPoint[] newArray(int size) {
         return new InertialPoint[size];
      }
   };


   private boolean _AllSegmentsValid;
   private double _TimeSpan;
   private double _DistX, _DistY, _DistZ;
   private double _Azimuth;



   public InertialPoint() {
      super();
   }

   public InertialPoint(Parcel source) {
      super(source);

      _AllSegmentsValid = source.readBoolean();
      _TimeSpan = source.readDouble();
      _Azimuth = source.readDouble();
      _DistX = source.readDouble();
      _DistY = source.readDouble();
      _DistZ = source.readDouble();
   }


   public InertialPoint(TtPoint p) {
      super(p);

      if (p.getOp() == OpType.Inertial) {
         copy((InertialPoint)p);
      }
   }


   public InertialPoint(InertialPoint p) {
      super(p);
      copy(p);
   }

   private void copy(InertialPoint p) {

      _AllSegmentsValid = p._AllSegmentsValid;
      _TimeSpan = p._TimeSpan;
      _Azimuth = p._Azimuth;
      _DistX = p._DistX;
      _DistY = p._DistY;
      _DistZ = p._DistZ;
   }

   @Override
   public void writeToParcel(Parcel dest, int flags) {
      super.writeToParcel(dest, flags);

      dest.writeBoolean(_AllSegmentsValid);
      dest.writeDouble(_TimeSpan);
      dest.writeDouble(_Azimuth);
      dest.writeDouble(_DistX);
      dest.writeDouble(_DistY);
      dest.writeDouble(_DistZ);
   }

   @Override
   public OpType getOp() {
      return OpType.Inertial;
   }

   @Override
   public boolean adjustPoint() {
      if (!_calculated)
         return false;
      _adjusted = true;
      return true;
   }

   public boolean adjustPoint(TtPoint source) {
      return calculatePoint(source, true);
   }

   @Override
   public boolean calculatePoint(TtPolygon polygon, TtPoint previousPoint) {
      return calculatePoint(previousPoint, false);
   }

   public boolean calculatePoint(TtPoint previousPoint, boolean isAdjusted) {
      Double az = null;

      if (previousPoint instanceof InertialStartPoint) {
         az = ((InertialStartPoint)previousPoint).getTotalAzimuth();
      } else if (previousPoint instanceof IAzimuth) {
         az = ((IAzimuth)previousPoint).getAzimuth();
      }

      if (az != null) {
         double azAsRad = TtUtils.Convert.degreesToRadians(az);
         double dist = TtUtils.Math.distance(0, 0, Math.abs(_DistX), Math.abs(_DistY));

         if (isAdjusted) {
            _AdjX = previousPoint.getAdjX() + (dist * Math.cos(azAsRad));
            _AdjY = previousPoint.getAdjY() + (dist * Math.sin(azAsRad));
            _AdjZ = previousPoint.getAdjZ() + _DistZ;
            _adjusted = true;
         } else {
            _AdjX = previousPoint.getUnAdjX() + (dist * Math.cos(azAsRad));
            _AdjY = previousPoint.getUnAdjY() + (dist * Math.sin(azAsRad));
            _AdjZ = previousPoint.getAdjZ() + _DistZ;
            _calculated = true;
            _adjusted = false;
         }
      } else {
         _calculated = false;
         throw new RuntimeException("Null Azimuth");
      }

      return true;
   }

   @Override
   public double getAzimuth() {
      return _Azimuth;
   }

   public boolean areAllSegmentsValid() {
      return _AllSegmentsValid;
   }

   public double getTimeSpan() {
      return _TimeSpan;
   }

   public double getDistX() {
      return _DistX;
   }

   public double getDistY() {
      return _DistY;
   }

   public double getDistZ() {
      return _DistZ;
   }

   public void setInertialValues(double azimuth, boolean allSegmentsValid, double timespan, double distX, double distY, double distZ) {
      this._Azimuth = azimuth;
      this._AllSegmentsValid = allSegmentsValid;
      this._TimeSpan = timespan;
      this._DistX = distX;
      this._DistY = distY;
      this._DistZ = distZ;
   }
}
