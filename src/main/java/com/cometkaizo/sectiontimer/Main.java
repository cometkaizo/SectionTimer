package com.cometkaizo.sectiontimer;

import com.cometkaizo.sectiontimer.model.TimerData;
import com.cometkaizo.sectiontimer.persistence.TimerRepository;
import com.cometkaizo.sectiontimer.ui.MainFrame;
import com.cometkaizo.sectiontimer.ui.Theme;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.io.InputStream;

public final class Main {
    private Main() {
    }

    static void main() {
        SwingUtilities.invokeLater(() -> {
            useSystemLookAndFeel();
            Theme.install();

            TimerRepository repository = new TimerRepository();
            TimerData data = repository.load();
            MainFrame frame = new MainFrame(repository, data);
            frame.setVisible(true);
        });
    }

    private static void useSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception exception) {
            System.err.println("Could not apply the system look and feel: " + exception.getMessage());
        }
    }

    public static void log(String msg) {
        System.out.println(msg);
    }
    public static void err(String msg) {
        System.err.println(msg);
    }
    public static InputStream getResource(String p) {
        var resource = Main.class.getResourceAsStream(p.replaceAll("\\\\", "/"));
        if (resource == null) throw new IllegalStateException("Cannot find resource: " + p);
        return resource;
    }
}
