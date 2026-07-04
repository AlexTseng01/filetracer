package com.alex.filetracer;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JScrollPane;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
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

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
					MainFrame frame = new MainFrame();
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
	public MainFrame() {
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
		
		table = new JTable();
		scrollPane.setViewportView(table);
		
		JPanel progressPanel = new JPanel();
		progressPanel.setBounds(10, 590, 698, 24);
		contentPane.add(progressPanel);
		progressPanel.setLayout(new GridLayout(0, 1, 0, 0));
		
		JProgressBar progressBar = new JProgressBar();
		progressBar.setForeground(Color.GREEN);
		progressPanel.add(progressBar);
		
		JPanel toolPanel = new JPanel();
		toolPanel.setBounds(10, 11, 698, 79);
		contentPane.add(toolPanel);
		toolPanel.setLayout(null);
		
		JButton scanButton = new JButton("Scan");
		scanButton.setBounds(10, 11, 89, 23);
		toolPanel.add(scanButton);
		
		JButton cleanButton = new JButton("Clean");
		cleanButton.setBounds(109, 11, 89, 23);
		toolPanel.add(cleanButton);
		
		JLabel directoryLabel = new JLabel("Directory:");
		directoryLabel.setBounds(208, 17, 58, 14);
		toolPanel.add(directoryLabel);
		
		directoryField = new JTextField();
		directoryField.setBounds(276, 11, 186, 20);
		toolPanel.add(directoryField);
		directoryField.setColumns(10);
		
		JLabel searchLabel = new JLabel("Search:");
		searchLabel.setBounds(208, 51, 58, 14);
		toolPanel.add(searchLabel);
		
		searchField = new JTextField();
		searchField.setBounds(276, 45, 186, 20);
		toolPanel.add(searchField);
		searchField.setColumns(10);
		
		JComboBox sortComboBox = new JComboBox();
		sortComboBox.setModel(new DefaultComboBoxModel(new String[] {"Alphabetical (A-Z)", "Alphabetical (Z-A)", "Recently modified", "Oldest modified"}));
		sortComboBox.setBounds(544, 8, 144, 22);
		toolPanel.add(sortComboBox);
		
		JLabel sortLabel = new JLabel("Sort by:");
		sortLabel.setBounds(472, 14, 62, 14);
		toolPanel.add(sortLabel);
		
		JButton exportButton = new JButton("Export");
		exportButton.setBounds(109, 44, 89, 23);
		toolPanel.add(exportButton);
		
		JButton stopButton = new JButton("Stop");
		stopButton.setBounds(10, 45, 89, 23);
		toolPanel.add(stopButton);
		
		JLabel filterLabel = new JLabel("Filter by:");
		filterLabel.setBounds(472, 47, 62, 14);
		toolPanel.add(filterLabel);
		
		JComboBox filterComboBox = new JComboBox();
		filterComboBox.setModel(new DefaultComboBoxModel(new String[] {"All", "Folders", "Files"}));
		filterComboBox.setBounds(544, 43, 144, 22);
		toolPanel.add(filterComboBox);
		
		JPanel infoPanel = new JPanel();
		infoPanel.setBounds(10, 101, 698, 24);
		contentPane.add(infoPanel);
		infoPanel.setLayout(null);
		
		JLabel countEntriesLabel = new JLabel("Entries count:");
		countEntriesLabel.setBounds(10, 0, 128, 24);
		infoPanel.add(countEntriesLabel);
		
		JLabel scanTimeLabel = new JLabel("Scan time:");
		scanTimeLabel.setBounds(148, 5, 128, 14);
		infoPanel.add(scanTimeLabel);
		
		JLabel throughputLabel = new JLabel("Throughput:");
		throughputLabel.setBounds(286, 0, 128, 24);
		infoPanel.add(throughputLabel);
		
		toolPanel.setBorder(BorderFactory.createDashedBorder(Color.GRAY));

	}
}
