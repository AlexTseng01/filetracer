package com.alex.filetracer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.nio.file.AccessDeniedException;

public class FileScanner implements Runnable {
    private final BlockingQueue<Path> dirQueue;
    private final BlockingQueue<Path> fileQueue;
    private final AtomicInteger activeScanners;
    private final Path POISON;
    private AtomicBoolean alive;
    
    public FileScanner(BlockingQueue<Path> dirQueue, BlockingQueue<Path> fileQueue, AtomicInteger activeScanners, Path POISON, AtomicBoolean alive) {
        this.dirQueue = dirQueue;
        this.fileQueue = fileQueue;
        this.activeScanners = activeScanners;
        this.POISON = POISON;
        this.alive = alive;
    }

    @Override
    public void run() {
        while (alive.get()) {
            try {
                Path path = dirQueue.take();

                if (path.equals(POISON)) {
                    break;
                }

                activeScanners.incrementAndGet();

                try {
                    scan(path);
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
            	if (!alive.get()) {
            		return;
            	}
            	
                try {
                	while (alive.get()) {
                	    if (fileQueue.offer(path, 100, TimeUnit.MILLISECONDS)) {
                	        break;
                	    }
                	}
                    
                    if (!alive.get()) {
                		return;
                	}

                    if (Files.isDirectory(path)) {
                    	while (alive.get()) {
                    	    if (dirQueue.offer(path, 100, TimeUnit.MILLISECONDS)) {
                    	        break;
                    	    }
                    	}
                    }
                } catch (Exception ignored) {

                }
            });
        } catch (AccessDeniedException e) {
        	
        }
    }
}