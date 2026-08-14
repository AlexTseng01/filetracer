/*
Consumer class
*/
package com.alex.filetracer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class FileIndexer implements Runnable {
    private final IndexDatabase db;
    private final BlockingQueue<Path> fileQueue;
    private final AtomicInteger filesProcessed;
    private final ScanListener listener;
    private final Path POISON;

    public FileIndexer(IndexDatabase db, BlockingQueue<Path> fileQueue, AtomicInteger filesProcessed, ScanListener listener, Path POISON) {
        this.db = db;
        this.fileQueue = fileQueue;
        this.filesProcessed = filesProcessed;
        this.listener = listener;
        this.POISON = POISON;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Path path = fileQueue.take();
                
                if (path.equals(POISON)) {
                	fileQueue.put(path);
                    break;
                }
                
                long size = Files.isDirectory(path) ? 0L : Files.size(path);
                db.insert(path, size);
                
                System.out.println("Indexing: " + path);
                
                int count = filesProcessed.incrementAndGet();
                
                if (listener != null) {
                	listener.onProgress(count);
                }
            }
        } catch (Exception e) {
        	Thread.currentThread().interrupt();
        }
    }
}
