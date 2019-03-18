package me.hardikrakholiya.net.impl;

import me.hardikrakholiya.Process;
import me.hardikrakholiya.net.api.MessengerService;
import me.hardikrakholiya.net.model.Instance;
import me.hardikrakholiya.net.model.Message;
import me.hardikrakholiya.net.model.MessageType;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import static me.hardikrakholiya.Configurations.getInstances;
import static me.hardikrakholiya.net.model.MessageType.*;

public class MessengerServiceImpl implements MessengerService {

    //master process, the one to which this messenger service serves
    private Process process;

    //mailbox for delivering messages to master process
    private BlockingQueue<String> mailbox = new LinkedBlockingQueue<>();

    //executor service to handle all incoming messages. single threaded for safety
    private ExecutorService messageProcessorService = Executors.newSingleThreadExecutor();

    public MessengerServiceImpl(Process process) {
        this.process = process;
    }

    @Override
    public void run() {
        //start tcp server to wait for incoming connections
        try (ServerSocket serverSocket = new ServerSocket(process.getInstance().getPort());
        ) {
            while (true) {
                Socket clientRequest = serverSocket.accept();
                Message incomingMessage = (Message) new ObjectInputStream(clientRequest.getInputStream()).readObject();
                messageProcessorService.submit(new ReceiveIncomingMessage(incomingMessage));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void send(String messageText, Instance instance) {
        Message message = createOutgoingMessage(messageText, P2P);
        sendOverNetwork(message, instance);
    }

    public void multicast(String messageText, Instance[] instances) {
        Message message = createOutgoingMessage(messageText, MC);
        multicastOverNetwork(message, instances);
    }

    public String receive() {
        try {
            return mailbox.take();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error";
        }
    }

    private int clock = 0;
    private PriorityQueue<Message> msgQ = new PriorityQueue<>();
    private final Map<String, Integer> msgAckCountMap = new HashMap<>();
    private final Set<String> acknowledgedMsgs = new HashSet<>();

    private Message createOutgoingMessage(String messageText, MessageType messageType) {
        Message message;
        clock++;
        message = new Message(clock + "." + process.getId(), messageType, messageText);
        return message;
    }

    private void sendOverNetwork(Message message, Instance instance) {
        // if the sender is same as the receiver then don't send over TCP
        if (instance.equals(this.process.getInstance())) {
            messageProcessorService.submit(new ReceiveIncomingMessage(message));
        }
        // if the sender and the receiver are different, send over TCP
        else {
            try (Socket socket = new Socket(instance.getAddress(), instance.getPort())) {
                ObjectOutputStream serverOutputStream = new ObjectOutputStream(socket.getOutputStream());
                serverOutputStream.writeObject(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void multicastOverNetwork(Message message, Instance[] instances) {
        //multicast is nothing but sending the same message to all processes
        for (Instance instance : instances) {
            sendOverNetwork(message, instance);
        }
    }

    private void processIncomingMessage(Message message) {
        clock = Math.max(clock, new Double(message.getId()).intValue());
        clock++;

        try {
            switch (message.getMessageType()) {
                case P2P:
                    msgQ.add(message);
                    break;
                case MC:
                    msgQ.add(message);
                    break;
                case ACK:
                    if (!msgAckCountMap.containsKey(message.getText())) {
                        msgAckCountMap.put(message.getText(), 0);
                    }
                    msgAckCountMap.put(message.getText(), msgAckCountMap.get(message.getText()) + 1);
                    break;

            }

            processMessageQueue();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processMessageQueue() throws InterruptedException {

        while (msgQ.size() > 0) {
            Message message = msgQ.peek();

            if (message.getMessageType() == P2P) {
                mailbox.put(message.getText());
                msgQ.remove(message);

            } else if (message.getMessageType() == MC) {
                if (!acknowledgedMsgs.contains(message.getId())) {
                    Message ack = createOutgoingMessage(message.getId(), ACK);
                    multicastOverNetwork(ack, getInstances());
                    acknowledgedMsgs.add(message.getId());
                }

                if (msgAckCountMap.containsKey(message.getId()) && msgAckCountMap.get(message.getId()) == getInstances().length) {
                    mailbox.put(message.getText());
                    msgQ.remove(message);
                    msgAckCountMap.remove(message.getId());
                    acknowledgedMsgs.remove(message.getId());
                } else {
                    break;
                }
            }
        }

    }

    private class ReceiveIncomingMessage implements Runnable {

        private Message message;

        private ReceiveIncomingMessage(Message message) {
            this.message = message;
        }

        public void run() {
            try {
                processIncomingMessage(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}