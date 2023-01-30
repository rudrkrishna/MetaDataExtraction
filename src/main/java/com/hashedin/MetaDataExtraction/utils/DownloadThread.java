package com.hashedin.MetaDataExtraction.utils;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.net.URL;

public class DownloadThread extends Thread {

    private volatile boolean running = true;
    private final String url;
    private final String fileName;


    public DownloadThread(String url, String fileName) {
        this.url = url;
        this.fileName = fileName;
    }

    @Override
    public void run() {
        try {
            URL downloadUrl = new URL(url);
            BufferedInputStream in = new BufferedInputStream(downloadUrl.openStream());
            FileOutputStream fos = new FileOutputStream(fileName);
            byte[] data = new byte[1024];
            int count;
            while ((count = in.read(data, 0, 1024)) != -1) {
                fos.write(data, 0, count);
            }
            fos.flush();
            fos.close();
            in.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (Thread.interrupted()) {
            running = false;
        }
    }

    public void stopThread() {
        interrupt();
    }

}