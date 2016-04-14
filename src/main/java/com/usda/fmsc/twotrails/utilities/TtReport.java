package com.usda.fmsc.twotrails.utilities;

import android.util.Log;

import com.usda.fmsc.twotrails.Consts;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Date;

import com.usda.fmsc.utilities.StringEx;

public class TtReport {
    private static final String _fileName = "TwoTrailsLog.txt";
    private static TtReport inst = new TtReport();
    private static PrintWriter logWriter;
    private static String filePath;

    public TtReport() {}

    private synchronized void changeFilePathString(String path) {
        filePath = path;

        if (logWriter != null) {
            logWriter.close();
        }

        try {
            logWriter = new PrintWriter(new FileOutputStream(new File(path), true));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void changeFilePath(String path) {
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
            logWriter = new PrintWriter(new FileOutputStream(new File(filePath), false));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private synchronized void closeFile() {
        if (logWriter != null) {
            logWriter.flush();
            logWriter.close();
        }
    }

    private synchronized void writeToReport(String text) {
        try {
            logWriter.println(text);
            logWriter.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }

//        File file = new File(_FlePath);
//        FileOutputStream stream = null;


//        try {
//            stream = new FileOutputStream(file);
//            stream.write(String.format("%s%n", text).getBytes());
//            stream.close();
//        } catch (Exception e){
//            e.printStackTrace();
//        }

        Log.d(Consts.LOG_TAG, text);
    }

    public void writeError(String msg, String codePage) {
        String error = String.format("ERR[%s][%s]: %s", new Date(), codePage, msg);
        Log.e(Consts.LOG_TAG, error);
        inst.writeToReport(error);
    }

    public void writeError(String msg, String codePage, StackTraceElement[] stack) {
        writeError(msg, codePage);

        String error = String.format("Stack Trace: %n");
        Log.d(Consts.LOG_TAG, error);
        inst.writeToReport(error);

        if (stack != null) {
            for (StackTraceElement ste : stack) {
                error = String.format("%-5s%s", StringEx.Empty, ste.toString());
                Log.d(Consts.LOG_TAG, error);
                inst.writeToReport(error);
            }
        }
    }

    public void writeMessage(String msg, String codePage) {
        String msgStr = String.format("MSG[%s][%s]: %s", new Date(), codePage, msg);
        Log.d(Consts.LOG_TAG, msgStr);
        inst.writeToReport(msgStr);
    }

    public void writeMessage(String msg, String codePage, String adv) {
        writeMessage(msg, codePage);
        String msgStr = String.format("%-5s%s", StringEx.Empty, adv);
        Log.d(Consts.LOG_TAG, msgStr);
        inst.writeToReport(msgStr);
    }

    public void writeEvent(String event) {
        String eStr = String.format("EVT[%s]: %s", new Date(), event);
        Log.i(Consts.LOG_TAG, eStr);
        inst.writeToReport(eStr);
    }
}
