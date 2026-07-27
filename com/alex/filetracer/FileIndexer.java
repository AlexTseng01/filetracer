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
                
                System.out.println("Consumer threads are processing: " + path);
                
                if (path.equals(POISON)) {
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
        
        System.out.println("Consumer thread terminated");
    }
}
