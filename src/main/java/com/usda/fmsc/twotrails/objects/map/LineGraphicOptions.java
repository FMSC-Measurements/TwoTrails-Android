package com.usda.fmsc.twotrails.objects.map;


public class LineGraphicOptions {
   private final int LineColor;
   private final float LineWidth;

   public LineGraphicOptions(int lineColor, float lineWidth) {
      LineColor = lineColor;
      LineWidth = lineWidth;
   }

   public int getLineColor() {
      return LineColor;
   }

   public float getLineWidth() {
      return LineWidth;
   }
}
