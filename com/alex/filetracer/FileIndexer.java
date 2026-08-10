/*
Consumer class
*/
package com.alex.filetracer;

import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class FileIndexer implements Runnable {
    private final IndexDatabase db;
    private final BlockingQueue<Path> fileQueue;
    private final AtomicInteger filesProcessed;
    private final ScanListener listener;
    private LogListener logListener;
    private final Path POISON;

    public FileIndexer(IndexDatabase db, BlockingQueue<Path> fileQueue, AtomicInteger filesProcessed, ScanListener listener, Path POISON, LogListener logListener) {
        this.db = db;
        this.fileQueue = fileQueue;
        this.filesProcessed = filesProcessed;
        this.listener = listener;
        this.POISON = POISON;
        this.logListener = logListener;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Path path = fileQueue.take();
                
                if (!path.equals(POISON)) {
//                	log("indexing: " + path);
                }
                else {
                	fileQueue.put(path);
                    break;
                }
                
                db.insert(path);
                
                int count = filesProcessed.incrementAndGet();
                
                if (listener != null) {
                	listener.onProgress(count);
                }
            }
        } catch (Exception e) {
        	Thread.currentThread().interrupt();
        }
    }
    
    public void setLogListener(LogListener logListener) {
        this.logListener = logListener;
    }

    private void log(String message) {
        if (logListener != null) {
            logListener.onLog(message);
        }
    }
}
