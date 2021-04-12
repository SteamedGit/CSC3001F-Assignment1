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
                    String registerName = messageArray[5];
                    String sendingPort = messageArray[4];
                    
                    
                    if(Message.testRegisterCheckSum(Long.parseLong(messageArray[3]), registerName, messageArray[2]))
                    {
                        List<ClientData> result1 = searchByName(registerName); //check if another client with the same name exists

                        List<ClientData> result2 = searchByAddressAndRecPort(packet.getPort(), packet.getAddress()); //check if another client with the same inetaddress and  rport  or sport exists

                        result2.addAll(searchByAddressAndSendPort(packet.getPort(), packet.getAddress())); //add sport and address results 

                        if(result1.size() == 0 && result2.size() == 0)
                        {
                            clientData.add(new ClientData(packet.getAddress(), packet.getPort(), Integer.parseInt(sendingPort), registerName));
                            confirmRegister(socket, packet.getAddress(), packet.getPort(), registerName, packet);
                            System.out.println(">" + registerName+" is registered with receiving port " 
                            + packet.getPort() + " and sending port " + sendingPort + "\n");
                        }
                        if(result1.size() >0)
                        {
                            ClientData matchingClient = result1.get(0);
                            
                            if(matchingClient.getAddress().equals(packet.getAddress()) 
                            && matchingClient.getReceivingPort() == packet.getPort() 
                            && matchingClient.getSendingPort() == Integer.parseInt(sendingPort))
                            {
                                System.out.println("Already registered as " + matchingClient.getName());
                                confirmRegister(socket, packet.getAddress(), packet.getPort(), registerName, packet);
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
                            
                            if(!result2.get(0).getName().equals(registerName))
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
                    else
                    {
                        System.out.println("Register checksum failed.");
                        System.out.println("");
                    }

                    
                    
                    
                }

                else
                {
                    boolean alreadyHave = false;
                    for(ClientData cd: clientData)
                    {
                        ArrayList<Long> checksumsAlreadySent = cd.getMessagesSentToServerCheckSums();
                        
                        if(checksumsAlreadySent.contains(Long.parseLong(messageArray[3]))
                        && (messageArray[1].equals(Message.RESPONSE_SEND_MESSAGE_CLIENT_RECEIPT)))
                        {
                            if(cd.searchMessagesSentToServer(Long.parseLong(messageArray[3])).sameMessage(messageArray[5]))
                            {
                                System.out.println("We already have this message. Ignore it.");
                                alreadyHave = true;
                                break;
                            }
                          
                            
                        }
                        else
                        {
                            if(checksumsAlreadySent.contains(Long.parseLong(messageArray[3])))
                            {
                                System.out.println("We already have this message. Ignore it.");
                                alreadyHave = true;
                                break;
                            }
                        }
                    }
                        if(alreadyHave == false)
                        {
                            if(messageArray[1].equals("Get-Clients"))
                            {
                                if(Message.testGetClientCheckSum(Long.parseLong(messageArray[3]), messageArray[5], messageArray[2]))
                                {
                                    ClientData client = clientData.stream()
                                    .filter(a -> a.getName().equals(messageArray[5]))
                                    .collect(Collectors.toList()).get(0);
                                    
                                    String getClientDetails[] = {messageArray[2], messageArray[3]};
                                    Message getClientMsg = new Message(getClientDetails, Message.REQUEST_GET_CLIENTS, client.getName(),"Server");
                                    client.addMessageSentToServer(getClientMsg.getCheckSum(), getClientMsg);
                
                                    //TODO: Add sent and received and checksum check!
                                    client.incrementReceived();
                                    System.out.println(client.getName() + " " + client.getReceivedNum());
                                    if(!checkSentAndReceivedCounts(messageArray[4], client))
                                    {
                                        Message.getMissingMessages(socket, packet, client.getMessagesSentToServerCheckSums(), client.getAddress(), client.getReceivingPort());
                                        client.incrementSent();
                                    }
                                    client.incrementSent();
                                    sendListOfClients(socket, packet.getAddress(), client.getReceivingPort(), messageArray[5], packet, clientData);
                                }
                                
                                else
                                {
                                    System.out.println("Failed the checksum test.");
                                    //client.decrementSent();
                                    //client.decrementReceived();
                                }
                               
                                
                                
                            }
            
                            else if(messageArray[1].equals("Send-MSG-C")) //TODO: Add confirmation to sender that server received message and get confirmation from recipient of receival
                            {
            
                                //create message object. 
            
                                String senderName = searchByAddressAndSendPort(packet.getPort(), packet.getAddress()).get(0).getName();
                                Message msgCFromClient = new Message(messageArray, Message.REQUEST_SEND_MESSAGE_CLIENT, senderName, messageArray[4]);
            
                                    if (msgCFromClient.testChecksum(Message.REQUEST_SEND_MESSAGE_CLIENT)){  //checks to see if checksum in header and checksum value of data are the same
            
                                        
                                        List<ClientData> result1 = searchByName(msgCFromClient.getReciever()); //looking for receipient
            
                                        List<ClientData> result2= searchByAddressAndSendPort(packet.getPort(), packet.getAddress()); //looking for sender
            
            
                                        System.out.println("Message for " + msgCFromClient.getReciever() + ": " + msgCFromClient.getText());
                                        if (result1.size()>0 && result2.size()>0) //TODO: Handle no matches
                                        {
                                            ClientData receipient = result1.get(0);
                                            ClientData sender = result2.get(0);
            
            
                                            msgCFromClient.setSent(true);
            
                                            //System.out.println(msgCFromClient.getSent());
                                            
                                            sender.addMessageSentToServer(msgCFromClient.getCheckSum(), msgCFromClient);
                                            receipient.addMessageSentFromServer(msgCFromClient.getCheckSum(), msgCFromClient);
            
                                            receipient.incrementSent();
                                            sender.incrementReceived();
                                            System.out.println(sender.getName() + " " + sender.getReceivedNum());
                                            if(!checkSentAndReceivedCounts(messageArray[6], sender))
                                            {
                                                Message.getMissingMessages(socket, packet, sender.getMessagesSentToServerCheckSums(), sender.getAddress(), sender.getReceivingPort());
                                                sender.incrementSent();
                                            }
                                            sender.incrementSent();
                                            
                                            System.out.println("");
                                            System.out.println(receipient.getName());
                                            System.out.println("Sent to address: " + 
                                            receipient.getAddress().toString() + 
                                            " Port: " + Integer.toString(receipient.getReceivingPort()));
                                            
                                            msgCFromClient.messageForward(socket, receipient.getAddress(), receipient.getReceivingPort(), 
                                            packet, receipient.getReceivedNum(), receipient.getSentNum());
            
                                            msgCFromClient.messageSentConfirmation(socket, sender.getAddress(), sender.getReceivingPort(), 
                                            packet, sender.getReceivedNum(), sender.getSentNum());
                                        }
                                    
                                }
                                else {
                                    System.out.println("The checksum is different and thus information is lost"); //TODO: Ask for resend
                                }
                            }
            
                            else if(messageArray[1].equals("Send-MSG-C-RECEIPT")) {
            
                                List<ClientData> result1 = searchByName(messageArray[4]); //looking for receipient(original sender)
            
                                List<ClientData> result2 = searchByAddressAndSendPort(packet.getPort(), packet.getAddress()); //looking for sender
            
                                
                                System.out.println("Receipt for message");
                                if (result1.size()>0 && result2.size()>0)
                                {
                                    
                                    ClientData recipient = result1.get(0); //original sender
                                    ClientData sender = result1.get(0); //original receipient, one who sent receipt
            
                                
            
                                   // System.out.println("Sending the receipt to " + recipient.getName());
                                    Message msgCFMessage = recipient.searchMessagesSentToServer(Long.parseLong(messageArray[3]));

                                    if(msgCFMessage.testChecksum(Message.RESPONSE_SEND_MESSAGE_CLIENT_RECEIPT))
                                    {
                                        recipient.incrementSent();
                                        sender.incrementReceived();
                                        System.out.println(sender.getName() + " " + sender.getReceivedNum());
                                        if (!checkSentAndReceivedCounts(messageArray[6], sender))
                                        {
                                            //Message.getMissingMessages(socket, packet, sender.getMessagesSentToServer());
                                            Message.getMissingMessages(socket, packet,sender.getMessagesSentToServerCheckSums(), sender.getAddress(), sender.getReceivingPort());
                                            sender.incrementSent();
                                        }
                                       
                                        System.out.println(recipient.getName());
                                        System.out.println("Sent to address: " + 
                                        recipient.getAddress().toString() + 
                                        " Port: " + Integer.toString(recipient.getReceivingPort()));
                                        msgCFMessage.sendMessageReceiptServer(socket, recipient.getAddress(), recipient.getReceivingPort(), 
                                        recipient.getName(), packet, recipient.getReceivedNum(), recipient.getSentNum());
                                    }
                                    else
                                    {
                                        System.out.println("Checksum test failed.");
                                    }
            
                                
                                    
                                    
                                    
                                }
            
            
                            }
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
     * Use streams to search client data for a client with a matching name.
     * @param clientName
     */
    protected List<ClientData> searchByName(String clientName)
    {
        return clientData.stream()
        .filter(a -> a.getName().equals(clientName))
        .collect(Collectors.toList());
    }

    
    /**
     * Uses streams to search client data for a client with a matching receiving port and address.
     * @param clientRecPort
     * @param clientAddress
     * @return
     */
    protected List<ClientData> searchByAddressAndRecPort(int clientRecPort, InetAddress clientAddress)
    {
        return clientData.stream() //looking for sender
        .filter(a -> a.getReceivingPort()==clientRecPort)
        .filter(a-> a.getAddress().equals(clientAddress))
        .collect(Collectors.toList());
    }

     /**
     * Uses streams to search client data for a client with a matching sending port and address.
     * @param clientSendPort
     * @param clientAddress
     * @return
     */
    protected List<ClientData> searchByAddressAndSendPort(int clientSendPort, InetAddress clientAddress)
    {
        return clientData.stream()
                    .filter(a -> a.getSendingPort()==clientSendPort)
                    .filter(a-> a.getAddress().equals(clientAddress))
                    .collect(Collectors.toList());
    }


    protected boolean checkSentAndReceivedCounts(String sentAndReceivedCountsLine, ClientData client)
    {
        
        String lines[] = sentAndReceivedCountsLine.split("\\|");
        int clientReceivedFromServerCount =  Integer.parseInt(lines[0].substring(3));  
        int clientSentToServerCount =  Integer.parseInt(lines[1].substring(3));

        if(clientReceivedFromServerCount != client.getSentNum()) //if number of messages the client has received does not match the number sent by server
        {
            System.out.println("Non-critical issue: Number of messages "+ client.getName() +" has received: " + clientReceivedFromServerCount 
            + " does not match number sent by server " + client.getSentNum()); //doesnt actually matter. server never drops messages
        }
        else
        {
            System.out.println(clientReceivedFromServerCount + " matches " + client.getSentNum());
        }
        if(clientSentToServerCount > client.getReceivedNum()) //if number of messages the client has sent does not match the number received by server
        {
            System.out.println("Number of messages " + client.getName() + " has sent: " + clientSentToServerCount 
            + " does not match number received by server " + client.getReceivedNum());
            return false; // we need to ask the client to resend the missing messages
        }
        else if(clientSentToServerCount < client.getReceivedNum())
        {
            System.out.println("Strange... server has more than 'sent'");
            System.out.println("Number of messages " + client.getName() + " has sent: " + clientSentToServerCount 
            + " does not match number received by server " + client.getReceivedNum());
            return true;
        }
        else
        {
            System.out.println(clientSentToServerCount + " matches " + client.getReceivedNum());
        }
        return true;
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
        String chatProtocolVersion = "ChatTP v1.0\n"; //TODO: Make this method a static method in the message class.
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
        String chatProtocolVersion = "ChatTP v1.0\n"; //TODO: Make this method a static method in the message class
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
        String chatProtocolVersion = "ChatTP v1.0\n"; //TODO: Make this method a static method in the message class
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

 
}
