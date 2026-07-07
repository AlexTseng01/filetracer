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
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

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
import java.awt.Color;

public class MainFrame extends JFrame {

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	private JTextField directoryField;
	private JTextField searchField;
	private JTable table;
	
	private final FileTracerApp tracerApp;
	
	private double time = 0;
	private int entries = 0;
	private String dir = "";
	
	private JLabel countEntriesLabel;
	private JLabel scanTimeLabel;
	private JLabel throughputLabel;
	private JProgressBar progressBar;
	
    private static final String DB_URL = "jdbc:sqlite:file_index.db";
    static IndexDatabase db = new IndexDatabase();
    
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
					
					int producerCount = 8;
					int consumerCount = 4;
					BlockingQueue<Path> dirQueue = new ArrayBlockingQueue<>(10000);
			        BlockingQueue<Path> fileQueue = new ArrayBlockingQueue<>(10000);
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

	/**
	 * Create the frame.
	 */
	public MainFrame(FileTracerApp app) {
		this.tracerApp = app;
		
		setResizable(false);
		setTitle("MainFrame");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 734, 664);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		JPanel resultsPanel = new JPanel();
		resultsPanel.setBounds(10, 136, 698, 443);
		contentPane.add(resultsPanel);
		resultsPanel.setLayout(new GridLayout(0, 1, 0, 0));
		
		JScrollPane scrollPane = new JScrollPane();
		resultsPanel.add(scrollPane);
		
		// A table apparently
		table = new JTable();
		scrollPane.setViewportView(table);
		
		// Handle progress bar
		JPanel progressPanel = new JPanel();
		progressPanel.setBounds(10, 590, 698, 24);
		contentPane.add(progressPanel);
		progressPanel.setLayout(new GridLayout(0, 1, 0, 0));
		
		progressBar = new JProgressBar();
		progressBar.setForeground(Color.GREEN);
		progressPanel.add(progressBar);
		
		// Tool panel
		JPanel toolPanel = new JPanel();
		toolPanel.setBounds(10, 11, 698, 79);
		contentPane.add(toolPanel);
		toolPanel.setLayout(null);
		
		// Handle scanning
		JButton scanButton = new JButton("Scan");
		scanButton.setBounds(10, 11, 89, 23);
		toolPanel.add(scanButton);
		
		scanButton.addActionListener(e -> {
			countEntriesLabel.setText("Entries count: 0");
        	scanTimeLabel.setText("Scan time: 0.000");
        	throughputLabel.setText("Throughput: 0 files/s");
        	
		    Path origin = Paths.get(dir);

		    new Thread(() -> {
		        tracerApp.runScan(origin, new ScanListener() {
		            @Override
		            public void onProgress(int count) {
		                SwingUtilities.invokeLater(() -> {
		                	progressBar.setValue(count);
		                	progressBar.setString(count + " files");
		                	entries = count;
		                });
		            }

		            @Override
		            public void onComplete(double seconds) {
		                SwingUtilities.invokeLater(() -> {
		                	time = seconds;
		                	
		                	countEntriesLabel.setText("Entries count: " + db.showCount());
		                	scanTimeLabel.setText("Scan time: " + String.format("%.3f", time));
		                	throughputLabel.setText("Throughput: " + (int)(entries / time) + " files/sec");
		                	
		                	progressBar.setString("Done");
		                	progressBar.setValue(progressBar.getMaximum());
		                	progressBar.setValue(0);
		                    progressBar.setStringPainted(false);
		                });
		            }
		        });
		    }).start();
		});
		
		// Handle cleaning database
		JButton cleanButton = new JButton("Clean");
		cleanButton.setBounds(109, 11, 89, 23);
		toolPanel.add(cleanButton);
		
		cleanButton.addActionListener(e -> {
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
					
					int i = 0;
					for (String table : tables) {
						stmt.executeUpdate("DELETE FROM \"" + table + "\"");
						int progress = ++i;
						
						SwingUtilities.invokeLater(() -> {
		                    progressBar.setValue(progress);
		                    progressBar.setString("Cleaning... " + progress + "/" + total);
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
	                	entries = 0;
	                	
	                	countEntriesLabel.setText("Entries count: " + entries);
	                	scanTimeLabel.setText("Scan time: " + String.format("%.3f", time));
	                	throughputLabel.setText("Throughput: " + (int)(entries / time) + " files/s");
	                	
	                	progressBar.setValue(0);
	                    progressBar.setStringPainted(false);
	                });
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}).start();
		});
		
		// Handle directory input
		JLabel directoryLabel = new JLabel("Directory:");
		directoryLabel.setBounds(208, 17, 58, 14);
		toolPanel.add(directoryLabel);
		
		directoryField = new JTextField();
		directoryField.setBounds(276, 11, 186, 20);
		toolPanel.add(directoryField);
		directoryField.setColumns(10);
		
		directoryField.addActionListener(e -> {
		    dir = directoryField.getText().trim();
		    System.out.println("Directory set to: " + dir);
		});
		
		// Handle search input
		JLabel searchLabel = new JLabel("Search:");
		searchLabel.setBounds(208, 51, 58, 14);
		toolPanel.add(searchLabel);
		
		searchField = new JTextField();
		searchField.setBounds(276, 45, 186, 20);
		toolPanel.add(searchField);
		searchField.setColumns(10);
		
		// Handle sorting
		JComboBox sortComboBox = new JComboBox();
		sortComboBox.setModel(new DefaultComboBoxModel(new String[] {"Alphabetical (A-Z)", "Alphabetical (Z-A)", "Recently modified", "Oldest modified"}));
		sortComboBox.setBounds(544, 8, 144, 22);
		toolPanel.add(sortComboBox);
		
		JLabel sortLabel = new JLabel("Sort by:");
		sortLabel.setBounds(472, 14, 62, 14);
		toolPanel.add(sortLabel);
		
		// Handle exporting
		JButton pauseButton = new JButton("Pause");
		pauseButton.setBounds(109, 44, 89, 23);
		toolPanel.add(pauseButton);
		
		// Stop threads
		JButton stopButton = new JButton("Stop");
		stopButton.setBounds(10, 45, 89, 23);
		toolPanel.add(stopButton);
		
		stopButton.addActionListener(e -> {
		    tracerApp.stopScan();
		});
		
		// Handle filtering
		JLabel filterLabel = new JLabel("Filter by:");
		filterLabel.setBounds(472, 47, 62, 14);
		toolPanel.add(filterLabel);
		
		JComboBox filterComboBox = new JComboBox();
		filterComboBox.setModel(new DefaultComboBoxModel(new String[] {"All", "Folders", "Files"}));
		filterComboBox.setBounds(544, 43, 144, 22);
		toolPanel.add(filterComboBox);
		
		// Information panel
		JPanel infoPanel = new JPanel();
		infoPanel.setBounds(10, 101, 698, 24);
		contentPane.add(infoPanel);
		infoPanel.setLayout(null);
		
		// Count entries
		countEntriesLabel = new JLabel("Entries count: 0");
		countEntriesLabel.setBounds(10, 0, 128, 24);
		infoPanel.add(countEntriesLabel);
		
		// Scan time
		scanTimeLabel = new JLabel("Scan time: 0.000");
		scanTimeLabel.setBounds(148, 5, 128, 14);
		infoPanel.add(scanTimeLabel);
		
		// Throughput
		throughputLabel = new JLabel("Throughput: 0 files/s");
		throughputLabel.setBounds(286, 0, 500, 24);
		infoPanel.add(throughputLabel);
		
		// Others
		toolPanel.setBorder(BorderFactory.createDashedBorder(Color.GRAY));

	}
}
