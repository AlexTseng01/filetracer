/*
Long story short, this is a producer class
*/

import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;

public class FileScanner implements Runnable {
    private final BlockingQueue<Path> queue;
    private final Path root;
    
    // Responsible for 
    public FileScanner(Path root, BlockingQueue<Path> queue) {
        this.root = root;
        this.queue = queue;
    }

    // For threading
    @Override
    public void run() {

    }

    // Traverse file path
    private void scan() throws Exception {
        
    }
}
