package me.hardikrakholiya;

import me.hardikrakholiya.net.api.MessengerService;
import me.hardikrakholiya.net.impl.MessengerServiceImpl;
import me.hardikrakholiya.net.model.Instance;

import java.util.ArrayList;
import java.util.List;

import static me.hardikrakholiya.Configurations.getInstances;

public class Process implements Runnable {
    private final int id;
    private Instance instance;
    private List<String> messages = new ArrayList<>();
    private MessengerService messengerService = new MessengerServiceImpl(this);

    public Process(int id, Instance instance) {
        this.id = id;
        this.instance = instance;
    }

    @Override
    public void run() {

        try {
            //start a middleware process to handle all the coordination with other processes
            new Thread(messengerService).start();

            //start a thread to receive incoming message from the middleware
            new Thread(new ReceiveMessage()).start();

            //wait for the messengerService server to setup itself
            Thread.sleep(1000);

            //send multicast and P2P messages from a schedule
            new MessageGenerator().send();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public int getId() {
        return id;
    }

    public Instance getInstance() {
        return instance;
    }

    private class ReceiveMessage implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    messages.add(messengerService.receive());
                    System.out.println("Inbox of process " + id + ": " + messages);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class MessageGenerator {

        private void send() {

            String alphabet = "abcdefghijklmnopqrstuvwxyz";

            //multicast
            messengerService.multicast(String.valueOf(alphabet.charAt(id)), getInstances());

            //P2P messages to 4 neighbor processes in a ring
            messengerService.send("" + id, getInstances()[(id + 2) % getInstances().length]);
            messengerService.send("" + id, getInstances()[(id + 1) % getInstances().length]);
            messengerService.send("" + id, getInstances()[(id + getInstances().length - 1) % getInstances().length]);
            messengerService.send("" + id, getInstances()[(id + getInstances().length - 2) % getInstances().length]);

            //again multicast
            messengerService.multicast(String.valueOf(alphabet.charAt(25 - id)), getInstances());
        }
    }
}

