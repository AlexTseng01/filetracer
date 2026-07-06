/*
Producer class
*/
package com.alex.filetracer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.nio.file.AccessDeniedException;

public class FileScanner implements Runnable {
    private final BlockingQueue<Path> dirQueue; // All producer threads share
    private final BlockingQueue<Path> fileQueue; // Consumed by consumers
    private final AtomicInteger activeScanners;
    private final Path POISON;
    
    public FileScanner(BlockingQueue<Path> dirQueue, BlockingQueue<Path> fileQueue, AtomicInteger activeScanners, Path POISON) {
        this.dirQueue = dirQueue;
        this.fileQueue = fileQueue;
        this.activeScanners = activeScanners;
        this.POISON = POISON;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Path dir = dirQueue.take();

                if (dir.equals(POISON)) {
                    break;
                }

                activeScanners.incrementAndGet();

                try {
                    scan(dir);
//                    System.out.println(dir);
                } finally {
                    activeScanners.decrementAndGet();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void scan(Path dir) throws Exception {
        try (var stream = Files.list(dir)) {
            stream.forEach(path -> {
                try {
                    // Consumers will consume this
                    fileQueue.put(path);

                    if (Files.isDirectory(path)) {
                        // Producers will consume this
                        dirQueue.put(path);
                    }
                } catch (Exception ignored) {

                }
            });
        } catch (AccessDeniedException e) {
            System.out.println("Access denied: " + dir);
        }
    }
}