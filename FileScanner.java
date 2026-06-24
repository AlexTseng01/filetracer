/*
Producer class
*/

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.BlockingQueue;

public class FileScanner implements Runnable {
    private final Path root;
    private final BlockingQueue<Path> queue;
    
    public FileScanner(Path root, BlockingQueue<Path> queue) {
        this.root = root;
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            scan(root);
            queue.put(Paths.get("__DONE__"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Scans an entire directory, puts Paths in a BlockingQueue, consumer class CONSUMES the stuff
    private void scan(Path dir) throws Exception {
        Files.list(dir).forEach(path -> {
            try {
                if (Files.isDirectory(path)) {scan(path);}
                else {queue.put(path);}
            } catch (Exception ignored) {}
        });
    }
}
