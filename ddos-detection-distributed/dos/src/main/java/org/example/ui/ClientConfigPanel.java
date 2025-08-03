package org.example.ui;

import javax.swing.*;
import java.awt.*;

public class ClientConfigPanel extends JPanel {
    private final String ip;
    private final JCheckBox enabled;
    private final JSlider slider;

    public ClientConfigPanel(String ip) {
        this.ip = ip;
        setLayout(new FlowLayout(FlowLayout.LEFT,10,5));
        add(new JLabel("Client " + ip));
        enabled = new JCheckBox("Enabled");
        add(enabled);

        slider = new JSlider(0,1000,50);
        slider.setPreferredSize(new Dimension(200,40));
        slider.setMajorTickSpacing(250);
        slider.setMinorTickSpacing(50);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        add(new JLabel("Speed (req/s):"));
        add(slider);
    }

    public boolean isEnabled() { return enabled.isSelected(); }
    public int     getSpeed()   { return slider.getValue();      }
    public String  getIp()      { return ip;                    }
}
