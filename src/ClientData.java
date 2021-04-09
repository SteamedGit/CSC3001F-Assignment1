import java.net.*;
/**
 * Class stores data about clients.
 */
public class ClientData
{
    private InetAddress address;
    private int receivingPort;
    private int sendingPort;
    private String name;

    //Maybe all their sent messages?

    public ClientData(InetAddress address, int receivingPort, int sendingPort, String name)
    {
        this.address = address;
        this.receivingPort = receivingPort;
        this.sendingPort = sendingPort;
        this.name = name;
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
}