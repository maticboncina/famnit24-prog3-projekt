package org.example.ui;

import javax.swing.*;
import java.awt.*;

public class FirewallLogPanel extends JPanel {
    private final JTextArea area;

    public FirewallLogPanel() {
        setLayout(new BorderLayout());
        area = new JTextArea();
        area.setEditable(false);
        add(new JScrollPane(area), BorderLayout.CENTER);
    }

    public void appendLog(String line) {
        SwingUtilities.invokeLater(() -> area.append(line));
    }
}
