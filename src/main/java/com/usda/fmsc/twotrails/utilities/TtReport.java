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
    private PrintWriter logWriter;
    private final File logFile;

    public TtReport(File logFile) throws IOException {
        this.logFile = logFile;

        if (!logFile.exists()) {
            logFile.createNewFile();
        }

        logWriter = new PrintWriter(new FileOutputStream(logFile, true));
    }

    private PrintWriter getLogWriter() throws FileNotFoundException {
        return logWriter == null ? (logWriter = new PrintWriter(new FileOutputStream(logFile, true))) : logWriter;
    }

    public void closeReport() {
        if (logWriter != null) {
            logWriter.flush();
            logWriter.close();
            logWriter = null;
        }
    }

    public void clearReport() {
        closeReport();

        try {
            logWriter = new PrintWriter(new FileOutputStream(logFile, false));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void writeToReport(String text) {
        try {
            getLogWriter().println(text);
            getLogWriter().flush();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(Consts.LOG_TAG, text);
        }
    }

    public void writeError(String msg, String codePage) {
        String error = String.format("ERR[%s][%s]: %s", df.format(new Date()), codePage, msg);
        Log.e(Consts.LOG_TAG, error);
        writeToReport(error);
    }

    public void writeError(String msg, String codePage, StackTraceElement[] stack) {
        writeError(msg, codePage);
        writeStackTrace(stack);
    }

    public void writeEvent(String event) {
        String eStr = String.format("EVT[%s]: %s", df.format(new Date()), event);
        Log.i(Consts.LOG_TAG, eStr);
        writeToReport(eStr);
    }

    public void writeWarn(String msg, String codePage) {
        String error = String.format("WARN[%s][%s]: %s", df.format(new Date()), codePage, msg);
        Log.w(Consts.LOG_TAG, error);
        writeToReport(error);
    }

    public void writeWarn(String msg, String codePage, StackTraceElement[] stack) {
        writeWarn(msg, codePage);
        writeStackTrace(stack);
    }

    public void writeDebug(String msg, String codePage) {
        String error = String.format("DBG[%s][%s]: %s", df.format(new Date()), codePage, msg);
        Log.d(Consts.LOG_TAG, error);
        writeToReport(error);
    }

    private void writeStackTrace(StackTraceElement[] stack) {
        String msg = "Stack Trace:";
        Log.d(Consts.LOG_TAG, msg);
        writeToReport(msg);

        if (stack != null) {
            for (StackTraceElement ste : stack) {
                msg = String.format("%-5s%s", StringEx.Empty, ste.toString());
                Log.d(Consts.LOG_TAG, msg);
                writeToReport(msg);
            }
        }
    }
}
