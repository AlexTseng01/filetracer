/*
 * Planned features:
 * 1. Locate empty folders
 * 2. Sort or filter file size
 * 3. Add column for file size
 * 4. Sort drop down menu replaced with clicking on database column names
 * 5. Locate empty files (0 bytes)
 * 6. Searching results in progress bar loading or flashing
 * 7. Batching file inserts (optimization, no need to shard databases because batching already sort of does that)
 * */

package com.alex.filetracer;

import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JScrollPane;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import javax.swing.JTextArea;

public class MainFrame extends JFrame {

	private static final long serialVersionUID = 1L;
	private final FileTracerApp tracerApp;
	
	private JPanel contentPane;
	
	private JTable table;
	
	private JTextField directoryField;
	private JTextField searchField;
	
	private JLabel countEntriesLabel;
	private JLabel scanTimeLabel;
	private JLabel throughputLabel;
	private JLabel nameLabel;
	private JLabel pathLabel;
	private JLabel sizeLabel;
	private JLabel creationLabel;
	private JLabel modifiedLabel;
	private JLabel typeLabel;
	
	private JProgressBar progressBar;
		
	private double time = 0;
	
	private String dir = "";
	
    private static final String DB_URL = "jdbc:sqlite:file_index.db";
    
    static IndexDatabase db = new IndexDatabase();
    
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
					
					// Settings
					int producerCount = 8;
					int consumerCount = 4;
					
					// Setup
					BlockingQueue<Path> dirQueue = new ArrayBlockingQueue<>(100000);
			        BlockingQueue<Path> fileQueue = new ArrayBlockingQueue<>(100000);
			        List<Thread> producers = new ArrayList<>();
			        List<Thread> consumers = new ArrayList<>();
			        
					FileTracerApp app = new FileTracerApp(producerCount, consumerCount, dirQueue, fileQueue, producers, consumers, db);
					
					MainFrame frame = new MainFrame(app);
					
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public MainFrame(FileTracerApp app) {
		this.tracerApp = app;
		
		setResizable(false);
		setTitle("FileTracer v0.0.1");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 795, 827);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		// Information 1 panel
		JPanel infoPanel_A = new JPanel();
		infoPanel_A.setBounds(10, 101, 758, 24);
		contentPane.add(infoPanel_A);
		infoPanel_A.setLayout(null);
		
		// Count entries
		countEntriesLabel = new JLabel("Entries count: " + db.showCount());
		countEntriesLabel.setBounds(10, 0, 128, 24);
		infoPanel_A.add(countEntriesLabel);
		
		// Scan time
		scanTimeLabel = new JLabel("Scan time: 0.000");
		scanTimeLabel.setBounds(148, 5, 128, 14);
		infoPanel_A.add(scanTimeLabel);
		
		// Throughput
		throughputLabel = new JLabel("Throughput: 0 files/sec");
		throughputLabel.setBounds(286, 0, 500, 24);
		infoPanel_A.add(throughputLabel);
		
		// Table panel
		JPanel tablePanel = new JPanel();
		tablePanel.setBounds(10, 136, 758, 443);
		contentPane.add(tablePanel);
		tablePanel.setLayout(new GridLayout(0, 1, 0, 0));
		
		JScrollPane scrollPane = new JScrollPane();
		tablePanel.add(scrollPane);
		
		table = new JTable();
		scrollPane.setViewportView(table);
		loadTable();
		
		table.getSelectionModel().addListSelectionListener(e -> {
		    if (!e.getValueIsAdjusting()) {

		        int selectedRow = table.getSelectedRow();

		        if (selectedRow == -1) {
		            return;
		        }

		        String fileName = table.getValueAt(selectedRow, 1).toString();
		        String filePath = table.getValueAt(selectedRow, 2).toString();

		        nameLabel.setText("Name: " + fileName);
		        pathLabel.setText("Path: " + filePath);

		        Path path = Paths.get(filePath);

		        try {
		            long size = Files.size(path);

		            sizeLabel.setText("Size: " + formatFileSize(size));

		            var attributes = Files.readAttributes(
		                path,
		                java.nio.file.attribute.BasicFileAttributes.class
		            );

		            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a").withZone(ZoneId.systemDefault());

		            creationLabel.setText("Created: " + formatter.format(attributes.creationTime().toInstant()));

		            modifiedLabel.setText("Modified: " + formatter.format(attributes.lastModifiedTime().toInstant()));

		            String name = path.getFileName().toString();
		            
		            int dotLocation = name.lastIndexOf('.');
		            
		            String extension;
		            
		            if (dotLocation != -1 && dotLocation < fileName.length() - 1) {
		            	extension = fileName.substring(dotLocation).toLowerCase();
		            }
		            else {
		            	extension = "folder";
		            }
		            
		            typeLabel.setText("Type: " + extension);

		        } catch (IOException ex) {
		            sizeLabel.setText("Size: Unknown");
		            creationLabel.setText("Created: Unknown");
		            modifiedLabel.setText("Modified: Unknown");
		            typeLabel.setText("Type: Unknown");
		        }
		    }
		});
		
		table.addMouseListener(new java.awt.event.MouseAdapter() {
		    @Override
		    public void mouseClicked(java.awt.event.MouseEvent e) {
		        if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {

		            int row = table.rowAtPoint(e.getPoint());

		            if (row == -1) {
		                return;
		            }

		            int modelRow = table.convertRowIndexToModel(row);

		            String filePath = table.getModel()
		                    .getValueAt(modelRow, 2)
		                    .toString();

		            openInFileExplorer(filePath);
		        }
		    }
		});
		
		// Tool panel
		JPanel toolPanel = new JPanel();
		toolPanel.setBounds(10, 11, 758, 79);
		contentPane.add(toolPanel);
		toolPanel.setLayout(null);
		
		// Scan button
		JButton scanButton = new JButton("Scan");
		scanButton.setBounds(10, 11, 89, 23);
		toolPanel.add(scanButton);
		scanButton.addActionListener(e -> {
			loadLabels(0,"0.000", 0);
        	
		    Path origin = Paths.get(dir);
		    
		    progressBar.setIndeterminate(true);
		    progressBar.setString("Scanning 0 files");
		    progressBar.setStringPainted(true);
		    
		    new Thread(() -> {
		        tracerApp.runScan(origin, new ScanListener() {
		            @Override
		            public void onProgress(int count) {
		                SwingUtilities.invokeLater(() -> {
		                	progressBar.setString("Scanning " + count + " files");
		                });
		            }

		            @Override
		            public void onComplete(double seconds) {
		                SwingUtilities.invokeLater(() -> {
		                	time = seconds;
		                	
		                	loadLabels(db.showCount(), String.format("%.3f", time), (int)(db.showCount() / time));
		                	
		                	progressBar.setIndeterminate(false);
		        		    progressBar.setStringPainted(false);
		                    
		                    loadTable();
		                    clearInfoPanel();
		                    clearSearchField();
		                });
		            }
		        });
		    }).start();
		});
		
		// Clean button
		JButton cleanButton = new JButton("Clean");
		cleanButton.setBounds(109, 11, 89, 23);
		toolPanel.add(cleanButton);
		cleanButton.addActionListener(e -> {
			tracerApp.stopScan();
			new Thread(() -> {
				try (Connection connection = DriverManager.getConnection(DB_URL); Statement stmt = connection.createStatement()) {
					connection.setAutoCommit(true);
					stmt.execute("PRAGMA foreign_keys = OFF");
					ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'");
					List<String> tables = new ArrayList<>();
					while (rs.next()) {
						tables.add(rs.getString(1));
					}
					rs.close();
					
					int total = tables.size();
					
					SwingUtilities.invokeLater(() -> {
		                progressBar.setMinimum(0);
		                progressBar.setMaximum(total);
		                progressBar.setValue(0);
		                progressBar.setStringPainted(true);
		            });
										
					for (String table : tables) {
						stmt.executeUpdate("DELETE FROM \"" + table + "\"");
						
						SwingUtilities.invokeLater(() -> {
		                    progressBar.setValue(total);
		                    progressBar.setString("Cleaning");
		                });
						
						try {
							Thread.sleep(50);
						} catch(InterruptedException ignored) {
							
						}
					}
					
					stmt.execute("PRAGMA foreign_keys = ON");
					
					SwingUtilities.invokeLater(() -> {
						progressBar.setValue(total);
	                	time = 0;
	                	
	                	loadLabels(db.showCount(), String.format("%.3f", time), (int)(db.showCount() / time));
	                	
	                	// Clearing the database is basically instant so I don't need to do any math
	                	progressBar.setValue(0);
	                    progressBar.setStringPainted(false);
	                    
	                    loadTable();
	                    clearInfoPanel();
	                    clearSearchField();
	                    clearDirectoryField();
	                });
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}).start();
		});
		
		// Stop button
		JButton stopButton = new JButton("Stop");
		stopButton.setBounds(10, 45, 89, 23);
		toolPanel.add(stopButton);
		stopButton.addActionListener(e -> {
		    tracerApp.stopScan();
		    
		    loadTable();
		    
		    progressBar.setIndeterminate(false);
		    progressBar.setValue(0);
		    progressBar.setStringPainted(false);
		});
				
		// Pause button
		JButton pauseButton = new JButton("Pause");
		pauseButton.setBounds(109, 44, 89, 23);
		toolPanel.add(pauseButton);
		
		// Directory field
		JLabel directoryLabel = new JLabel("Directory:");
		directoryLabel.setBounds(235, 17, 58, 14);
		toolPanel.add(directoryLabel);
		
		directoryField = new JTextField();
		directoryField.setBounds(303, 11, 186, 20);
		toolPanel.add(directoryField);
		directoryField.setColumns(10);
		directoryField.addActionListener(e -> {
		    dir = directoryField.getText().trim();
		    System.out.println("Directory set to: "+ dir);
		});
		
		// Search field
		JLabel searchLabel = new JLabel("Search:");
		searchLabel.setBounds(235, 51, 58, 14);
		toolPanel.add(searchLabel);
		
		searchField = new JTextField();
		searchField.setBounds(303, 45, 186, 20);
		toolPanel.add(searchField);
		searchField.setColumns(10);
		
		searchField.addActionListener(e -> {
			String sub = searchField.getText().trim();

			loadSearches(sub);
		});
		
		// Sort button
		JComboBox sortComboBox = new JComboBox();
		sortComboBox.setModel(new DefaultComboBoxModel(new String[] {"Alphabetical (A-Z)", "Alphabetical (Z-A)", "Recently modified", "Oldest modified", "Largest size", "Smallest size"}));
		sortComboBox.setBounds(602, 11, 144, 22);
		toolPanel.add(sortComboBox);
		
		JLabel sortLabel = new JLabel("Sort by:");
		sortLabel.setBounds(530, 17, 62, 14);
		toolPanel.add(sortLabel);
		
		sortComboBox.addActionListener(e -> {
			switch (sortComboBox.getSelectedIndex()) {
			case 0:
				loadSorted(Order.ALPHA_ASC);
				break;
			case 1:
				loadSorted(Order.ALPHA_DESC);
				break;
			case 2:
				loadSorted(Order.RECENTLY_MOD);
				break;
			case 3:
				loadSorted(Order.OLDEST_MOD);
				break;
			case 4:
				loadSorted(Order.SIZE_ASC);
				break;
			case 5:
				loadSorted(Order.SIZE_DESC);
				break;
			}
		});
		
		// Filter button
		JLabel filterLabel = new JLabel("Filter by:");
		filterLabel.setBounds(530, 50, 62, 14);
		toolPanel.add(filterLabel);
		
		JComboBox filterComboBox = new JComboBox();
		filterComboBox.setModel(new DefaultComboBoxModel(new String[] {"All", "Folders", "Files"}));
		filterComboBox.setBounds(602, 46, 144, 22);
		toolPanel.add(filterComboBox);
		
		// Information 2 panel
		JPanel infoPanel_B = new JPanel();
		infoPanel_B.setBounds(10, 590, 758, 160);
		contentPane.add(infoPanel_B);
		infoPanel_B.setLayout(null);
		
		// Display file name
		nameLabel = new JLabel("Name:");
		nameLabel.setBounds(10, 11, 738, 14);
		infoPanel_B.add(nameLabel);
		
		// Display full path
		pathLabel = new JLabel("Path:");
		pathLabel.setBounds(10, 36, 738, 14);
		infoPanel_B.add(pathLabel);
		
		// Display file size
		sizeLabel = new JLabel("Size:");
		sizeLabel.setBounds(10, 61, 738, 14);
		infoPanel_B.add(sizeLabel);
		
		// Display creation date
		creationLabel = new JLabel("Created:");
		creationLabel.setBounds(10, 86, 738, 14);
		infoPanel_B.add(creationLabel);
		
		// Display modified date
		modifiedLabel = new JLabel("Modified:");
		modifiedLabel.setBounds(10, 111, 738, 14);
		infoPanel_B.add(modifiedLabel);
		
		// Display file type
		typeLabel = new JLabel("Type:");
		typeLabel.setBounds(10, 136, 738, 14);
		infoPanel_B.add(typeLabel);
		
		// Progress bar
		JPanel progressPanel = new JPanel();
		progressPanel.setBounds(10, 761, 758, 24);
		contentPane.add(progressPanel);
		progressPanel.setLayout(new GridLayout(0, 1, 0, 0));
		
		progressBar = new JProgressBar();
		progressPanel.add(progressBar);
		progressBar.setForeground(Color.GREEN);
		
		// Cosmetics
		toolPanel.setBorder(BorderFactory.createDashedBorder(Color.GRAY));
		infoPanel_B.setBorder(BorderFactory.createDashedBorder(Color.GRAY));
	}
	
	// Update labels
	private void loadLabels(int count, String time, int throughput) {
		countEntriesLabel.setText("Entries count: " + count);
    	scanTimeLabel.setText("Scan time: " + time);
    	throughputLabel.setText("Throughput: " + throughput + " files/sec");
	}
	
	// Display entire database on table
	private void loadTable() {
		try {
			String sql = "SELECT filename, filepath, size, modified FROM files";
			
			Connection conn = DriverManager.getConnection(DB_URL);
			
			Statement stmt = conn.createStatement();
			
			ResultSet rs = stmt.executeQuery(sql);
			
			DefaultTableModel model = new DefaultTableModel();
			table.setDefaultEditor(Object.class, null);
			
			model.addColumn("#");
			model.addColumn("Name");
			model.addColumn("Path");
			model.addColumn("Size");
			model.addColumn("Modified");
			
			int number = 1;
			
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
			
			while(rs.next()) {
				long size = rs.getLong("size");
				long modified = rs.getLong("modified");
				
				String modifiedStr = formatter.format(Instant.ofEpochMilli(modified));
				
				model.addRow(new Object[] {number++, rs.getString("filename"), rs.getString("filepath"), formatFileSize(size), modifiedStr});
			}
			
			table.setModel(model);
			
			table.getColumnModel().getColumn(0).setPreferredWidth(80);
			table.getColumnModel().getColumn(1).setPreferredWidth(200);
			table.getColumnModel().getColumn(2).setPreferredWidth(500);
			table.getColumnModel().getColumn(3).setPreferredWidth(60);
			table.getColumnModel().getColumn(4).setPreferredWidth(110);
			
			rs.close();
			stmt.close();
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// Display search results on table
	private void loadSearches(String sub) {
		try {
			String sql = "SELECT filename, filepath, size, modified FROM files WHERE filename LIKE ? OR filepath LIKE ?";
			
			Connection conn = DriverManager.getConnection(DB_URL);
			
			PreparedStatement stmt = conn.prepareStatement(sql);
			
			String search = "%" + sub + "%";
			
			stmt.setString(1,  search);
			stmt.setString(2,  search);
			
			ResultSet rs = stmt.executeQuery();
			
			DefaultTableModel model = new DefaultTableModel();
			table.setDefaultEditor(Object.class, null);
			
			model.addColumn("#");
			model.addColumn("Name");
			model.addColumn("Path");
			model.addColumn("Size");
			model.addColumn("Modified");
			
			int number = 1;
			
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
			
			while(rs.next()) {
				long size = rs.getLong("size");
				long modified = rs.getLong("modified");
				
				String modifiedStr = formatter.format(Instant.ofEpochMilli(modified));
				
				model.addRow(new Object[] {number++, rs.getString("filename"), rs.getString("filepath"), formatFileSize(size), modifiedStr});
			}
			
			table.setModel(model);
			
			table.getColumnModel().getColumn(0).setPreferredWidth(80);
			table.getColumnModel().getColumn(1).setPreferredWidth(200);
			table.getColumnModel().getColumn(2).setPreferredWidth(500);
			table.getColumnModel().getColumn(3).setPreferredWidth(60);
			table.getColumnModel().getColumn(4).setPreferredWidth(110);
			
			rs.close();
			stmt.close();
			conn.close();
			
			countEntriesLabel.setText("Entries count: " + (number - 1));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// Display sorted results on table
	private void loadSorted(Order order) {
		try {
			String algorithm;
			
			switch (order) {
				case ALPHA_ASC:
					algorithm = "ORDER BY filename COLLATE NOCASE ASC";
					break;
				case ALPHA_DESC:
					algorithm = "ORDER BY filename COLLATE NOCASE DESC";
					break;
				case RECENTLY_MOD:
					algorithm = "ORDER BY modified DESC";
					break;
				case OLDEST_MOD:
					algorithm = "ORDER BY modified ASC";
					break;
				case SIZE_ASC:
					algorithm = "ORDER BY size DESC";
					break;
				case SIZE_DESC:
					algorithm = "ORDER BY size ASC";
					break;
				default:
					algorithm = "ORDER BY filename COLLATE NOCASE ASC";
					break;
			}
			
			String search = searchField.getText().trim();
			
			String sql = "SELECT filename, filepath, size, modified FROM files";
			
			if (!search.isEmpty()) {
				sql += " WHERE filename LIKE ? OR filepath LIKE ?";
			}
			
			sql += " " + algorithm;
			
			Connection conn = DriverManager.getConnection(DB_URL);
			
			PreparedStatement stmt = conn.prepareStatement(sql);
			
			if (!search.isEmpty()) {
				String searchPattern = "%" + search + "%";

				stmt.setString(1, searchPattern);
				stmt.setString(2, searchPattern);
			}
			
			ResultSet rs = stmt.executeQuery();
			
			DefaultTableModel model = new DefaultTableModel();
			table.setDefaultEditor(Object.class, null);
			
			model.addColumn("#");
			model.addColumn("Name");
			model.addColumn("Path");
			model.addColumn("Size");
			model.addColumn("Modified");
			
			int number = 1;
			
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
			
			while(rs.next()) {
				long size = rs.getLong("size");
				long modified = rs.getLong("modified");
				
				String modifiedStr = formatter.format(Instant.ofEpochMilli(modified));
				
				model.addRow(new Object[] {number++, rs.getString("filename"), rs.getString("filepath"), formatFileSize(size), modifiedStr});
			}
			
			table.setModel(model);
			
			table.getColumnModel().getColumn(0).setPreferredWidth(50);
			table.getColumnModel().getColumn(1).setPreferredWidth(180);
			table.getColumnModel().getColumn(2).setPreferredWidth(430);
			table.getColumnModel().getColumn(3).setPreferredWidth(60);
			table.getColumnModel().getColumn(4).setPreferredWidth(110);

			rs.close();
			stmt.close();
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
		
	// Format file size
	private String formatFileSize(long bytes) {
	    if (bytes < 1024) {
	        return bytes + " bytes";
	    }

	    if (bytes < 1024 * 1024) {
	        return String.format("%.2f KB", bytes / 1024.0);
	    }

	    if (bytes < 1024 * 1024 * 1024) {
	        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
	    }

	    return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
	}
		
	// Open a specific file in file explorer
	private void openInFileExplorer(String filePath) {
	    try {
	        Path path = Paths.get(filePath);

	        if (!Files.exists(path)) {
	            return;
	        }

	        if (Files.isDirectory(path)) {
	            new ProcessBuilder("explorer.exe", path.toString()).start();
	        } else {
	            new ProcessBuilder("explorer.exe","/select,",path.toString()).start();
	        }

	    } catch (IOException ex) {
	    	
	    }
	}
		
	// Clean up methods
	private void clearInfoPanel() {
		nameLabel.setText("Name:");
		pathLabel.setText("Path:");
		sizeLabel.setText("Size:");
		creationLabel.setText("Created:");
		modifiedLabel.setText("Modified:");
		typeLabel.setText("Type:");
	}
	
	// Clear search field
	private void clearSearchField() {
		searchField.setText("");
	}
	
	// Clear directory field
	private void clearDirectoryField() {
		directoryField.setText("");
	}
	
}
