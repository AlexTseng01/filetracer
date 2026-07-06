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
	private int producerCount;
	private int consumerCount;
	private BlockingQueue<Path> dirQueue;
	private BlockingQueue<Path> fileQueue;
	List<Thread> producers;
	List<Thread> consumers;
	
	private final Path POISON = Path.of("__DONE__");
	
	public FileTracerApp(int producerCount, int consumerCount, BlockingQueue<Path> dirQueue, BlockingQueue<Path> fileQueue, List<Thread> producers, List<Thread> consumers) {
		this.producerCount = producerCount;
		this.consumerCount = consumerCount;
		this.dirQueue = dirQueue;
		this.fileQueue = fileQueue;
		this.producers = producers;
		this.consumers = consumers;
	}
	
    public void runScan(Path origin, ScanListener listener) {
    	long startTime = System.nanoTime();

        AtomicInteger activeScanners = new AtomicInteger(0);
        AtomicInteger filesProcessed = new AtomicInteger(0);
        
        IndexDatabase db = new IndexDatabase();

        dirQueue.add(origin);

        // Create producer threads
        for (int i = 0; i < producerCount; i++) {
            Thread t = new Thread(new FileScanner(dirQueue, fileQueue, activeScanners, POISON));
            producers.add(t);
            t.start();
        }

        // Create consumer threads        
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
        
    }
    
    public void stopScan() {
    	for (Thread t : producers) {
    		t.interrupt();
    	}
    	
    	for (Thread t : consumers) {
    		t.interrupt();
    	}
    	
    	dirQueue.clear();
    	fileQueue.clear();
    	
    	dirQueue.offer(POISON);
    	fileQueue.offer(POISON);
    	
    	System.out.println("Stopped");
    }
    
}