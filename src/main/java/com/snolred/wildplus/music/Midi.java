package com.snolred.wildplus.music;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

/** MIDI 파일 → 시간순 음표 목록. 타악기 채널(10번)은 뺀다 */
public final class Midi {
    public record Note(long ms, int key, float volume) {}

    private record Raw(long tick, int key, int vel, long tempo) {}

    private Midi() {}

    public static List<Note> read(File f) throws Exception {
        Sequence seq = MidiSystem.getSequence(f);
        List<Raw> events = new ArrayList<>();
        for (Track t : seq.getTracks()) {
            for (int i = 0; i < t.size(); i++) {
                MidiEvent e = t.get(i);
                if (e.getMessage() instanceof MetaMessage m && m.getType() == 0x51 && m.getData().length == 3) {
                    byte[] d = m.getData();
                    events.add(new Raw(e.getTick(), -1, 0, ((d[0] & 255L) << 16) | ((d[1] & 255L) << 8) | (d[2] & 255L)));
                } else if (e.getMessage() instanceof ShortMessage s && s.getCommand() == ShortMessage.NOTE_ON && s.getData2() > 0 && s.getChannel() != 9) {
                    events.add(new Raw(e.getTick(), s.getData1(), s.getData2(), 0));
                }
            }
        }
        events.sort(Comparator.comparingLong(Raw::tick));
        List<Note> out = new ArrayList<>();
        long tempo = 500_000L, last = 0L;
        double us = 0;
        boolean ppq = seq.getDivisionType() == Sequence.PPQ;
        double perTick = ppq ? 0 : 1_000_000.0 / (seq.getDivisionType() * seq.getResolution());
        for (Raw r : events) {
            us += (r.tick() - last) * (ppq ? (double) tempo / seq.getResolution() : perTick);
            last = r.tick();
            if (r.key() < 0) tempo = r.tempo();
            else out.add(new Note((long) (us / 1000), r.key(), 0.35F + r.vel() / 127F * 0.65F));
        }
        return out;
    }
}
