package me.hardikrakholiya.net.model;

import java.io.Serializable;

public class Message implements Serializable, Comparable<Message> {
    private final String id;
    private final MessageType messageType;
    private final String text;

    public Message(String id, MessageType messageType, String text) {
        this.id = id;
        this.messageType = messageType;
        this.text = text;
    }

    public String getId() {
        return id;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return "Message{" +
                "id='" + id + '\'' +
                ", messageType=" + messageType +
                ", text='" + text + '\'' +
                '}';
    }

    @Override
    public int compareTo(Message other) {
//        Integer[] clockProcessIdPair = (Integer[]) Arrays.stream(id.split(".")).map(Integer::parseInt).toArray();
//        Integer[] otherClockProcessIdPair = (Integer[]) Arrays.stream(other.getId().split(".")).map(Integer::parseInt).toArray();
//        int clock = clockProcessIdPair[0];
//        int processId = clockProcessIdPair[1];
//        int otherClock = otherClockProcessIdPair[0];
//        int otherProcessId = otherClockProcessIdPair[1];
//
//        if (clock != otherClock) {
//            return Integer.compare(clock, otherClock);
//        }
//
//        return Integer.compare(processId, otherProcessId);

        return Double.compare(new Double(this.id), new Double(other.id));
    }
}
