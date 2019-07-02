package com.usda.fmsc.twotrails.utilities;

import android.util.Log;

import com.usda.fmsc.twotrails.Consts;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.usda.fmsc.utilities.StringEx;

public class TtReport {
    private static final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.ENGLISH);
    private static final String _fileName = "TwoTrailsLog.txt";
    private static TtReport inst = new TtReport();
    private static PrintWriter logWriter;
    private static String filePath;
    private static File logFile;

    public TtReport() {}

    private synchronized void changeFilePathString(String path) {
        closeFile();
        filePath = path;
        openFile(path);
    }

    public void changeDirectory(String path) {
        inst.changeFilePathString(path + File.separator + _fileName);
    }

    public String getFilePath() {
        return filePath;
    }

    public void closeReport() {
        inst.closeFile();
    }

    public void clearReport() {
        closeReport();
        try {
            logWriter = new PrintWriter(new FileOutputStream(logFile, false));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private synchronized void openFile(String path) {
        if (logFile == null) {
            logFile = new File(path);

            if (!logFile.exists()) {
                try {
                    logFile.createNewFile();
                } catch (IOException e) {
                    Log.e(Consts.LOG_TAG, "changeFilePathString-createNewFile:\n" + e.getMessage());
                }
            }
        }

        if (logFile != null && logWriter == null) {
            try {
                logWriter = new PrintWriter(new FileOutputStream(logFile, true));
            } catch (FileNotFoundException e) {
                Log.e(Consts.LOG_TAG, "changeFilePathString-logWriter:\n" + e.getMessage());
            }
        }
    }

    private synchronized void closeFile() {
        if (logWriter != null) {
            logWriter.flush();
            logWriter.close();
            logWriter = null;
        }

        if (logFile != null) {
            logFile = null;
        }
    }

    private synchronized void writeToReport(String text) {
        openFile(getFilePath());

        try {
            logWriter.println(text);
            logWriter.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d(Consts.LOG_TAG, text);
    }

    public void writeError(String msg, String codePage) {
        String error = String.format("ERR[%s][%s]: %s", df.format(new Date()), codePage, msg);
        Log.e(Consts.LOG_TAG, error);
        inst.writeToReport(error);
    }

    public void writeError(String msg, String codePage, StackTraceElement[] stack) {
        writeError(msg, codePage);
        writeStackTrace(stack);
    }

    public void writeEvent(String event) {
        String eStr = String.format("EVT[%s]: %s", df.format(new Date()), event);
        Log.i(Consts.LOG_TAG, eStr);
        inst.writeToReport(eStr);
    }

    public void writeWarn(String msg, String codePage) {
        String error = String.format("WARN[%s][%s]: %s", df.format(new Date()), codePage, msg);
        Log.w(Consts.LOG_TAG, error);
        inst.writeToReport(error);
    }

    public void writeWarn(String msg, String codePage, StackTraceElement[] stack) {
        writeWarn(msg, codePage);
        writeStackTrace(stack);
    }

    public void writeDebug(String msg, String codePage) {
        String error = String.format("DBG[%s][%s]: %s", df.format(new Date()), codePage, msg);
        Log.d(Consts.LOG_TAG, error);
        inst.writeToReport(error);
    }

    private void writeStackTrace(StackTraceElement[] stack) {
        String msg = "Stack Trace:";
        Log.d(Consts.LOG_TAG, msg);
        inst.writeToReport(msg);

        if (stack != null) {
            for (StackTraceElement ste : stack) {
                msg = String.format("%-5s%s", StringEx.Empty, ste.toString());
                Log.d(Consts.LOG_TAG, msg);
                inst.writeToReport(msg);
            }
        }
    }
}
