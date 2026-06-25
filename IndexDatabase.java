/*
Database class
*/

import java.util.List;
import java.util.ArrayList;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class IndexDatabase {
    private static final String DB_URL = "jdbc:sqlite:file_index.db";

    public IndexDatabase() {
        try (Connection connection = DriverManager.getConnection(DB_URL); Statement stmt = connection.createStatement()) {
            String createTableSQL = "CREATE TABLE IF NOT EXISTS files (filename TEXT, filepath TEXT UNIQUE);";
        stmt.execute(createTableSQL);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Literally just an add method for the SQLite db
    public void insert(Path file) {

    }

    // Also literally just a get method for the SQLite db
    public List<String> search(String query) {
        return null;
    }
}
