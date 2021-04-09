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
                String[] messageArray = messageFromClient.split("\n", 10);   // (Alaric Eddited) changed max splits to 6 to allow \n to be allowed in the body. solving apesands

                if (messageArray[1].equals("Register-Client"))
                {
                    
                    List<ClientData> result1 = clientData.stream() //check if another client with the same name exists
                    .filter(a -> a.getName().equals(messageArray[4]))
                    .collect(Collectors.toList());

                    List<ClientData> result2 = clientData.stream() //check if another client with the same inetaddress and  rport  or sport exists
                    .filter(a -> a.getReceivingPort()==packet.getPort())
                    .filter(a-> a.getAddress().equals(packet.getAddress()))
                    .collect(Collectors.toList());

                    result2.addAll(clientData.stream()
                    .filter(a -> a.getSendingPort()==packet.getPort())
                    .filter(a-> a.getAddress().equals(packet.getAddress()))
                    .collect(Collectors.toList()));

                    if(result1.size() == 0 && result2.size() == 0)
                    {
                        clientData.add(new ClientData(packet.getAddress(), packet.getPort(), Integer.parseInt(messageArray[3]) ,messageArray[4]));
                        confirmRegister(socket, packet.getAddress(), packet.getPort(), messageArray[4], packet);
                        System.out.println(">" + messageArray[4]+" is registered with receiving port " 
                        + packet.getPort() + " and sending port " + messageArray[3] + "\n");
                    }
                    if(result1.size() >0)
                    {
                        if(result1.get(0).getAddress().equals(packet.getAddress()) 
                        && result1.get(0).getReceivingPort() == packet.getPort() 
                        && result1.get(0).getSendingPort() == Integer.parseInt(messageArray[3]))
                        {
                            System.out.println("Already registered as " + result1.get(0).getName());
                            confirmRegister(socket, packet.getAddress(), packet.getPort(), messageArray[4], packet);
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
                        
                        if(!result2.get(0).getName().equals(messageArray[4]))
                        {
                            String error = "Someone else is registered at " + result2.get(0).getAddress() 
                            + " with receiving port " + result2.get(0).getReceivingPort() 
                            + " and sending port " + result2.get(0).getSendingPort() 
                            + " as " + result2.get(0).getName();
                            System.out.println(error);
                            badRegister(socket, packet.getAddress(), packet.getPort(), error, packet);
                        }
                        
                    }
                    
                    
                }

                else if(messageArray[1].equals("Get-Clients"))
                {
                    ClientData client = clientData.stream()
                    .filter(a -> a.getName().equals(messageArray[3]))
                    .collect(Collectors.toList()).get(0);
                    
                    sendListOfClients(socket, packet.getAddress(), client.getReceivingPort(), messageArray[3], packet, clientData);
                }

                else if(messageArray[1].equals("Send-MSG-C")) //TODO: Add confirmation to sender that server received message and get confirmation from recipient of receival
                {

                    // index 3: checksum   4: reciever    5: text    6: sent    7: recieved    8: sender
                    // reciever + sender + text   = hash for checksum
                    //

                    //create message object. 

                    Message messageOnServer = new Message(messageArray[4], messageArray[8], messageArray[5]);

                        if (messageOnServer.getCheckSum() == Long.parseLong(messageArray[3])){  //checks to see if checksum sent and checksum done are the same

                            
                                
                            
                        
                            List<ClientData> result1 = clientData.stream() //looking for receipient
                            .filter(a -> a.getName().equals(messageArray[4]))
                            .collect(Collectors.toList());

                            List<ClientData> result2 = clientData.stream() //looking for sender
                            .filter(a -> a.getSendingPort()==packet.getPort())
                            .filter(a-> a.getAddress().equals(packet.getAddress()))
                            .collect(Collectors.toList());

                            System.out.println("Message for " + messageArray[4] + ": " + messageArray[5]);
                            if (result1.size()>0 && result2.size()>0)
                            {
                                System.out.println(result1.get(0).getName());
                                System.out.println("Sent to address: " + 
                                result1.get(0).getAddress().toString() + 
                                " Port: " + Integer.toString(result1.get(0).getReceivingPort()));
                                message(socket, result1.get(0).getAddress(), result1.get(0).getReceivingPort(), result2.get(0).getName() , messageArray[5], packet, messageArray[9]);
                                messageS(socket, result2.get(0).getAddress(), result2.get(0).getReceivingPort(), result1.get(0).getName(), messageArray[9], packet);
                            }
                        
                    }
                    else {
                        System.out.println("The checksum is different and thus information is lost");
                    }
                }

                else if(messageArray[1].equals("Send-MSG-RECIEPT-C")) {

                    List<ClientData> result1 = clientData.stream() //looking for receipient
                    .filter(a -> a.getName().equals(messageArray[3]))
                    .collect(Collectors.toList());

                    List<ClientData> result2 = clientData.stream() //looking for sender
                    .filter(a -> a.getSendingPort()==packet.getPort())
                    .filter(a-> a.getAddress().equals(packet.getAddress()))
                    .collect(Collectors.toList());

                        System.out.println("Reciept for message");
                        if (result1.size()>0 && result2.size()>0)
                        {
                            System.out.println(result1.get(0).getName());
                            System.out.println("Sent to address: " + 
                            result1.get(0).getAddress().toString() + 
                            " Port: " + Integer.toString(result1.get(0).getReceivingPort()));
                            System.out.println("AM I GETTING HERE WITH");
                            messageR(socket, result1.get(0).getAddress(), result1.get(0).getReceivingPort(), result2.get(0).getName(), messageArray[4], packet);
                            
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
    * @param packetx
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
    protected void message(DatagramSocket socket, InetAddress address, int port, String sender, String text, DatagramPacket packet, String id) throws IOException
    {
        String chatProtocolVersion = "ChatTP v1.0\n";
        String chatRequestType = "Send-MSG-S\n";
        DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String chatDate = df.format(new Date()) + "\n";
        //Add hash...
        String sndr = sender + "\n";
        String identity = id + '\n';
        String body = text + "\n";
        String msg = chatProtocolVersion + chatRequestType + chatDate + sndr + body + identity;
        //System.out.println(msg);
        byte[] buf = new byte[1024];
        buf = msg.getBytes();
        packet.setAddress(address);
        packet.setPort(port);
        packet.setData(buf);
        socket.send(packet);
        
    }

    protected void messageR(DatagramSocket socket, InetAddress address, int port, String sender, String id, DatagramPacket packet) throws IOException 
    {
        String chatProtocolVersion = "ChatTP v1.0\n";
        String chatRequestType = "Send-MSG-RECIEPT-S\n";
        DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String chatDate = df.format(new Date()) + "\n";
        String sndr = sender + "\n";
        String identity = id ;
        String msg = chatProtocolVersion + chatRequestType + chatDate + sndr + identity;

        byte[] buf = new byte[1024];
        buf = msg.getBytes();
        packet.setAddress(address);
        packet.setPort(port);
        packet.setData(buf);
        socket.send(packet);
    }

    protected void messageS(DatagramSocket socket, InetAddress address, int port, String sender, String id, DatagramPacket packet) throws IOException 
    {
        String chatProtocolVersion = "ChatTP v1.0\n";
        String chatRequestType = "Send-MSG-SENT-S\n";
        DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String chatDate = df.format(new Date()) + "\n";
        String sndr = sender + "\n";
        String identity = id ;
        String msg = chatProtocolVersion + chatRequestType + chatDate + sndr + identity;

        byte[] buf = new byte[1024];
        buf = msg.getBytes();
        packet.setAddress(address);
        packet.setPort(port);
        packet.setData(buf);
        socket.send(packet);
    }
 
}
