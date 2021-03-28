import java.net.*;
/**
 * Class stores data about clients.
 */
public class ClientData
{
    private InetAddress address;
    private int port;
    private String name;

    //Maybe all their sent messages?

    public ClientData(InetAddress address, int port, String name)
    {
        this.address = address;
        this.port = port;
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

    public int getPort()
    {
        return this.port;
    }
}