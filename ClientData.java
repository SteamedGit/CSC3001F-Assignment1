import java.net.*;
import java.util.*;
/**
 * Class stores data about clients.
 */
public class ClientData
{
    private InetAddress address;
    private int receivingPort;
    private int sendingPort;
    private String name;

    private int numberOfMessagesSentToClient;
    private int numberOfMessagesReceivedFromClient;

    private Map<Long, Message> messagesSentToServer;
    private Map<Long, Message> messagesSentFromServer;

    public ClientData(InetAddress address, int receivingPort, int sendingPort, String name)
    {
        this.address = address;
        this.receivingPort = receivingPort;
        this.sendingPort = sendingPort;
        this.name = name;
        this.numberOfMessagesSentToClient = 0;
        this.numberOfMessagesReceivedFromClient = 0;

        this.messagesSentToServer = new HashMap<Long, Message>();
        this.messagesSentFromServer = new HashMap<Long, Message>();
    }

    public InetAddress getAddress()
    {
        return this.address;
    }

    public String getName()
    {
        return this.name;
    }

    public int getReceivingPort()
    {
        return this.receivingPort;
    }

    public int getSendingPort()
    {
        return this.sendingPort;
    }

    public int getReceivedNum()
    {
        return numberOfMessagesReceivedFromClient;
    }

    public int getSentNum()
    {
        return numberOfMessagesSentToClient;
    }

    public void incrementReceived()
    {
        this.numberOfMessagesReceivedFromClient += 1;
    }

    public void incrementSent()
    {
        this.numberOfMessagesSentToClient += 1;
    }

    public void decrementReceived()
    {
        this.numberOfMessagesReceivedFromClient -= 1;
    }

    public void decrementSent()
    {
        this.numberOfMessagesSentToClient -= 1;
    }

    public void addMessageSentToServer(Long checksum, Message sentMessage)
    {
        this.messagesSentToServer.put(checksum, sentMessage);
    }

    public void addMessageSentFromServer(Long checksum, Message sentMessage)
    {
        this.messagesSentFromServer.put(checksum, sentMessage);
    }

    public Message searchMessagesSentToServer(Long checksum)
    {
       return this.messagesSentToServer.get(checksum);
    }

    public Message searchMessagesSentFromServer(Long checksum)
    {
       return this.messagesSentFromServer.get(checksum);
    }

    public ArrayList<Message> getMessagesSentToServer()
    {
        return new ArrayList<Message>(messagesSentToServer.values());
    }

    public ArrayList<Long> getMessagesSentToServerCheckSums()
    {
        return new ArrayList<Long>(messagesSentToServer.keySet());
    }
}