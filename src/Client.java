import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.lang.Integer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.*;


/**
 * This class defines the functionality of the client.
 */
public class Client
{
    
    public static void main(String[] args) throws IOException, InterruptedException {

        AtomicBoolean isRegistered = new AtomicBoolean(false);
        AtomicBoolean isRegisterSocketOpen = new AtomicBoolean(false);
        String otherClients = new String();

        if (args.length != 3) {
                System.out.println("Usage: java Client <client port> <client name> <hostname>");
                return;
        }
        
        //We cannot proceed until we are registered. If we successfully register than the isRegistered flag will be set to
        //true by the current RegisterClientThread
        while(!isRegistered.get()) 
        {
            DatagramSocket socket = new DatagramSocket(Integer.parseInt(args[0]));
            isRegisterSocketOpen.set(true);
            InetAddress address = InetAddress.getByName(args[2]);
            System.out.println("Starting new registration thread.");
            new RegisterClientThread(socket, isRegistered, isRegisterSocketOpen,args[1], address, 4445).start();
            TimeUnit.SECONDS.sleep(1);
            socket.close();
            isRegisterSocketOpen.set(false);
        }

        InetAddress address = InetAddress.getByName(args[2]);
        DatagramSocket socket = new DatagramSocket(Integer.parseInt(args[0]));
        byte[] buf = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 4445);
        String messageFromServer;
        String[] messageArray;
    
        
        //Gets the list of all other registered clients.
        //This tells us who we can message.
        getOtherClients(socket, args[1], packet);

        buf = new byte[1024];
        packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);

        String received = new String(packet.getData(), packet.getOffset(), packet.getLength());
        otherClients = received.split("\n")[3];
        System.out.println("Available Clients: " + otherClients);


        //Send and Receive Messages
        Scanner input = new Scanner(System.in);
        while(true)
        {
            System.out.println("Do you want to send a message(S) or receive a message(R) or quit?(Q)");
            String sRQuit = input.nextLine();
            System.out.println("");
            if(sRQuit.toLowerCase().equals("s"))
            {
                
                System.out.println("Who do you want to send a message to?");
                String recipient = input.nextLine();
                System.out.println("What is the message?");
                String text = input.nextLine();
                System.out.println("");
                sendMessage(socket, recipient, text, packet);
                continue;
            }
            if(sRQuit.toLowerCase().equals("quit") || sRQuit.toLowerCase().equals("q") )
            {
                break;
            }
            buf = new byte[1024];
            packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);
            messageFromServer = new String(packet.getData(), packet.getOffset(), packet.getLength());
            messageArray = messageFromServer.split("\n");
            System.out.println("Message from " + messageArray[3] + ":");
            System.out.println(messageArray[4]+"\n");
        }
      
      

        input.close();
        socket.close();
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
        String chatRequestType = "Get-Clients\n";
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
     * Sends message for another client to the server. The server will pass the message on to the intended recipient.
     * Example:
     * 
     * ChatTP v1.0
     * Send-MSG-C
     * 29-03-2021 00:30:45
     * Client 2
     * Hello Client 2!
     * 
     * 
     * 
     * @param socket
     * @param recipient
     * @param text
     * @param packet
     * @throws IOException
     */
    private static void sendMessage(DatagramSocket socket, String recipient, String text, DatagramPacket packet) throws IOException
    {
        String chatProtocolVersion = "ChatTP v1.0\n";
        String chatRequestType = "Send-MSG-C\n";
        DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String chatDate = df.format(new Date()) + "\n";
        //Add hash...
        String recip = recipient + "\n";
        String body = text + "\n";

        String msg = chatProtocolVersion + chatRequestType + chatDate + recip + body;
        byte[] buf = new byte[1024];
        buf = msg.getBytes();
        packet.setData(buf);
        
        socket.send(packet);
    }

}
