package me.hardikrakholiya;

import com.google.common.base.Preconditions;
import me.hardikrakholiya.net.model.Instance;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

public class Configurations {

    private static Instance[] instances;

    public static void loadConfigurations(String configPath) {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(configPath));
        } catch (IOException e) {
            System.out.println("Properties file at path: '" + configPath + "' not found");
            System.exit(-1);
        }

        //read instances from configurations
        String commaSeparatedInstances = properties.getProperty("instances");
        Preconditions.checkArgument(commaSeparatedInstances != null && !commaSeparatedInstances.isEmpty(), "Provide comma separated list of instances");

        instances = Arrays.stream(commaSeparatedInstances.split("\\s*,\\s*"))
                .map(instance -> {
                    String[] addressAndPort = instance.split(":");
                    return new Instance(addressAndPort[0], Integer.parseInt(addressAndPort[1]));
                }).toArray(Instance[]::new);
    }

    public static Instance[] getInstances() {
        return instances;
    }

}
