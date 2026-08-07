package com.cometkaizo.sectiontimer.ui;

import com.cometkaizo.sectiontimer.model.TimerData;
import com.cometkaizo.sectiontimer.model.TimerProfile;
import com.cometkaizo.sectiontimer.persistence.TimerRepository;
import com.cometkaizo.sectiontimer.timer.TimerEngine;
import com.cometkaizo.sectiontimer.timer.TimerState;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.MouseInfo;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class MainFrame extends JFrame {
    private final TimerRepository repository;
    private final TimerData data;
    private final TimerEngine timerEngine;

    private final CircularTimerPanel timerPanel = new CircularTimerPanel();
    private final JLabel profileNameLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel profilePositionLabel = new JLabel("", SwingConstants.CENTER);
    private final JButton previousButton = new RoundedButton("‹", 2, 16, 6, 16);
    private final JButton nextButton = new RoundedButton("›", 2, 16, 6, 16);
    private final JButton deleteButton = new RoundedButton("Delete");
    private final MiniSessionBar miniSessionBar = new MiniSessionBar();
    private final JLabel miniProfileNameLabel = new JLabel("", SwingConstants.CENTER);
    private final javax.swing.Timer miniHoverTimer = new javax.swing.Timer(
            1_000 / 60, event -> updateMiniHoverState());
    private final JPanel normalContent;
    private final JPanel miniContent;
    private JPanel normalTimerArea;
    private JPanel miniTimerArea;

    private int currentProfileIndex;
    private Rectangle normalBounds;
    private boolean miniMode;
    private double miniTitleOpacity;

    public MainFrame(TimerRepository repository, TimerData data) {
        super("Drawing Timer");
        this.repository = repository;
        this.data = data;
        this.currentProfileIndex = clampProfileIndex(data.getCurrentProfileIndex());
        this.timerEngine = new TimerEngine(
                this::handleTimerState,
                () -> Assets.sound("warning_1").play(),
                () -> Assets.sound("warning_2").play()
        );

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(650, 650));
        normalContent = buildNormalContent();
        miniContent = buildMiniContent();
        normalContent.addMouseWheelListener(event -> {
            if (miniMode || event.getPreciseWheelRotation() == 0
                    || data.getProfiles().size() < 2) {
                return;
            }
            Assets.sound("button_up").play();
            switchProfile(event.getPreciseWheelRotation() > 0 ? 1 : -1);
        });
        setContentPane(normalContent);
        setIconImage(Assets.texture("icon"));

        installActions();
        installWindowListener();
        showCurrentProfile();

        pack();
        setSize(Math.max(getWidth(), 650), Math.max(getHeight(), 650));
        setLocationRelativeTo(null);
    }

    private JPanel buildNormalContent() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.add(buildHeader(), BorderLayout.NORTH);
        panel.add(buildTimerArea(), BorderLayout.CENTER);
        panel.add(buildBottomButtons(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildMiniContent() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JButton expandButton = new RoundedButton("↗", 9, 6, 9, 6);
        expandButton.setToolTipText("Return to full-size mode");
        expandButton.addActionListener(event -> leaveMiniMode());

        JPanel leftColumn = new JPanel(new BorderLayout(0, 4));
        leftColumn.add(expandButton, BorderLayout.NORTH);
        leftColumn.add(miniSessionBar, BorderLayout.CENTER);

        JPanel dragBar = new JPanel();
        dragBar.setLayout(new BorderLayout());
        dragBar.setOpaque(false);
        dragBar.setPreferredSize(new Dimension(1, expandButton.getPreferredSize().height));
        dragBar.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        dragBar.setToolTipText("Drag to move the mini timer");
        miniProfileNameLabel.setFont(miniProfileNameLabel.getFont().deriveFont(Font.PLAIN, 11f));
        miniProfileNameLabel.setForeground(Theme.MUTED_TEXT);
        miniProfileNameLabel.setVisible(false);
        dragBar.add(miniProfileNameLabel, BorderLayout.CENTER);
        installMiniWindowDragging(dragBar);
        installMiniWindowDragging(miniProfileNameLabel);

        miniTimerArea = new JPanel(new BorderLayout());
        miniTimerArea.add(dragBar, BorderLayout.NORTH);

        panel.add(leftColumn, BorderLayout.WEST);
        panel.add(miniTimerArea, BorderLayout.CENTER);
        return panel;
    }

    private void installMiniWindowDragging(JComponent dragArea) {
        MouseAdapter dragListener = new MouseAdapter() {
            private Point dragStart;
            private Point windowStart;

            @Override public void mousePressed(MouseEvent event) {
                if (event.getButton() == MouseEvent.BUTTON1) {
                    dragStart = event.getLocationOnScreen();
                    windowStart = getLocation();
                }
            }

            @Override public void mouseDragged(MouseEvent event) {
                if (dragStart == null || windowStart == null) return;
                Point screenPoint = event.getLocationOnScreen();
                setLocation(
                        windowStart.x + screenPoint.x - dragStart.x,
                        windowStart.y + screenPoint.y - dragStart.y
                );
            }

            @Override public void mouseReleased(MouseEvent event) {
                dragStart = null;
                windowStart = null;
            }
        };
        dragArea.addMouseListener(dragListener);
        dragArea.addMouseMotionListener(dragListener);
    }

    private void updateMiniHoverState() {
        if (!miniMode || !isShowing()) {
            miniTitleOpacity = 0.0;
            miniProfileNameLabel.setVisible(false);
            return;
        }
        Point pointer = MouseInfo.getPointerInfo().getLocation();
        double targetOpacity = getBounds().contains(pointer) ? 1.0 : 0.0;
        miniTitleOpacity += (targetOpacity - miniTitleOpacity) * 0.18;
        if (Math.abs(targetOpacity - miniTitleOpacity) < 0.01) {
            miniTitleOpacity = targetOpacity;
        }

        Color baseColor = Theme.MUTED_TEXT;
        miniProfileNameLabel.setForeground(new Color(
                baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(),
                (int) Math.round(miniTitleOpacity * 255.0)));
        miniProfileNameLabel.setVisible(miniTitleOpacity > 0.0);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(4, 2));
        header.setBorder(BorderFactory.createEmptyBorder(14, 14, 0, 14));

        profileNameLabel.setFont(profileNameLabel.getFont().deriveFont(Font.BOLD, 40f));
        profilePositionLabel.setFont(profilePositionLabel.getFont().deriveFont(Font.PLAIN, 13f));

        header.add(profileNameLabel, BorderLayout.CENTER);
        header.add(profilePositionLabel, BorderLayout.SOUTH);
        return header;
    }

    private JPanel buildTimerArea() {
        JPanel area = new JPanel(new GridBagLayout());
        normalTimerArea = area;
        area.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));

        previousButton.setFont(previousButton.getFont().deriveFont(Font.BOLD, 34f));
        nextButton.setFont(nextButton.getFont().deriveFont(Font.BOLD, 34f));
        previousButton.setToolTipText("Previous timer profile (Left arrow)");
        nextButton.setToolTipText("Next timer profile (Right arrow)");
        previousButton.addActionListener(event -> switchProfile(-1));
        nextButton.addActionListener(event -> switchProfile(1));

        timerPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getButton() == MouseEvent.BUTTON1) {
                    timerEngine.handleTimerCircleClick();
                }
            }
        });
        timerPanel.setEditAction(this::editCurrentTimer);

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.insets = new Insets(4, 8, 4, 8);
        c.fill = GridBagConstraints.NONE;

        c.gridx = 0;
        c.weightx = 0;
        area.add(previousButton, c);

        c.gridx = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        area.add(timerPanel, c);

        c.gridx = 2;
        c.weightx = 0;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        area.add(nextButton, c);

        return area;
    }

    private JPanel buildBottomButtons() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        JButton miniButton = new RoundedButton("Mini mode");
        miniButton.setToolTipText("Move the timer to the bottom-left corner");
        miniButton.addActionListener(event -> enterMiniMode());

        JButton createButton = new RoundedButton("Create New");
        createButton.addActionListener(event -> createNewTimer());

        deleteButton.addActionListener(event -> deleteCurrentTimer());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        actions.add(createButton);
        actions.add(deleteButton);
        panel.add(miniButton, BorderLayout.WEST);
        panel.add(actions, BorderLayout.CENTER);
        return panel;
    }

    private void enterMiniMode() {
        if (miniMode) return;
        normalBounds = getBounds();
        miniMode = true;
        dispose();
        setUndecorated(true);
        setAlwaysOnTop(true);
        timerPanel.setCompactMode(true);
        miniTimerArea.add(timerPanel, BorderLayout.CENTER);
        setContentPane(miniContent);
        setMinimumSize(new Dimension(230, 180));
        setSize(230, 220);
        setShape(new RoundRectangle2D.Double(
                0, 0, getWidth(), getHeight(), 24, 24));
        positionAtBottomLeft();
        revalidate();
        repaint();
        setVisible(true);
        miniHoverTimer.start();
    }

    private void leaveMiniMode() {
        if (!miniMode) return;
        miniMode = false;
        miniHoverTimer.stop();
        miniTitleOpacity = 0.0;
        miniProfileNameLabel.setVisible(false);
        dispose();
        setShape(null);
        setUndecorated(false);
        setAlwaysOnTop(false);
        timerPanel.setCompactMode(false);
        GridBagConstraints timerConstraints = new GridBagConstraints();
        timerConstraints.gridx = 1;
        timerConstraints.gridy = 0;
        timerConstraints.weightx = 1;
        timerConstraints.weighty = 1;
        timerConstraints.insets = new Insets(4, 8, 4, 8);
        timerConstraints.fill = GridBagConstraints.BOTH;
        normalTimerArea.add(timerPanel, timerConstraints);
        setContentPane(normalContent);
        setMinimumSize(new Dimension(650, 650));
        if (normalBounds != null) setBounds(normalBounds);
        revalidate();
        repaint();
        setVisible(true);
    }

    private void positionAtBottomLeft() {
        GraphicsConfiguration configuration = getGraphicsConfiguration();
        Rectangle usable = configuration == null
                ? GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds()
                : configuration.getBounds();
        java.awt.Insets screenInsets = configuration == null
                ? new java.awt.Insets(0, 0, 0, 0)
                : getToolkit().getScreenInsets(configuration);
        int x = usable.x + screenInsets.left;
        int y = usable.y + usable.height - screenInsets.bottom - getHeight();
        setLocation(x, y);
    }

    private void createNewTimer() {
        timerEngine.stop();
        Set<String> existingNames = new HashSet<>();
        for (TimerProfile profile : data.getProfiles()) {
            existingNames.add(profile.getName());
        }

        TimerProfile newProfile = TimerEditorDialog.showDialog(this, existingNames);
        if (newProfile == null) {
            showCurrentProfile();
            return;
        }

        data.getProfiles().add(newProfile);
        currentProfileIndex = data.getProfiles().size() - 1;
        data.setCurrentProfileIndex(currentProfileIndex);
        saveDataWithMessage();
        showCurrentProfile();
    }

    private void editCurrentTimer() {
        List<TimerProfile> profiles = data.getProfiles();
        if (profiles.isEmpty()) {
            return;
        }

        timerEngine.stop();
        TimerProfile currentProfile = profiles.get(currentProfileIndex);
        Set<String> otherNames = new HashSet<>();
        for (int i = 0; i < profiles.size(); i++) {
            if (i != currentProfileIndex) {
                otherNames.add(profiles.get(i).getName());
            }
        }

        TimerProfile editedProfile = TimerEditorDialog.showEditDialog(
                this, otherNames, currentProfile);
        if (editedProfile != null) {
            profiles.set(currentProfileIndex, editedProfile);
            saveDataWithMessage();
        }
        showCurrentProfile();
    }

    private void deleteCurrentTimer() {
        List<TimerProfile> profiles = data.getProfiles();
        if (profiles.isEmpty()) {
            return;
        }

        TimerProfile profile = profiles.get(currentProfileIndex);
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Delete ‘" + profile.getName() + "’?",
                "Delete timer",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        timerEngine.stop();
        profiles.remove(currentProfileIndex);
        currentProfileIndex = clampProfileIndex(currentProfileIndex);
        data.setCurrentProfileIndex(currentProfileIndex);
        saveDataWithMessage();
        showCurrentProfile();
    }

    private void switchProfile(int direction) {
        List<TimerProfile> profiles = data.getProfiles();
        if (profiles.size() < 2) {
            return;
        }

        timerEngine.stop();
        currentProfileIndex = Math.floorMod(currentProfileIndex + direction, profiles.size());
        data.setCurrentProfileIndex(currentProfileIndex);
        saveDataQuietly();
        showCurrentProfile(direction);
    }

    private void showCurrentProfile() {
        showCurrentProfile(0);
    }

    private void showCurrentProfile(int slideDirection) {
        List<TimerProfile> profiles = data.getProfiles();
        currentProfileIndex = clampProfileIndex(currentProfileIndex);

        if (profiles.isEmpty()) {
            profileNameLabel.setText("Drawing Timer");
            profilePositionLabel.setText("No saved profiles");
            previousButton.setEnabled(false);
            nextButton.setEnabled(false);
            deleteButton.setEnabled(false);
            timerEngine.setProfile(null);
            return;
        }

        TimerProfile profile = profiles.get(currentProfileIndex);
        profileNameLabel.setText(profile.getName());
        profilePositionLabel.setText((currentProfileIndex + 1) + " of " + profiles.size());
        boolean multipleProfiles = profiles.size() > 1;
        previousButton.setEnabled(multipleProfiles);
        nextButton.setEnabled(multipleProfiles);
        deleteButton.setEnabled(true);
        timerPanel.prepareProfileTransition(slideDirection);
        timerEngine.setProfile(profile);
    }

    private void handleTimerState(TimerState state) {
        timerPanel.setTimerState(state);
        miniSessionBar.setTimerState(state);
        miniProfileNameLabel.setText(state.profile() == null ? "" : state.profile().getName());
        if (state.profile() == null) {
            setTitle("Drawing Timer");
        } else if (state.complete()) {
            setTitle(state.profile().getName() + " - Complete");
        } else {
            String status = state.running() ? "Running" : "Paused";
            setTitle(state.profile().getName() + " - " + status);
        }
    }

    private int clampProfileIndex(int candidate) {
        int size = data.getProfiles().size();
        if (size == 0) {
            return 0;
        }
        return Math.max(0, Math.min(candidate, size - 1));
    }

    private void installActions() {
        bindKey("SPACE", "toggle", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                timerEngine.toggleRunning();
            }
        });
        bindKey("R", "reset", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (!miniMode) timerEngine.reset();
            }
        });
        bindKey("LEFT", "previousProfile", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                switchProfile(-1);
            }
        });
        bindKey("RIGHT", "nextProfile", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                switchProfile(1);
            }
        });
    }

    private void bindKey(String keyStroke, String actionName, AbstractAction action) {
        JComponent root = getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(keyStroke), actionName);
        root.getActionMap().put(actionName, action);
    }

    private void installWindowListener() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                timerEngine.stop();
                saveDataQuietly();
                dispose();
            }
        });
    }

    private void saveDataWithMessage() {
        try {
            repository.save(data);
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(
                    this,
                    "The timer could not be saved to:\n" + repository.getDataFile()
                            + "\n\n" + exception.getMessage(),
                    "Save error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void saveDataQuietly() {
        try {
            repository.save(data);
        } catch (IOException exception) {
            System.err.println("Could not save timer data: " + exception.getMessage());
        }
    }
}
