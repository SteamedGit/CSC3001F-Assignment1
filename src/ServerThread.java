import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.Integer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.stream.Collectors;
 
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
 
    public ServerThread(String name) throws IOException {
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
                    List<ClientData> result = clientData.stream()
                    .filter(a -> a.getName().equals(messageArray[3]))
                    .collect(Collectors.toList());
                    if(result.size() == 0)
                    {
                        clientData.add(new ClientData(packet.getAddress(), packet.getPort(), messageArray[3]));
                    }
                    else
                    {
                        System.out.println("Already registered.");
                    }
                    System.out.println(messageArray[3]+" is registered.");
                    confirmRegister(socket, packet.getAddress(), packet.getPort(), messageArray[3], packet);
                }

                else if(messageArray[1].equals("List-Clients"))
                {
                    sendListOfClients(socket, packet.getAddress(), packet.getPort(), messageArray[3], packet, clientData);
                }



 
        // send the response to the client at "address" and "port"
               
        /*
                InetAddress address = packet.getAddress();
                int port = packet.getPort();
                byte[] sendBuf = new byte[512];
                sendBuf = ("Server: Message Received").getBytes();
                packet = new DatagramPacket(sendBuf, sendBuf.length, address, port);
                socket.send(packet); */
            } 
            catch (IOException e) 
            {
                e.printStackTrace();
            }
        }
        //socket.close();
    }

   /**
    * Sends a request back to the client confirming that it is registered.
    * Example:
    * 
    * ChatTP v1.0
    * Register-Client-REC
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
        String chatRequestType = "Register-Client-REC\n";
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
        //TODO: If the body string is empty, put a "No other clients" in it.
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
        body += "\n";
        String msg = chatProtocolVersion + chatRequestType + chatDate + body;
        byte[] buf = new byte[1024];
        buf = msg.getBytes();
        packet.setData(buf);
        socket.send(packet);
        
    }
 
}
