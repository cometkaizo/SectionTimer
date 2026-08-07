package com.cometkaizo.sectiontimer.ui;

import com.cometkaizo.sectiontimer.model.TimerProfile;
import com.cometkaizo.sectiontimer.model.TimerSection;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class TimerEditorDialog extends JDialog {
    private static final int[] COLUMN_WIDTHS = {220, 72, 72, 96, 126, 96, 116, 92};
    private static final Color[] DEFAULT_COLORS = {
            new Color(79, 142, 247),
            new Color(242, 166, 90),
            new Color(155, 107, 214),
            new Color(80, 180, 145),
            new Color(224, 100, 120)
    };

    private final JTextField profileNameField = new JTextField(24);
    private final JPanel rowsPanel = new JPanel();
    private final List<SectionRow> rows = new ArrayList<>();
    private final Set<String> existingNames;
    private final boolean editing;

    private TimerProfile result;

    private TimerEditorDialog(Window owner, Set<String> existingNames, TimerProfile profile) {
        super(owner, profile == null ? "Create New Timer" : "Edit Timer", ModalityType.APPLICATION_MODAL);
        this.existingNames = existingNames;
        this.editing = profile != null;

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(12, 12));
        setMinimumSize(new Dimension(680, 430));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildSectionsArea(), BorderLayout.CENTER);
        add(buildButtons(), BorderLayout.SOUTH);

        if (profile == null) {
            addSectionRow("Section 1", 15, 0, DEFAULT_COLORS[0]);
        } else {
            profileNameField.setText(profile.getName());
            for (TimerSection section : profile.getSections()) {
                int duration = section.getDurationSeconds();
                addSectionRow(section.getName(), duration / 60, duration % 60,
                        ColorUtil.fromHex(section.getColorHex()), section.getLowTimeWarningPercent(),
                        section.isWaitForConfirmation());
            }
        }

        pack();
        setSize(Math.max(getWidth(), 980), Math.max(getHeight(), 520));
        setLocationRelativeTo(owner);

        if (owner instanceof JFrame frame) {
            setAlwaysOnTop(frame.isAlwaysOnTop());
        }
    }

    public static TimerProfile showDialog(Window owner, Set<String> existingNames) {
        TimerEditorDialog dialog = new TimerEditorDialog(owner, existingNames, null);
        dialog.setVisible(true);
        return dialog.result;
    }

    public static TimerProfile showEditDialog(Window owner, Set<String> existingNames,
                                               TimerProfile profile) {
        TimerEditorDialog dialog = new TimerEditorDialog(owner, existingNames, profile);
        dialog.setVisible(true);
        return dialog.result;
    }

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 0, 12));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 8);
        constraints.anchor = GridBagConstraints.WEST;

        constraints.gridx = 0;
        constraints.gridy = 0;
        panel.add(new JLabel("Timer name:"), constraints);

        constraints.gridx = 1;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(profileNameField, constraints);

        return panel;
    }

    private Component buildSectionsArea() {
        JPanel wrapper = new JPanel(new BorderLayout(8, 8));
        wrapper.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));

        JPanel columnHeadings = new JPanel(new GridBagLayout());
        GridBagConstraints c = baseRowConstraints();
        c.gridy = 0;
        addHeading(columnHeadings, "Section name", 0, 1.0, c);
        addHeading(columnHeadings, "Minutes", 1, 0, c);
        addHeading(columnHeadings, "Seconds", 2, 0, c);
        addHeading(columnHeadings, "Color", 3, 0, c);
        addHeading(columnHeadings, "Low-time warning", 4, 0, c);
        addHeading(columnHeadings, "% remaining", 5, 0, c);
        addHeading(columnHeadings, "Confirm end", 6, 0, c);
        addHeading(columnHeadings, "", 7, 0, c);
        wrapper.add(columnHeadings, BorderLayout.NORTH);

        rowsPanel.setLayout(new BoxLayout(rowsPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(rowsPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        columnHeadings.setBorder(BorderFactory.createEmptyBorder(
                0, 0, 0, scrollPane.getVerticalScrollBar().getPreferredSize().width));
        wrapper.add(scrollPane, BorderLayout.CENTER);

        JButton addSectionButton = new RoundedButton("Add section");
        addSectionButton.addActionListener(event -> {
            int index = rows.size();
            addSectionRow("Section " + (index + 1), 10, 0,
                    DEFAULT_COLORS[index % DEFAULT_COLORS.length]);
        });

        JPanel addPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        addPanel.add(addSectionButton);
        wrapper.add(addPanel, BorderLayout.SOUTH);

        return wrapper;
    }

    private JPanel buildButtons() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));

        JButton cancelButton = new RoundedButton("Cancel");
        cancelButton.addActionListener(event -> dispose());

        JButton saveButton = new RoundedButton(editing ? "Save changes" : "Create timer");
        saveButton.addActionListener(event -> saveProfile());
        getRootPane().setDefaultButton(saveButton);

        panel.add(cancelButton);
        panel.add(saveButton);
        return panel;
    }

    private void addSectionRow(String name, int minutes, int seconds, Color color) {
        addSectionRow(name, minutes, seconds, color, 0, true);
    }

    private void addSectionRow(String name, int minutes, int seconds, Color color,
                               int warningPercent, boolean waitForConfirmation) {
        SectionRow row = new SectionRow(name, minutes, seconds, color,
                warningPercent, waitForConfirmation);
        rows.add(row);
        refreshRowsPanel();
    }

    private void removeSectionRow(SectionRow row) {
        if (rows.size() == 1) {
            JOptionPane.showMessageDialog(this, "A timer must contain at least one section.");
            return;
        }
        rows.remove(row);
        refreshRowsPanel();
    }


    private void refreshRowsPanel() {
        rowsPanel.removeAll();
        for (SectionRow row : rows) {
            rowsPanel.add(row.panel);
            rowsPanel.add(Box.createVerticalStrut(6));
        }
        rowsPanel.revalidate();
        rowsPanel.repaint();
    }

    private void saveProfile() {
        String profileName = profileNameField.getText().trim();
        if (profileName.isEmpty()) {
            showValidationError("Enter a name for the timer.", profileNameField);
            return;
        }
        if (existingNames.stream().anyMatch(name -> name.equalsIgnoreCase(profileName))) {
            showValidationError("A timer with that name already exists.", profileNameField);
            return;
        }

        List<TimerSection> sections = new ArrayList<>();
        for (SectionRow row : rows) {
            String sectionName = row.nameField.getText().trim();
            int minutes = (Integer) row.minutesSpinner.getValue();
            int seconds = (Integer) row.secondsSpinner.getValue();
            int totalSeconds = minutes * 60 + seconds;

            if (sectionName.isEmpty()) {
                showValidationError("Every section needs a name.", row.nameField);
                return;
            }
            if (totalSeconds <= 0) {
                showValidationError("Every section must be longer than zero seconds.", row.minutesSpinner);
                return;
            }

            int warningPercent = row.warningEnabled.isSelected()
                    ? (Integer) row.warningPercentSpinner.getValue() : 0;
            sections.add(new TimerSection(sectionName, totalSeconds,
                    ColorUtil.toHex(row.selectedColor), warningPercent,
                    row.confirmEnd.isSelected()));
        }

        result = new TimerProfile(profileName, sections);
        dispose();
    }

    private void showValidationError(String message, Component component) {
        JOptionPane.showMessageDialog(this, message, "Cannot create timer", JOptionPane.WARNING_MESSAGE);
        component.requestFocusInWindow();
    }

    private GridBagConstraints baseRowConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.insets = new Insets(3, 4, 3, 4);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        return c;
    }

    private void addHeading(JPanel panel, String text, int x, double weight, GridBagConstraints template) {
        GridBagConstraints c = (GridBagConstraints) template.clone();
        c.gridx = x;
        c.weightx = weight;
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        setColumnWidth(label, x);
        panel.add(label, c);
    }

    private void setColumnWidth(JComponent component, int column) {
        Dimension preferred = component.getPreferredSize();
        Dimension size = new Dimension(COLUMN_WIDTHS[column], preferred.height);
        component.setPreferredSize(size);
        component.setMinimumSize(size);
    }

    private final class SectionRow {
        private final JPanel panel = new JPanel(new GridBagLayout());
        private final JTextField nameField;
        private final JSpinner minutesSpinner;
        private final JSpinner secondsSpinner;
        private final JCheckBox warningEnabled = new JCheckBox("On");
        private final JSpinner warningPercentSpinner;
        private final JCheckBox confirmEnd = new JCheckBox("Wait");
        private final JButton colorButton = new RoundedButton("");
        private Color selectedColor;

        private SectionRow(String name, int minutes, int seconds, Color color,
                           int warningPercent, boolean waitForConfirmation) {
            nameField = new JTextField(name, 20);
            minutesSpinner = new JSpinner(new SpinnerNumberModel(minutes, 0, 999, 1));
            secondsSpinner = new JSpinner(new SpinnerNumberModel(seconds, 0, 59, 1));
            warningPercentSpinner = new JSpinner(new SpinnerNumberModel(
                    warningPercent > 0 ? warningPercent : 25, 1, 100, 1));
            warningEnabled.setSelected(warningPercent > 0);
            warningPercentSpinner.setEnabled(warningEnabled.isSelected());
            warningEnabled.addActionListener(event ->
                    warningPercentSpinner.setEnabled(warningEnabled.isSelected()));
            confirmEnd.setSelected(waitForConfirmation);
            selectedColor = color;
            updateColorButton();

            colorButton.setPreferredSize(new Dimension(90, 28));
            colorButton.addActionListener(event -> chooseColor());

            JButton removeButton = new RoundedButton("Remove");
            removeButton.addActionListener(event -> removeSectionRow(this));

            setColumnWidth(nameField, 0);
            setColumnWidth(minutesSpinner, 1);
            setColumnWidth(secondsSpinner, 2);
            setColumnWidth(colorButton, 3);
            setColumnWidth(warningEnabled, 4);
            setColumnWidth(warningPercentSpinner, 5);
            setColumnWidth(confirmEnd, 6);
            setColumnWidth(removeButton, 7);

            GridBagConstraints c = baseRowConstraints();
            c.gridx = 0;
            c.weightx = 1.0;
            panel.add(nameField, c);

            c = baseRowConstraints();
            c.gridx = 1;
            c.weightx = 0;
            panel.add(minutesSpinner, c);

            c = baseRowConstraints();
            c.gridx = 2;
            panel.add(secondsSpinner, c);

            c = baseRowConstraints();
            c.gridx = 3;
            panel.add(colorButton, c);

            c = baseRowConstraints();
            c.gridx = 4;
            panel.add(warningEnabled, c);

            c = baseRowConstraints();
            c.gridx = 5;
            panel.add(warningPercentSpinner, c);

            c = baseRowConstraints();
            c.gridx = 6;
            panel.add(confirmEnd, c);

            c = baseRowConstraints();
            c.gridx = 7;
            panel.add(removeButton, c);

            panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
        }

        private void chooseColor() {
            Color newColor = JColorChooser.showDialog(
                    TimerEditorDialog.this,
                    "Choose section color",
                    selectedColor
            );
            if (newColor != null) {
                selectedColor = newColor;
                updateColorButton();
            }
        }

        private void updateColorButton() {
            colorButton.setText(ColorUtil.toHex(selectedColor));
            colorButton.setBackground(selectedColor);
            colorButton.setForeground(ColorUtil.readableTextColor(selectedColor));
            colorButton.setOpaque(true);
        }
    }
}
