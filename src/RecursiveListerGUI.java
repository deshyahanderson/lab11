package src;
import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class RecursiveListerGUI extends JFrame implements ActionListener {

    // --- GUI Components ---
    private JLabel titleLabel;
    private JTextArea fileListTextArea;
    private JScrollPane scrollPane;
    private JButton startButton;
    private JButton quitButton;
    private JFileChooser directoryChooser; // To select the directory

    // --- Constructor ---
    public RecursiveListerGUI() {
        // Frame setup
        super("Recursive File Lister"); // Set the window title
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Close on exit
        setSize(600, 400); // Set initial size
        setLocationRelativeTo(null); // Center the window

        // --- Component Initialization ---
        titleLabel = new JLabel("Recursive Directory Lister", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Serif", Font.BOLD, 20));

        fileListTextArea = new JTextArea();
        fileListTextArea.setEditable(false); // Prevent user editing
        scrollPane = new JScrollPane(fileListTextArea); // Wrap text area in a scroll pane

        startButton = new JButton("Select Directory and List");
        quitButton = new JButton("Quit");

        // Configure the file chooser to select directories only
        directoryChooser = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
        directoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); // Crucial setting

        // --- Layout Setup ---
        // Using BorderLayout for main frame layout
        setLayout(new BorderLayout(10, 10));

        // Add components to the frame
        add(titleLabel, BorderLayout.NORTH); // Title at the top
        add(scrollPane, BorderLayout.CENTER); // Text area in the center

        // Panel for buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10)); // Use FlowLayout for buttons
        buttonPanel.add(startButton);
        buttonPanel.add(quitButton);

        add(buttonPanel, BorderLayout.SOUTH); // Button panel at the bottom

        // --- Event Handling ---
        startButton.addActionListener(this); // Register this class as listener for startButton
        quitButton.addActionListener(this);  // Register this class as listener for quitButton

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            SwingUtilities.updateComponentTreeUI(this); // Update components
        } catch (Exception e) {
            System.err.println("Could not set look and feel: " + e.getMessage());
        }


        // Make the frame visible
        setVisible(true);
    }

    // --- ActionListener Implementation ---
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == startButton) {
            // Handle Start button click
            int returnValue = directoryChooser.showOpenDialog(this); // Show the directory chooser dialog

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File selectedDirectory = directoryChooser.getSelectedFile();
                if (selectedDirectory != null) {
                    fileListTextArea.setText(""); // Clear the text area before listing

                    // Start the recursive listing in a separate thread
                    // to prevent blocking the GUI
                    new Thread(() -> {
                        try {
                            listDirectoryContents(selectedDirectory.toPath());
                        } catch (IOException ex) {
                            // Update GUI with error message on the Event Dispatch Thread (EDT)
                            SwingUtilities.invokeLater(() ->
                                    fileListTextArea.append("Error listing directory: " + ex.getMessage() + "\n")
                            );
                            ex.printStackTrace();
                        }

                    }).start();
                }
            }
        } else if (e.getSource() == quitButton) {
            // Handle Quit button click
            System.exit(0); // Exit the application
        }
    }

    // --- Recursive Directory Listing Method ---
    private void listDirectoryContents(Path currentPath) throws IOException {
        // Update the text area with the current item on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            // Basic indentation for directories, or just print the path
            if (Files.isDirectory(currentPath)) {
                fileListTextArea.append("DIR: " + currentPath.toString() + "/\n");
            } else {
                fileListTextArea.append("FILE: " + currentPath.toString() + "\n");
            }
        });

        if (Files.isDirectory(currentPath)) {
            try (Stream<Path> entries = Files.list(currentPath)) {
                entries.forEach(entry -> {
                    try {
                        listDirectoryContents(entry); // Recurse on each entry
                    } catch (IOException e) {
                        // Update GUI with error for a specific entry
                        SwingUtilities.invokeLater(() ->
                                fileListTextArea.append("Error accessing " + entry.toString() + ": " + e.getMessage() + "\n")
                        );
                    }
                });
            } // Stream is closed automatically
        }
        // Base case (file) returns implicitly after appending to the text area.
    }


    // --- Main Method ---
    public static void main(String[] args) {
        // Run the GUI creation on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(RecursiveListerGUI::new);
    }
}
