package com.gianlu.internethacker.models;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Message {
    public final Header header;
    public final List<Question> questions;
    public final List<ResourceRecord> answers;
    public final List<ResourceRecord> authorities;
    public final List<ResourceRecord> additional;

    public Message(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);

        header = new Header(buffer);

        questions = new ArrayList<>(header.qdcount);
        for (int i = 0; i < header.qdcount; i++)
            questions.add(new Question(buffer));

        answers = new ArrayList<>(header.ancount);
        for (int i = 0; i < header.ancount; i++)
            answers.add(new ResourceRecord(buffer));

        authorities = new ArrayList<>(header.nscount);
        for (int i = 0; i < header.nscount; i++)
            authorities.add(new ResourceRecord(buffer));

        additional = new ArrayList<>(header.arcount);
        for (int i = 0; i < header.arcount; i++)
            additional.add(new ResourceRecord(buffer));
    }

    /**
     * Reads a set of labels from a DNS message
     *
     * @param data   a {@link ByteBuffer} to read the data from, position is not important.
     * @param offset where to start reading
     * @return the desired domain-name split into labels
     */
    @NotNull
    private static List<String> readLabels(ByteBuffer data, int offset) {
        List<String> labels = new ArrayList<>();

        int pos = offset;
        byte length;
        while ((length = data.get(pos++)) != 0) {
            if (((length >> 6) & 0b00000011) == 0b00000011) {
                int loc = data.get(pos++) | ((length & 0b00111111) << 8);
                labels.addAll(readLabels(data, loc));
                break;
            } else {
                byte[] buffer = new byte[length];
                for (int i = 0; i < length; i++)
                    buffer[i] = data.get(pos++);
                labels.add(new String(buffer));
            }
        }

        data.position(pos);

        return labels;
    }

    static List<String> readLabels(ByteBuffer data) {
        return readLabels(data, data.position());
    }

    static void writeLabels(OutputStream out, List<String> labels) throws IOException {
        for (String label : labels) {
            out.write(label.length());
            out.write(label.getBytes());
        }

        out.write(0);
    }

    public void write(OutputStream out) throws IOException {
        header.write(out);

        for (Question question : questions)
            question.write(out);

        for (ResourceRecord rr : answers)
            rr.write(out);

        for (ResourceRecord rr : authorities)
            rr.write(out);

        for (ResourceRecord rr : additional)
            rr.write(out);
    }
}
