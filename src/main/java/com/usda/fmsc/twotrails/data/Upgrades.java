package com.usda.fmsc.twotrails.data;

@SuppressWarnings("WeakerAccess")
public class Upgrades {
    public static final Upgrade DAL_2_0_3 = new Upgrade(TwoTrailsSchema.DAL_2_0_3, TwoTrailsSchema.UPGRADE_DAL_2_0_3);
    public static final Upgrade DAL_2_1_0 = new Upgrade(TwoTrailsSchema.DAL_2_1_0, TwoTrailsSchema.UPGRADE_DAL_2_1_0);
    public static final Upgrade DAL_2_2_0 = new Upgrade(TwoTrailsSchema.DAL_2_2_0, TwoTrailsSchema.UPGRADE_DAL_2_2_0);

    public static final Upgrade MAL_2_1_0 = new Upgrade(TwoTrailsMediaSchema.MAL_2_1_0, TwoTrailsMediaSchema.UPGRADE_MAL_2_1_0);
}
