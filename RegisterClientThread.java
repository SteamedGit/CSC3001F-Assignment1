import java.io.*;
import java.net.*;
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.concurrent.atomic.AtomicBoolean;


//This thread is entirely responsible for registering the client
public class RegisterClientThread extends Thread
{
    protected DatagramSocket socket;
    protected AtomicBoolean registerFlag; //we use this flag to indicate when we have successfully registered
    protected AtomicBoolean isSocketOpen; //this flag is set by Client's main to indicate when the socket is open
    protected String clientName;
    protected int sendingPort;
    protected int serverPort;
    protected InetAddress address;
    
    public RegisterClientThread(DatagramSocket socket, AtomicBoolean isRegistered, AtomicBoolean isSocketOpen, 
    String clientName, int sendingPort, InetAddress address, int serverPort) throws IOException
    {
        this.socket = socket;
        this.registerFlag = isRegistered;
        this.isSocketOpen = isSocketOpen;
        this.clientName = clientName;
        this.sendingPort = sendingPort;
        this.address = address;
        this.serverPort = serverPort;
    }

    public void run()
    {
        try
        {
            byte[] buf = new byte[1024];
            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, serverPort);
            Message.registerClient(socket, sendingPort, clientName, packet);
                
            //Here we await a response
            buf = new byte[1024];
            packet = new DatagramPacket(buf, buf.length);
            socket.receive(packet);
            
            if(isSocketOpen.get())
            {
                String messageFromServer = new String(packet.getData(), packet.getOffset(), packet.getLength());
                String[] messageArray = messageFromServer.split("\n");
                
                if(messageArray[1].equals("Register-Client-OK") && messageArray[3].equals(clientName))
                {
                     System.out.println("We are registered.");
                     registerFlag.set(true); //we now break out of the registration loop in main
    
                }
                else if(messageArray[1].equals("Register-Client-BAD"))
                {
                    System.out.println("Please Re-register: ");
                    System.out.println(messageArray[3]);
                    System.exit(0); //if the registration was bad we need to restart the program
                }
            }
           
        }
        catch(IOException e)
        {
           //We are getting an error everytime main closes the socket, even with the flag, 
           //the error seems harmless so we are ignoring for now..
            
        }
    }

}