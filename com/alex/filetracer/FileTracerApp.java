/* 
Main class
*/
package com.alex.filetracer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class FileTracerApp {
    public void runScan(Path origin, ScanListener listener) {
    	long startTime = System.nanoTime();

        int producerCount = 8;
        int consumerCount = 4;
        final Path POISON = Path.of("__DONE__");

        BlockingQueue<Path> dirQueue = new ArrayBlockingQueue<>(10000);
        BlockingQueue<Path> fileQueue = new ArrayBlockingQueue<>(10000);

        AtomicInteger activeScanners = new AtomicInteger(0);
        AtomicInteger filesProcessed = new AtomicInteger(0);

        IndexDatabase db = new IndexDatabase();

        dirQueue.add(origin);

        // Create producer threads
        List<Thread> producers = new ArrayList<>();

        for (int i = 0; i < producerCount; i++) {
            Thread t = new Thread(new FileScanner(dirQueue, fileQueue, activeScanners, POISON));
            producers.add(t);
            t.start();
        }

        // Create consumer threads
        List<Thread> consumers = new ArrayList<>();
        
        for (int i = 0; i < consumerCount; i++) {
            Thread t = new Thread(() -> {
                try {
                    while (true) {
                        Path file = fileQueue.take();

                        if (file.equals(POISON)) {
                            break;
                        }

                        db.insert(file);
                        
                        int count = filesProcessed.incrementAndGet();
                        
                        if (listener != null) {
                        	listener.onProgress(count);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            consumers.add(t);
            t.start();
        }

        // Thread coordination
        System.out.println("Waiting on threads to finish...");

        while (true) {
            if (dirQueue.isEmpty() && activeScanners.get() == 0) {
                break;
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        for (int i = 0; i < producerCount; i++) {
            try {
                dirQueue.put(POISON);
            } catch (InterruptedException e) {
                
            }
        }

        for (Thread t : producers) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        for (int i = 0; i < consumerCount; i++) {
            try {
                fileQueue.put(POISON);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        for (Thread t : consumers) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Print duration
        long endTime = System.nanoTime();
        double seconds = (endTime - startTime) / 1_000_000_000.0;
        
        if (listener != null) {
        	listener.onComplete(seconds);
        }
        
//        System.out.printf("Execution time: %.3f seconds%n", seconds);
    }
    
}