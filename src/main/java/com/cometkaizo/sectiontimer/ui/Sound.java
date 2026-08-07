package com.cometkaizo.sectiontimer.ui;

import com.cometkaizo.sectiontimer.Main;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Sound {
    protected final AudioFormat format;
    protected final byte[] audio;
    /// Creates a new sound from the given input stream
    public Sound(InputStream in) {
        try {
            var audioIn = AudioSystem.getAudioInputStream(in);
            this.format = audioIn.getFormat();
            this.audio = audioIn.readAllBytes();
        } catch (UnsupportedAudioFileException | IOException e) {
            throw new RuntimeException(e);
        }
    }
    /// Creates a new sound from the given input stream, with a given pitch shift
    public Sound(InputStream in, float deltaPitchInSemitones) {
        try {
            var origAudio = AudioSystem.getAudioInputStream(in);
            var origFormat = origAudio.getFormat();
            // modify format
            this.format = new AudioFormat(
                    origFormat.getEncoding(),
                    origFormat.getSampleRate() * pitchMultiplier(deltaPitchInSemitones),
                    origFormat.getSampleSizeInBits(),
                    origFormat.getChannels(),
                    origFormat.getFrameSize(),
                    origFormat.getFrameRate() * pitchMultiplier(deltaPitchInSemitones),
                    origFormat.isBigEndian());

            byte[] data = origAudio.readAllBytes();
            var stream = new ByteArrayInputStream(data);

            var audio = new AudioInputStream(stream, format, data.length / format.getFrameSize()); // read audio with new format
            this.audio = audio.readAllBytes();
        } catch (UnsupportedAudioFileException | IOException e) {
            throw new RuntimeException(e);
        }
    }
    /// calculates the sample rate multiplier for the given amount of semitones up or down in pitch
    private float pitchMultiplier(double semitones) {
        return (float) Math.pow(2, semitones / 12); // 2^(1/12) is the multiplier for 1 semitone up, 2^(2/12) for 2 semitones, etc.
    }

    /// Plays this sound and returns the clip
    public Clip play() {
        return play(-15);
    }
    /// Plays this sound at the given volume and returns the clip
    public Clip play(float volume) {
        try {
            var clip = AudioSystem.getClip();
            clip.open(format, audio, 0, audio.length);

            trySetVolume(clip, volume);
            clip.start();
            return clip;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /// Tries to set the volume on the clip
    private static void trySetVolume(Clip clip, float volume) {
        if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            var gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);

            // ensure the value is within the control's limits
            float minGain = gainControl.getMinimum();
            float maxGain = gainControl.getMaximum();
            float effectiveGain = Math.min(maxGain, Math.max(minGain, volume));

            // set the volume in decibels
            gainControl.setValue(effectiveGain);
        } else {
            Main.log("Volume control not supported for audio");
        }
    }
}
