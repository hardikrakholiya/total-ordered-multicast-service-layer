## Design
This library provides a MessengerService interface and associated implementation for sending point-to-point and multicast messages among a process group. Its built on top of the transport layer and provides few methods that a user can use to send, multicast and receive text messages. Below is the definition of the interface.

```
public interface MessengerService {

    //to send point-to-point messages to another process
    public void send(String messageText, Instance instance);

    //to send a totally ordered multicast
    public void multicast(String messageText, Instance[] instances);

    //to deliver text messages to user service asynchronously
    //user will blocked after calling this method until a message is ready to be delivered
    public String receive();
}
```

## Using the MessengerService

The user can instantiate an object of MessengerService using the associated implementation. The implementation takes two parameters in its constructor: process_id and socket details. The process_id is used by MessengerService to create timestamps for messages. The socket details provided would be used the MessengerService for creating a TCP server.  

The user can simply invoke send() and multicast() methods of the interface to send point-to-point and multicast messages respectively.  

However receive() is a blocking call and the caller would be blocked until a message is received. So the user should create a separate thread to listen on the receive() call.

## Implementation details

Once instantiated, the MessengerService creates a TCP server socket using the provided socket details and would start listening on the port for incoming messages. Any time a message is received, a new thread is created to handle the message before going back to listening again. The new thread would then call process_incoming_message() method to process the message.

Below are the data structures and methods of the MessengerService implementation
```
class MessengerService:
    id # process_id
    socket_details # for creating tcp server
    clock # to store logical clock value
    msgQ # a priority queue to store incoming messages with message with the least message_id at its head
    msg_ack_map # to store counts of acks associated with a given message
    blocking_queue # for delivering messages asynchronously to the user in FIFO order
```
send() and multicast() are synchronous calls which create a message with P2P or MC type and simple sends it using the network layer. clock is incremented every time a message is created.

```
def send(message_text)
    message = create_message(type=P2P, message_text)
    create client_socket and send message over TCP

def multicast(message_text):
    message = create_message(type=MC, message_text)
    for every process:
        create client_socket and send message over TCP

def create_message(message_type, message_text):
    increment clock
    return message = Message(clock.process_id, message_type, message_text)
```

This blocking of caller process is achieved through the use of BlockingQueue data structure. The caller(consumer) is blocked on the queue until MessengerService(producer) puts the message to the queue. If message is enqueued, the called is unblocked and is given the message.
```
def receive:
    blocking_queue.dequeue()
```
process_incoming_message() is the crux of the total-ordered-multicast. It delivers message to the user in ordering of message's clock values. At any point of time, only the message at the head is processed. All other messages will wait until the message in the head is processed. If the message is P2P type it is simply dequeued and delivered to the user. If it is MC type, it will be delivered only after all ACKs for the messages are received.

```
def process_incoming_message(message):
    adjust clock using the message's clock value

    if message.type is P2P or MC:
        enqueue message in the msgQ
    else if message.type is ACK:
        increment ACK counter for the associated message

    # process msgQ
    while:
        message = msgQ.peek()
        if message.type is P2P:
            deliver to the user
            remove the message from msgQ
        else if message.type is MC:
            if ack not sent for the message:
                send ack with create_message(type=ACK, message.id)
            if all acks not received for the message:
                break loop and wait till all acks are received
            else when all acks are received:
                deliver to the user
                remove the message from msgQ
```
#### Format of messages
```
public Message {

    //message id is a pair of sender's clock counter and process id
    String id;

    //message type can be any of P2P(point-to-point), MC(multicast) or ACK(acknowledgement)
    MessageType messageType;

    //message text. In case of ACKs this will be the associated message id
    String text;
}
```

## Assumptions
- FIFO delivery of messages - messages are received in the order they are sent
- No process failures
- Any process is aware of all other process in the group. It also has the associated socket details to send messages
- The messages delivered to the framework have a pre-defined format supported by the framework

## Running the framework
You should have Java 8 and Apache Maven installed on your system.  
First execute `mvn clean install` to build an executable jar from the code.  
Once the executables are build, you can run the framework multiple times by executing `sh run.sh`

## Testing
I have bundled a test code for automatically sending messages once the framework is run. This was necessary to make sure all processes send messages simultaneously to really see the effect of total ordering of messages. User input would make the final order very predictive.

Below is the summary of the example test case that is automatically executed on running the framework:
- 13 processes are created
- Each process multicasts one of the first 13 character of the English alphabet
- Then it sends 4 point-to-point messages containing its own process ID to neighbor processes in the process ring
- it then again multicasts one of the last 13 characters of the English alphabet
- Finally it waits a few seconds for all the messages to arrive and prints its message inbox
- in summary, characters are multicasted and numbers are point-to-point messages


Below is the one such output. Output might be different every time the script is run but *the order of the characters(multicasts) will always be the same for all processes.*
```
Inbox of process 8: [a, b, c, d, e, f, g, h, i, j, k, l, m, 6, 7, 9, 10, w, u, t, s, r, q, p, o, n, y, x, v, z]
Inbox of process 0: [a, b, c, d, e, f, g, h, i, j, k, l, m, 11, 12, 1, w, u, t, s, r, q, p, o, n, 2, y, x, v, z]
Inbox of process 9: [a, b, c, d, e, f, g, h, i, j, k, l, m, 7, 8, 10, 11, w, u, t, s, r, q, p, o, n, y, x, v, z]
Inbox of process 5: [a, b, c, d, e, f, g, h, i, j, k, l, m, 3, 4, 6, 7, w, u, t, s, r, q, p, o, n, y, x, v, z]
Inbox of process 6: [a, b, c, d, e, f, g, h, i, j, k, l, m, 4, 5, 7, 8, w, u, t, s, r, q, p, o, n, y, x, v, z]
Inbox of process 2: [a, b, c, d, e, f, g, h, i, j, k, l, m, 1, 0, 3, w, u, t, s, r, q, p, o, n, y, 4, x, v, z]
Inbox of process 4: [a, b, c, d, e, f, g, h, i, j, k, l, m, 3, 5, 2, 6, w, u, t, s, r, q, p, o, n, y, x, v, z]
Inbox of process 11: [a, b, c, d, e, f, g, h, i, j, k, l, m, 9, 10, 12, w, u, t, s, r, q, p, o, n, y, x, v, 0, z]
Inbox of process 7: [a, b, c, d, e, f, g, h, i, j, k, l, m, 5, 6, 8, 9, w, u, t, s, r, q, p, o, n, y, x, v, z]
Inbox of process 10: [a, b, c, d, e, f, g, h, i, j, k, l, m, 8, 9, 11, 12, w, u, t, s, r, q, p, o, n, y, x, v, z]
Inbox of process 1: [a, b, c, d, e, f, g, h, i, j, k, l, m, 12, 3, w, u, t, s, r, q, p, o, n, 2, y, x, v, 0, z]
Inbox of process 3: [a, b, c, d, e, f, g, h, i, j, k, l, m, 1, 4, 5, 2, w, u, t, s, r, q, p, o, n, y, x, v, z]
Inbox of process 12: [a, b, c, d, e, f, g, h, i, j, k, l, m, 10, 11, w, u, t, s, r, q, p, o, n, 1, y, x, v, 0, z]
```
