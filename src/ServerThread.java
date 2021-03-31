import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.Integer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;
 
/**
 * Defines the server thread.
 */
public class ServerThread extends Thread {
 
    protected DatagramSocket socket = null;
    protected BufferedReader in = null;
    protected List<ClientData> clientData = null;
    protected boolean moreQuotes = true;
 
    public ServerThread() throws IOException {
    this("ServerThread");
    }
 
    public ServerThread(String name) throws IOException{
        super(name);
        socket = new DatagramSocket(4445);
 
    }
 
    public void run() {
        clientData = new ArrayList<ClientData>();
        while (true) {
            try {
                byte[] buf = new byte[1024];
 
                // receive request
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
 
                String messageFromClient = new String(packet.getData(), packet.getOffset(), packet.getLength());
                System.out.println(messageFromClient);
                String[] messageArray = messageFromClient.split("\n");

                if (messageArray[1].equals("Register-Client"))
                {
                    
                    List<ClientData> result1 = clientData.stream() //check if another client with the same name exists
                    .filter(a -> a.getName().equals(messageArray[3]))
                    .collect(Collectors.toList());

                    List<ClientData> result2 = clientData.stream() //check if another client with the same inetaddress and port exists
                    .filter(a -> a.getPort()==packet.getPort())
                    .filter(a-> a.getAddress().equals(packet.getAddress()))
                    .collect(Collectors.toList());
                    if(result1.size() == 0 && result2.size() == 0)
                    {
                        clientData.add(new ClientData(packet.getAddress(), packet.getPort(), messageArray[3]));
                        confirmRegister(socket, packet.getAddress(), packet.getPort(), messageArray[3], packet);
                        System.out.println(">" + messageArray[3]+" is registered.\n");
                    }
                    if(result1.size() >0)
                    {
                        if(result1.get(0).getAddress().equals(packet.getAddress()) && result1.get(0).getPort() == packet.getPort())
                        {
                            System.out.println("Already registered as " + result1.get(0).getName());
                            confirmRegister(socket, packet.getAddress(), packet.getPort(), messageArray[3], packet);
                        }
                        else
                        {
                            String error = "Someone with this name is already registered. Register with a new name.";
                            System.out.println(error);
                            badRegister(socket, packet.getAddress(), packet.getPort(), error, packet);
                        }
                        
                    }
                    else if(result2.size() > 0)
                    {
                        
                        if(!result2.get(0).getName().equals(messageArray[3]))
                        {
                            String error = "Someone else is registered at " + result2.get(0).getAddress() + " " + result2.get(0).getPort() + " as " + result2.get(0).getName();
                            System.out.println(error);
                            badRegister(socket, packet.getAddress(), packet.getPort(), error, packet);
                        }
                        
                    }
                    
                    
                }

                else if(messageArray[1].equals("Get-Clients"))
                {
                    sendListOfClients(socket, packet.getAddress(), packet.getPort(), messageArray[3], packet, clientData);
                }

                else if(messageArray[1].equals("Send-MSG-C")) //TODO: Add confirmation to sender that server received message and get confirmation from recipient of receival
                {
                    List<ClientData> result1 = clientData.stream() 
                    .filter(a -> a.getName().equals(messageArray[3]))
                    .collect(Collectors.toList());

                    List<ClientData> result2 = clientData.stream() 
                    .filter(a -> a.getPort()==packet.getPort())
                    .filter(a-> a.getAddress().equals(packet.getAddress()))
                    .collect(Collectors.toList());

                    System.out.println("Message for " + messageArray[3] + ": " + messageArray[4]);
                    if (result1.size()>0 && result2.size()>0)
                    {
                        System.out.println(result1.get(0).getName());
                        message(socket, result1.get(0).getAddress(), result1.get(0).getPort(), result2.get(0).getName() , messageArray[4], packet);
                    }
                    
                }
               
            } 
            catch (IOException e) 
            {
                e.printStackTrace();
            }
        }
        //socket.close();
    }

   /**
    * Sends a respond back to the client confirming that it is registered.
    * Example:
    * 
    * ChatTP v1.0
    * Register-Client-OK
    * 29-03-2021 00:30:45
    * Client 1
    *
    * @param socket
    * @param address
    * @param port
    * @param clientName
    * @param packet
    * @throws IOException
    */
    protected void confirmRegister(DatagramSocket socket, InetAddress address, int port, String clientName, DatagramPacket packet) throws IOException
    {
        String chatProtocolVersion = "ChatTP v1.0\n";
        String chatRequestType = "Register-Client-OK\n";
        DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String chatDate = df.format(new Date()) + "\n";
        //Add hash...
        String body = clientName + "\n";
        String msg = chatProtocolVersion + chatRequestType + chatDate + body;
        System.out.println(msg);
        byte[] buf = new byte[1024];
        buf = msg.getBytes();
        packet.setAddress(address);
        packet.setPort(port);
        packet.setData(buf);
        socket.send(packet);
    }

   /**
    * Sends a response back to the client indicating that registration was unsuccessful.
    * Example:
    *
    * ChatTP v1.0
    * Register-Client-BAD
    * 29-03-2021 00:30:45
    * Someone with this name is already registered. Register with a new name.
    *
    * @param socket
    * @param address
    * @param port
    * @param error
    * @param packet
    * @throws IOException
    */
    protected void badRegister (DatagramSocket socket, InetAddress address, int port, String error, DatagramPacket packet) throws IOException
    {
        String chatProtocolVersion = "ChatTP v1.0\n";
        String chatRequestType = "Register-Client-BAD\n";
        DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String chatDate = df.format(new Date()) + "\n";
        //Add hash...
        String body = error + "\n";
        String msg = chatProtocolVersion + chatRequestType + chatDate + body;
        byte[] buf = new byte[1024];
        buf = msg.getBytes();
        packet.setAddress(address);
        packet.setPort(port);
        packet.setData(buf);
        socket.send(packet);
    }

    
    /**
     * Sends a request back to the client with a list of all other registered clients.
     * Example:
     * 
     * ChatTP v1.0
     * List-Of-Clients
     * 29-03-2021 00:30:45
     * Client 1, Client 2, Client 3, 
     * 
     * @param socket
     * @param address
     * @param port
     * @param clientName
     * @param packet
     * @param clientData
     * @throws IOException
     */
    protected void sendListOfClients(DatagramSocket socket, InetAddress address, int port, String clientName, DatagramPacket packet, List<ClientData> clientData) throws IOException
    {
        String chatProtocolVersion = "ChatTP v1.0\n";
        String chatRequestType = "List-Of-Clients\n";
        DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String chatDate = df.format(new Date()) + "\n";
        //Add hash...
        String body = "";
        for(ClientData data : clientData)
        {
            //System.out.println("Name is: " + data.getName());
            if(!(data.getName().equals(clientName)))
            {
                String tmp = data.getName() + ", ";
                body += tmp;
            }
            
        }
        if(body.isEmpty())
        {
            body = "No other clients";
        }
        body += "\n";
        String msg = chatProtocolVersion + chatRequestType + chatDate + body;
        byte[] buf = new byte[1024];
        buf = msg.getBytes();
        packet.setAddress(address);
        packet.setPort(port);
        packet.setData(buf);
        socket.send(packet);
        
    }

    /**
     * Forwards a client's message to the intended recipient.
     * 
     * Example:
     * 
     * ChatTP v1.0
     * Send-MSG-S
     * 29-03-2021 00:30:45
     * Client 1
     * Hello Client 2!
     * 
     * 
     * @param socket
     * @param address
     * @param port
     * @param sender
     * @param text
     * @param packet
     * @throws IOException
     */
    protected void message(DatagramSocket socket, InetAddress address, int port, String sender, String text, DatagramPacket packet) throws IOException
    {
        String chatProtocolVersion = "ChatTP v1.0\n";
        String chatRequestType = "Send-MSG-S\n";
        DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String chatDate = df.format(new Date()) + "\n";
        //Add hash...
        String sndr = sender + "\n";
        String body = text + "\n";
        String msg = chatProtocolVersion + chatRequestType + chatDate + sndr + body;
        //System.out.println(msg);
        byte[] buf = new byte[1024];
        buf = msg.getBytes();
        packet.setAddress(address);
        packet.setPort(port);
        packet.setData(buf);
        socket.send(packet);
    }
 
}
