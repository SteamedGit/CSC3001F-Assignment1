import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.lang.Integer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * This class defines the functionality of the client.
 */
public class Client
{

    public static void main(String[] args) throws IOException, InterruptedException {

        if (args.length != 3) {
                System.out.println("Usage: java QuoteClient <client port> <client name> <hostname>");
                return;
        }
        
        // get a datagram socket
        DatagramSocket socket = new DatagramSocket(Integer.parseInt(args[0]));

        //First time running a client we need to register with the server.
        InetAddress address = InetAddress.getByName(args[2]);
        byte[] buf = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 4445);
        registerClient(socket, args[1], packet );


        //Here we wait for confirmation that we are registered... Need to add more logic, maybe a loop.
        buf = new byte[1024];
        DatagramPacket packet2 = new DatagramPacket(buf, buf.length, address, 4445);
        packet2 = new DatagramPacket(buf, buf.length);
        socket.receive(packet2);
        String messageFromServer = new String(packet.getData(), packet.getOffset(), packet.getLength());
        String[] messageArray = messageFromServer.split("\n");
        
        if(messageArray[1].equals("Register-Client-REC")) //TODO: check that the names match
        {
             System.out.println("We are registered.");
        }

        
        //Gets the list of all other registered clients. Should have this in main loop later on...
        //This tells us who we can message.
        getOtherClients(socket, args[1], packet);

        buf = new byte[1024];
        packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);

        // display response
        String received = new String(packet.getData(), packet.getOffset(), packet.getLength());
        System.out.println(received);
        
        socket.close();
    }

    
    /**
     * The chat application protocol is not finalized. This request sends the required data to register this client.
     * The header information is embedded into the data that gets turned into buf.
     * This is loosely modelled off of HTTP. Example of this type of request:
     * 
     * ChatTP v1.0
     * Register-Client
     * 29-03-2021 00:30:45
     * Client 1
     * 
     * The address and port of this client are already contained in the UDP header.
     * 
     * @param socket
     * @param clientName
     * @param packet
     * @throws IOException
     */
    private static void registerClient(DatagramSocket socket, String clientName, DatagramPacket packet) throws IOException
    {
        String chatProtocolVersion = "ChatTP v1.0\n";
        String chatRequestType = "Register-Client\n";
        DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String chatDate = df.format(new Date()) + "\n";
        //Add hash...
        String body = clientName + "\n";

        String msg = chatProtocolVersion + chatRequestType + chatDate + body;
        byte[] buf = new byte[1024];
        buf = msg.getBytes();
        packet.setData(buf);
        
        socket.send(packet);
    }

    
    /**
     * This request asks for a list of all other registered clients from the server.
     * Example of this type of request:
     * 
     * ChatTP v1.0
     * List-Clients
     * 29-03-2021 00:30:45
     * Client 2
     * 
     * @param socket
     * @param clientName
     * @param packet
     * @throws IOException
     */
    private static void getOtherClients(DatagramSocket socket, String clientName, DatagramPacket packet) throws IOException
    {
        String chatProtocolVersion = "ChatTP v1.0\n";
        String chatRequestType = "List-Clients\n";
        DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String chatDate = df.format(new Date()) + "\n";
        //Add hash...
        String body = clientName + "\n";

        String msg = chatProtocolVersion + chatRequestType + chatDate + body;
        byte[] buf = new byte[1024];
        buf = msg.getBytes();
        packet.setData(buf);
        
        socket.send(packet);

        //TODO: Change List-Clients to Get-Clients
    }

}
