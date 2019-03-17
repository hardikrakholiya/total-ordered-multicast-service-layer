package me.hardikrakholiya;


import java.util.ArrayList;
import java.util.List;

import static me.hardikrakholiya.Configurations.getInstances;
import static me.hardikrakholiya.Configurations.loadConfigurations;

public class Application {

    public static List<Thread> threads = new ArrayList<>();

    public static void main(String[] args) throws InterruptedException {
        if (args.length < 1) {
            System.out.println("Please provide path to config.properties as an argument");
        }

        String configPath = args[0];
        loadConfigurations(configPath);

        for (int i = 0; i < getInstances().length; i++) {
            Thread thread = new Thread(new Process(i, getInstances()[i]), "process_" + i);
            threads.add(thread);
            thread.start();
        }


        for (Thread thread : threads) {
            thread.join();
        }

    }
}
