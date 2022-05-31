package com.usda.fmsc.twotrails.objects.map;


import androidx.annotation.ColorInt;
import java.util.ArrayList;

public class LineGraphicOptions {
   private @ColorInt int _LineColor;
   private int _LineWidth;
   private LineStyle _LineStyle;
   private final ArrayList<LineGraphicOptions.Listener> listeners = new ArrayList<>();


   public LineGraphicOptions(@ColorInt int lineColor, int lineWidth, LineStyle lineStyle) {
      _LineColor = lineColor;
      _LineWidth = lineWidth;
      _LineStyle = lineStyle;
   }


   public @ColorInt int getLineColor() {
      return _LineColor;
   }

   public void setLineColor(@ColorInt int color) {
      _LineColor = color;
      onOptionChanged(LineGraphicCode.COLOR, _LineColor);
   }


   public int getLineWidth() {
      return _LineWidth;
   }

   public void setLineWidth(int width) {
      _LineWidth = width;
      onOptionChanged(LineGraphicCode.WIDTH, _LineWidth);
   }


   public LineStyle getLineStyle() {
      return _LineStyle;
   }

   public void setLineStyle(LineStyle style) {
      _LineStyle = style;
      onOptionChanged(LineGraphicCode.LINE_STYLE, style.getValue());
   }


   private void onOptionChanged(LineGraphicCode code, int value) {
      for (LineGraphicOptions.Listener listener : listeners) {
         listener.onOptionChanged(this, code, value);
      }
   }


   public void addListener(LineGraphicOptions.Listener listener) {
      if (!listeners.contains(listener)){
         listeners.add(listener);
      }
   }

   public void removeListener(PolygonGraphicOptions.Listener listener) {
      listeners.remove(listener);
   }


   public enum LineStyle {
      Solid(0),
      Dashed(1),
      Dotted(2);

      private final int value;

      LineStyle(int value) {
         this.value = value;
      }

      public int getValue() {
         return value;
      }

      public static LineStyle parse(int id) {
         LineStyle[] types = values();
         if(types.length > id && id > -1)
            return types[id];
         throw new IllegalArgumentException("Invalid id: " + id);
      }
   }

   public enum LineGraphicCode {
      COLOR,
      WIDTH,
      LINE_STYLE
   }


   public interface Listener {
      void onOptionChanged(LineGraphicOptions lgo, LineGraphicCode code, int value);
   }
}
