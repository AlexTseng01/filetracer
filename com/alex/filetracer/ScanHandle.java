package com.alex.filetracer;

import java.nio.file.Path;
import java.util.List;
//import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScanHandle {
	private final List<Thread> producers;
	private final List<Thread> consumers;
	private final BlockingQueue<Path> dirQueue;
    private final BlockingQueue<Path> fileQueue;
    private final Path poison;
    private final AtomicBoolean running;
    
    public ScanHandle(List<Thread> producers, List<Thread> consumers, BlockingQueue<Path> dirQueue, BlockingQueue<Path> fileQueue, Path poison, AtomicBoolean running) {
    	this.producers = producers;
        this.consumers = consumers;
        this.dirQueue = dirQueue;
        this.fileQueue = fileQueue;
        this.poison = poison;
        this.running = running;
    }
    
    public void stop() {
    	running.set(false);
    	
    	dirQueue.offer(poison);
    	fileQueue.offer(poison);
    	
    	for (Thread t : producers) t.interrupt();
    	for (Thread t : consumers) t.interrupt();
    }
}
