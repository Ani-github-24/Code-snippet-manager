import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import com.fasterxml.jackson.databind.*;
import com.formdev.flatlaf.FlatDarkLaf;

public class SnippetManager extends JFrame {
    private JTree snippetTree;
    private JTextArea snippetContent;
    private Map<String, Snippet> snippets;
    private File dataFile = new File("snippets.json");
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode rootNode;

    public SnippetManager() {
        setTitle("Snippet Manager");
        setSize(900, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem exportItem = new JMenuItem("Export Snippets");
        exportItem.addActionListener(e -> exportSnippets());
        fileMenu.add(exportItem);
        fileMenu.addSeparator();
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Code Snippet Manager\nMade by Anirudha\nOrganize and view your code!"));
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);

        snippets = loadSnippets();

        rootNode = new DefaultMutableTreeNode("Snippets");
        buildTree();
        treeModel = new DefaultTreeModel(rootNode);
        snippetTree = new JTree(treeModel);
        snippetTree.setRootVisible(false);
        snippetTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) snippetTree.getLastSelectedPathComponent();
            if (selectedNode == null || !selectedNode.isLeaf()) return;
            String title = selectedNode.getUserObject().toString();
            Snippet snip = snippets.get(title);
            if (snip != null) {
                snippetContent.setText("// Language: " + snip.language + "\n\n" + snip.code);
            }
        });

        snippetContent = new JTextArea();
        snippetContent.setFont(new Font("Consolas", Font.PLAIN, 14));
        snippetContent.setMargin(new Insets(10, 10, 10, 10));
        snippetContent.setEditable(false);

        JScrollPane treeScroll = new JScrollPane(snippetTree);
        treeScroll.setBorder(BorderFactory.createTitledBorder("\uD83D\uDCC2 Categories"));
        JScrollPane codeScroll = new JScrollPane(snippetContent);
        codeScroll.setBorder(BorderFactory.createTitledBorder("\uD83D\uDCDD Code Viewer"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroll, codeScroll);
        splitPane.setDividerLocation(300);
        add(splitPane, BorderLayout.CENTER);

        JButton addButton = new JButton("➕ Add Snippet");
        addButton.setPreferredSize(new Dimension(140, 30));
        addButton.addActionListener(e -> addSnippet());

        JButton deleteButton = new JButton("🗑 Delete Snippet");
        deleteButton.setPreferredSize(new Dimension(140, 30));
        deleteButton.addActionListener(e -> deleteSnippet());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);
        buttonPanel.setBackground(new Color(35, 35, 35));
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void buildTree() {
        rootNode.removeAllChildren();
        Map<String, DefaultMutableTreeNode> categories = new HashMap<>();
        for (Snippet snip : snippets.values()) {
            String cat = snip.category != null && !snip.category.isEmpty() ? snip.category : "Uncategorized";
            categories.putIfAbsent(cat, new DefaultMutableTreeNode(cat));
            categories.get(cat).add(new DefaultMutableTreeNode(snip.title));
        }
        categories.values().forEach(rootNode::add);
    }

    private void refreshTree() {
        buildTree();
        treeModel.reload();
    }

    private void addSnippet() {
        JTextField titleField = new JTextField(20);
        JTextField langField = new JTextField(20);
        JTextField tagField = new JTextField(20);
        JTextField catField = new JTextField(20);
        JTextArea codeArea = new JTextArea(12, 50);
        codeArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        JScrollPane codeScroll = new JScrollPane(codeArea);

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        formPanel.add(new JLabel("Title:"));
        formPanel.add(titleField);
        formPanel.add(Box.createVerticalStrut(8));
        formPanel.add(new JLabel("Language:"));
        formPanel.add(langField);
        formPanel.add(Box.createVerticalStrut(8));
        formPanel.add(new JLabel("Tags (comma-separated):"));
        formPanel.add(tagField);
        formPanel.add(Box.createVerticalStrut(8));
        formPanel.add(new JLabel("Category (Folder):"));
        formPanel.add(catField);
        formPanel.add(Box.createVerticalStrut(10));
        formPanel.add(new JLabel("Code:"));
        formPanel.add(codeScroll);

        JScrollPane scrollPane = new JScrollPane(formPanel);
        scrollPane.setPreferredSize(new Dimension(550, 450));
        int result = JOptionPane.showConfirmDialog(this, scrollPane, "Add Snippet",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String title = titleField.getText().trim();
            String lang = langField.getText().trim();
            String tags = tagField.getText().trim();
            String cat = catField.getText().trim();
            String code = codeArea.getText();

            if (!title.isEmpty() && !code.isEmpty()) {
                Snippet snip = new Snippet(title, lang, tags, code, cat);
                snippets.put(title, snip);
                saveSnippets();
                refreshTree();
            }
        }
    }

    private void deleteSnippet() {
        TreePath path = snippetTree.getSelectionPath();
        if (path == null) return;
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        if (!selectedNode.isLeaf()) return;
        String title = selectedNode.getUserObject().toString();
        if (snippets.containsKey(title)) {
            snippets.remove(title);
            saveSnippets();
            refreshTree();
            snippetContent.setText("");
        }
    }

    private void exportSnippets() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("snippets_export.json"));
        int option = fileChooser.showSaveDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            ObjectMapper mapper = new ObjectMapper();
            try {
                mapper.writerWithDefaultPrettyPrinter().writeValue(file, snippets);
                JOptionPane.showMessageDialog(this, "Exported to " + file.getName());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Export failed!");
                ex.printStackTrace();
            }
        }
    }

    private Map<String, Snippet> loadSnippets() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            if (dataFile.exists()) {
                return mapper.readValue(dataFile,
                        mapper.getTypeFactory().constructMapType(HashMap.class, String.class, Snippet.class));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new HashMap<>();
    }

    private void saveSnippets() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(dataFile, snippets);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SnippetManager().setVisible(true));
    }

    static class Snippet {
        public String title;
        public String language;
        public String tags;
        public String code;
        public String category;
        public long createdAt;

        public Snippet() {}

        public Snippet(String title, String language, String tags, String code, String category) {
            this.title = title;
            this.language = language;
            this.tags = tags;
            this.code = code;
            this.category = category;
            this.createdAt = System.currentTimeMillis();
        }
    }
}
