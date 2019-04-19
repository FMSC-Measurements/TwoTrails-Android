package com.usda.fmsc.twotrails.data;

import com.usda.fmsc.twotrails.TwoTrailsApp;
import com.usda.fmsc.utilities.FileUtils;

@SuppressWarnings("WeakerAccess")
public class DataAccessUpgrader {
    public static UpgradeResult UpgradeDAL(DataAccessLayer dal) {
        FileUtils.copyFile(dal.getFilePath(), dal.getFilePath()  + ".old");

        try {
            if (dal.getVersion().toIntVersion() < TwoTrailsSchema.OSV_2_0_2.toIntVersion()) {
                return UpgradeResult.VersionUnsupported;
            } else if (dal.getVersion().toIntVersion() < TwoTrailsSchema.OSV_2_0_3.toIntVersion()) {
                if (dal.Upgrade(new Upgrade(TwoTrailsSchema.OSV_2_0_3, TwoTrailsSchema.UPGRADE_OSV_2_0_3))) {
                    return UpgradeResult.Successful;
                }
            }
        } catch (Exception ex) {
            TwoTrailsApp.getInstance().getReport().writeError(ex.getMessage(), "DataAccessUpgrader:UpgradeDAL", ex.getStackTrace());
        }

        return UpgradeResult.Failed;
    }

    public static class Upgrade {
        public final String SQL;
        public final TtVersion Version;

        public Upgrade(TtVersion version, String sql) {
            SQL = sql;
            Version = version;
        }
    }

    public enum UpgradeResult {
        Failed,
        Successful,
        VersionUnsupported
    }
}
