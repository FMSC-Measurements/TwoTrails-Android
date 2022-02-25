package com.usda.fmsc.twotrails.objects.map;


public class LineGraphicOptions {
   private final int LineColor;
   private final float LineWidth;
   private final LineStyle LineStyle;

   public LineGraphicOptions(int lineColor, float lineWidth, LineStyle lineStyle) {
      LineColor = lineColor;
      LineWidth = lineWidth;
      LineStyle = lineStyle;
   }

   public int getLineColor() {
      return LineColor;
   }

   public float getLineWidth() {
      return LineWidth;
   }

   public LineStyle getLineStyle() {
      return LineStyle;
   }

   public enum LineStyle {
      Solid,
      Dashed,
      Dotted
   }
}
