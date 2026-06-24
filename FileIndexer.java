/*
Consumer class
*/

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

    }
}
