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
        try {
            while (true) {
                Path path = queue.take();

                // Puts the poison pill back into the queue for other threads to finish
                if (path.toString().equals("__DONE__")) {
                    queue.put(path);
                    break;
                }

                // Inserts a path into the database as long as the thread does not reach a poison pill
                db.insert(path);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
