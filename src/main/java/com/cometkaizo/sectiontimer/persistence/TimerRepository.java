package com.cometkaizo.sectiontimer.persistence;

import com.cometkaizo.sectiontimer.model.TimerData;
import com.cometkaizo.sectiontimer.model.TimerProfile;
import com.cometkaizo.sectiontimer.model.TimerSection;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TimerRepository {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path dataFile;

    public TimerRepository() {
        this(defaultDataFile());
    }

    public TimerRepository(Path dataFile) {
        this.dataFile = dataFile;
    }

    public Path getDataFile() {
        return dataFile;
    }

    private static Path defaultDataFile() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String userHome = System.getProperty("user.home");

        if (osName.contains("win")) {
            String appData = System.getenv("APPDATA");
            Path roamingDirectory = appData == null || appData.isBlank()
                    ? Path.of(userHome, "AppData", "Roaming")
                    : Path.of(appData);
            return roamingDirectory.resolve("SectionTimer").resolve("timers.json");
        }

        if (osName.contains("mac")) {
            return Path.of(userHome, "Library", "Application Support",
                    "SectionTimer", "timers.json");
        }

        String configHome = System.getenv("XDG_CONFIG_HOME");
        Path configDirectory = configHome == null || configHome.isBlank()
                ? Path.of(userHome, ".config")
                : Path.of(configHome);
        return configDirectory.resolve("SectionTimer").resolve("timers.json");
    }

    public TimerData load() {
        if (!Files.exists(dataFile)) {
            TimerData defaults = createDefaultData();
            try {
                save(defaults);
            } catch (IOException exception) {
                System.err.println("Could not create the initial timer file: " + exception.getMessage());
            }
            return defaults;
        }

        try {
            String json = Files.readString(dataFile, StandardCharsets.UTF_8);
            TimerData data = gson.fromJson(json, TimerData.class);
            return sanitize(data);
        } catch (IOException | JsonParseException exception) {
            backUpBrokenFile();
            System.err.println("Could not read timer data; defaults will be used: " + exception.getMessage());
            return createDefaultData();
        }
    }

    public void save(TimerData data) throws IOException {
        TimerData safeData = sanitize(data);
        Files.createDirectories(dataFile.getParent());

        Path temporaryFile = dataFile.resolveSibling(dataFile.getFileName() + ".tmp");
        String json = gson.toJson(safeData);
        Files.writeString(
                temporaryFile,
                json,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );

        try {
            Files.move(
                    temporaryFile,
                    dataFile,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporaryFile, dataFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private TimerData sanitize(TimerData data) {
        if (data == null) {
            return createDefaultData();
        }

        List<TimerProfile> validProfiles = new ArrayList<>();
        for (TimerProfile profile : data.getProfiles()) {
            if (profile == null || profile.getName() == null || profile.getName().isBlank()) {
                continue;
            }

            List<TimerSection> validSections = new ArrayList<>();
            for (TimerSection section : profile.getSections()) {
                if (section == null || section.getName() == null || section.getName().isBlank()) {
                    continue;
                }
                if (section.getDurationSeconds() <= 0) {
                    continue;
                }
                if (!isValidHexColor(section.getColorHex())) {
                    section.setColorHex("#4F8EF7");
                }
                section.setLowTimeWarningPercent(section.getLowTimeWarningPercent());
                validSections.add(section);
            }

            if (!validSections.isEmpty()) {
                profile.setSections(validSections);
                validProfiles.add(profile);
            }
        }

        data.setProfiles(validProfiles);
        int maximumIndex = validProfiles.size() - 1;
        if (maximumIndex < 0) {
            data.setCurrentProfileIndex(0);
        } else {
            data.setCurrentProfileIndex(Math.max(0, Math.min(data.getCurrentProfileIndex(), maximumIndex)));
        }
        return data;
    }

    private boolean isValidHexColor(String value) {
        return value != null && value.matches("#[0-9a-fA-F]{6}");
    }

    private void backUpBrokenFile() {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            Path backup = dataFile.resolveSibling("timers-broken-" + timestamp + ".json");
            Files.move(dataFile, backup, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            // The original read error is more useful than a backup error.
        }
    }

    private TimerData createDefaultData() {
        TimerProfile oneHour = new TimerProfile("1 Hour Drawing", List.of(
                new TimerSection("Sketching", 15 * 60, "#4F8EF7"),
                new TimerSection("Color Blocking", 15 * 60, "#F2A65A"),
                new TimerSection("Rendering", 30 * 60, "#9B6BD6")
        ));

        TimerProfile twoHours = new TimerProfile("2 Hour Drawing", List.of(
                new TimerSection("Sketching", 25 * 60, "#4F8EF7"),
                new TimerSection("Color Blocking", 30 * 60, "#F2A65A"),
                new TimerSection("Rendering", 65 * 60, "#9B6BD6")
        ));

        return new TimerData(List.of(oneHour, twoHours), 0);
    }
}
