package com.usda.fmsc.twotrails.ins;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.usda.fmsc.geospatial.ins.IINSData;
import com.usda.fmsc.twotrails.objects.TtObject;

import org.joda.time.DateTime;

public class TtInsData extends TtObject implements IINSData, Parcelable {
   public static final Parcelable.Creator<TtInsData> CREATOR = new Parcelable.Creator<TtInsData>() {
      @Override
      public TtInsData createFromParcel(Parcel source) {
         return new TtInsData(source);
      }

      @Override
      public TtInsData[] newArray(int size) {
         return new TtInsData[size];
      }
   };

   private final String pointCN;
   private final DateTime timeCreated;

   private final boolean isConsecutive;
   private final long timeSinceStart;
   private final double timeSpan;
   private final double distX, distY, distZ;
   private final double linAccelX, linAccelY, linAccelZ;
   private final double velX, velY, velZ;
   private final double rotX, rotY, rotZ;
   private final double yaw, pitch, roll;


   private TtInsData(Parcel source) {
      this(source.readString(), source.readString(), (DateTime) source.readSerializable(),
           source.readBoolean(), source.readLong(), source.readDouble(),
           source.readDouble(), source.readDouble(), source.readDouble(),
           source.readDouble(), source.readDouble(), source.readDouble(),
           source.readDouble(), source.readDouble(), source.readDouble(),
           source.readDouble(), source.readDouble(), source.readDouble(),
           source.readDouble(), source.readDouble(), source.readDouble());
   }

   public TtInsData(String cn, String pointCN, DateTime timeCreated,
                    boolean isConsecutive, long timeSinceStart, double timeSpan,
                    double distX, double distY, double distZ,
                    double linAccelX, double linAccelY, double linAccelZ,
                    double velX, double velY, double velZ,
                    double rotX, double rotY, double rotZ,
                    double yaw, double pitch, double roll) {
      setCN(cn);
      this.pointCN = pointCN;
      this.timeCreated = timeCreated;

      this.timeSinceStart = timeSinceStart;
      this.isConsecutive = isConsecutive;
      this.timeSpan = timeSpan;

      this.distX = distX;
      this.distY = distY;
      this.distZ = distZ;

      this.linAccelX = linAccelX;
      this.linAccelY = linAccelY;
      this.linAccelZ = linAccelZ;

      this.velX = velX;
      this.velY = velY;
      this.velZ = velZ;

      this.rotX = rotX;
      this.rotY = rotY;
      this.rotZ = rotZ;

      this.yaw = yaw;
      this.pitch = pitch;
      this.roll = roll;
   }

   public static TtInsData create(String pointCN, IINSData data) {
      return new TtInsData(
              java.util.UUID.randomUUID().toString(), pointCN, DateTime.now(),
              data.isConsecutive(), data.getTimeSinceStart(), data.getTimeSpan(),
              data.getDistanceX(), data.getDistanceY(), data.getDistanceZ(),
              data.getLinearAccelX(), data.getLinearAccelY(), data.getLinearAccelZ(),
              data.getVelocityX(), data.getVelocityY(), data.getVelocityZ(),
              data.getRotationX(), data.getRotationY(), data.getRotationZ(),
              data.getYaw(), data.getPitch(), data.getRoll());
   }


   @Override
   public int describeContents() {
      return 0;
   }

   @Override
   public void writeToParcel(@NonNull Parcel dest, int flags) {
      dest.writeString(getCN());
      dest.writeString(pointCN);
      dest.writeSerializable(timeCreated);

      dest.writeBoolean(isConsecutive);
      dest.writeLong(timeSinceStart);
      dest.writeDouble(timeSpan);

      dest.writeDouble(distX);
      dest.writeDouble(distY);
      dest.writeDouble(distZ);

      dest.writeDouble(linAccelX);
      dest.writeDouble(linAccelY);
      dest.writeDouble(linAccelZ);

      dest.writeDouble(velX);
      dest.writeDouble(velY);
      dest.writeDouble(velZ);

      dest.writeDouble(rotX);
      dest.writeDouble(rotY);
      dest.writeDouble(rotZ);

      dest.writeDouble(yaw);
      dest.writeDouble(pitch);
      dest.writeDouble(roll);
   }


   public String getPointCN() {
      return pointCN;
   }

   public DateTime getTimeCreated() {
      return timeCreated;
   }

   @Override
   public long getTimeSinceStart() {
      return timeSinceStart;
   }

   @Override
   public boolean isConsecutive() {
      return isConsecutive;
   }

   @Override
   public double getTimeSpan() {
      return timeSpan;
   }


   @Override
   public double getDistanceX() {
      return distX;
   }

   @Override
   public double getDistanceY() {
      return distY;
   }

   @Override
   public double getDistanceZ() {
      return distZ;
   }


   @Override
   public double getLinearAccelX() {
      return linAccelX;
   }

   @Override
   public double getLinearAccelY() {
      return linAccelY;
   }

   @Override
   public double getLinearAccelZ() {
      return linAccelZ;
   }


   @Override
   public double getVelocityX() {
      return velX;
   }

   @Override
   public double getVelocityY() {
      return velY;
   }

   @Override
   public double getVelocityZ() {
      return velZ;
   }


   @Override
   public double getRotationX() {
      return rotX;
   }

   @Override
   public double getRotationY() {
      return rotY;
   }

   @Override
   public double getRotationZ() {
      return rotZ;
   }


   @Override
   public double getYaw() {
      return yaw;
   }

   @Override
   public double getPitch() {
      return pitch;
   }

   @Override
   public double getRoll() {
      return roll;
   }
}
