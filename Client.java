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
 * "4446" "Ali" "localhost" --> Example of a started client.
 */
public class Client
{
    
    public static void main(String[] args) throws IOException, InterruptedException {

        AtomicBoolean isRegistered = new AtomicBoolean(false);
        AtomicBoolean isRegisterSocketOpen = new AtomicBoolean(false);
        String otherClients = new String();
        History history = new History(); //object that updates history

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
            new RegisterClientThread(socket, isRegistered, isRegisterSocketOpen, args[1], Integer.parseInt(args[0])+1, address, 4445).start();
            TimeUnit.SECONDS.sleep(1);
            socket.close();
            isRegisterSocketOpen.set(false);
        }

        InetAddress address = InetAddress.getByName(args[2] );
        DatagramSocket socket = new DatagramSocket(Integer.parseInt(args[0])+1);
        byte[] buf = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 4445);
        String[] messageArray;
        ReceiverClientThread rThread = new ReceiverClientThread(Integer.parseInt(args[0]));
        rThread.start();



        ArrayList<Message> messageList = new ArrayList<Message>();

        
        
        //Gets the list of all other registered clients.
        //This tells us who we can message.
        getOtherClients(socket, args[1], packet);
        TimeUnit.SECONDS.sleep(1);
        while(!rThread.messagesFromServer.isEmpty())
        {
            
            messageArray = rThread.messagesFromServer.poll();
            System.out.println("Available Clients: " + messageArray[3]);
        }


        //Send and Receive Messages
        Scanner input = new Scanner(System.in);
        while(true)
        {
            System.out.println("Do you want to send a message(S) or quit?(Q)");
            String sRQuit = input.nextLine();
            System.out.println("");
            if(sRQuit.toLowerCase().equals("s"))
            {
                
                System.out.println("Who do you want to send a message to?");
                String recipient = input.nextLine();
                System.out.println("What is the message?");
                String text = input.nextLine();
                System.out.println("");


                Message m = new Message(recipient, args[1], text);    //changed to create object
                history.update(m); //Updates history for those particular clients
                m.sendMessage(socket, packet);
                messageList.add(m);
                

            }
            if(sRQuit.toLowerCase().equals("quit") || sRQuit.toLowerCase().equals("q") )
            {
                break;
            }
            while(!rThread.messagesFromServer.isEmpty())  //this is how we see all the messages we have been sent
            {
                messageArray = rThread.messagesFromServer.poll();               
                
                System.out.println("Message from server: " + messageArray[1]);

                if (messageArray[1].equals("Send-MSG-S")){
                
                    System.out.println("Message from " + messageArray[3] + ":");
                    System.out.println(messageArray[4]+"\n");
                    sendMessageReciept(socket, messageArray[3], messageArray[5], packet);
                }

                if (messageArray[1].equals("Send-MSG-SENT-S")){
                    System.out.println("Message has been sent, message id = " + messageArray[4]);
                    
                    for (Message msg : messageList) { 		      
                        if (msg.getId().equals(messageArray[4])){
                            msg.setSent(true);
                            System.out.println("Message FOUND and set to SENT");
                            
                        }
                   }

                }


                if (messageArray[1].equals("Send-MSG-RECIEPT-S")){
                    System.out.println("reciept for:  " + messageArray[4]);


                    for (Message msg : messageList) { 		      
                        if (msg.getId().equals(messageArray[4])){
                            msg.setRecieved(true);
                            msg.setSent(true);
                            System.out.println("Message FOUND and set to  SENT and RECIEVED");
                            
                        }
                   }
                }
            }
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

    private static void sendMessageReciept(DatagramSocket socket, String recipient, String id, DatagramPacket packet) throws IOException
    {
        String chatProtocolVersion = "ChatTP v1.0\n";
        String chatRequestType = "Send-MSG-RECIEPT-C\n";
        DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String chatDate = df.format(new Date()) + "\n";
        //Add hash...
        String recip = recipient + "\n";
        String body = id;

        String msg = chatProtocolVersion + chatRequestType + chatDate + recip + body;
        byte[] buf = new byte[1024];
        buf = msg.getBytes();
        packet.setData(buf);
        
        socket.send(packet);
    }



}
