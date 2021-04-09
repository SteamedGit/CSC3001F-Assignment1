import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

//This class will continuously listen for messages from the server, indepedently of what we are doing in the Client class
public class ReceiverClientThread extends Thread
{
    protected AtomicInteger numberOfReceivedMessages; //keeps track of how many messages we have received
    protected BlockingQueue<String[]> messagesFromServer; //We store all messages in a queue
    protected int listeningPort;
    protected DatagramSocket listenSocket;

    public ReceiverClientThread(int port) throws IOException
    {
        this.listeningPort = port;
        this.listenSocket = new DatagramSocket(port);
        this.messagesFromServer = new LinkedBlockingQueue<String[]>();
        numberOfReceivedMessages = new AtomicInteger(0);
    }

    public void run()
    {
        try
        {
            while(true) //receives packets until the program ends
            {
                byte[] buf = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                listenSocket.receive(packet);
                String messageFromServer = new String(packet.getData(), packet.getOffset(), packet.getLength());
                String[] messageArray = messageFromServer.split("\n");
                messagesFromServer.add(messageArray);
                numberOfReceivedMessages.incrementAndGet();
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        
        listenSocket.close();
    }
}