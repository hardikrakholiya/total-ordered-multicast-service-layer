package me.hardikrakholiya.net.api;

import me.hardikrakholiya.net.model.Instance;

public interface MessengerService {

    public void send(String messageText, Instance instance);

    public void multicast(String messageText, Instance[] instances);

    public String receive();
}
