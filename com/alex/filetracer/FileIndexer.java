/*
Consumer class
*/
package com.alex.filetracer;

import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;

public class FileIndexer implements Runnable {
    private final IndexDatabase db;
    private final BlockingQueue<Path> queue;

    public FileIndexer(IndexDatabase db, BlockingQueue<Path> queue) {
        this.db = db;
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Path path = queue.take();

                if (path.toString().equals("__DONE__")) {
                    queue.put(path);
                    break;
                }

                db.insert(path);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
