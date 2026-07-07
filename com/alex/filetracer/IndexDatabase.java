/*
Database class
*/
package com.alex.filetracer;

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
    private Connection connection;

    public IndexDatabase() {
        try {
            this.connection = DriverManager.getConnection(DB_URL);
            
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL;");
                String createTableSQL = "CREATE TABLE IF NOT EXISTS files (filename TEXT, filepath TEXT UNIQUE);";
                stmt.execute(createTableSQL);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Literally just an add method for the SQLite db
    public synchronized void insert(Path file) {
        String sql = "INSERT OR IGNORE INTO files(filename, filepath) VALUES(?, ?)";
        
        String name = (file.getFileName() != null) ? file.getFileName().toString() : file.toString();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, file.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Also literally just a get method for the SQLite db
    public List<String> search(String query) {
        List<String> result = new ArrayList<>();
        String sql = "SELECT filepath FROM files WHERE filename LIKE ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, "%" + query + "%");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                result.add(rs.getString("filepath"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }
    
    public int showCount() {
        String sql = "SELECT COUNT(*) FROM files";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }
}
