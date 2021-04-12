import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;


import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Message {

    private String text;
    private String senderOfOriginalMessage;
    private String receipientOfOriginalMessage; 

    private boolean sent;
    private String received;
    private long checkSum;
    private String timeStamp;
    private String stringRepresentation;


    static final String CHAT_PROTOCOL_VERSION = "ChatTP v1.0";

    static final String REQUEST_REGISTER_CLIENT = "Register-Client";
    static final String REQUEST_SEND_MESSAGE_CLIENT = "Send-MSG-C";
    static final String REQUEST_GET_CLIENTS = "Get-Clients";
    static final String REQUEST_GET_MISSING_MESSAGES = "Get-Missing-Messages-S";


    static final String RESPONSE_REGISTER_CLIENT_OK = "Register-Client-OK";
    static final String RESPONSE_REGISTER_CLIENT_BAD = "Register-Client-BAD";
    static final String RESPONSE_SEND_MESSAGE_CLIENT_RECEIPT = "Send-MSG-C-RECEIPT";
    static final String RESPONSE_SEND_MESSAGE_SERVER = "Send-MSG-S";
    static final String RESPONSE_SEND_MESSAGE_SERVER_SENT = "Send-MSG-S-SENT";
    static final String RESPONSE_SEND_MESSAGE_SERVER_RECEIPT = "Send-MSG-S-RECEIPT";
    static final String RESPONSE_LIST_CLIENTS = "List-Of-Clients";

/**
 * Constructor for a message object. Object will automatically set sent and recieved to false on creation.
 * It will call the checkSum method to get a long value to validate that information about the parameters is correct. 
 * 
 * @param receipientOfOriginalMessage reciever of the message
 * @param senderOfOriginalMessage senderOfOriginalMessage of the message
 * @param text the message
 */
    public Message(String receipientOfOriginalMessage, String senderOfOriginalMessage, String text){
        
        this.receipientOfOriginalMessage = receipientOfOriginalMessage;
        this.senderOfOriginalMessage = senderOfOriginalMessage;
        this.text = text;
        this.sent = false;
        this.received = "0";

        this.checkSum = 0;
        
    }

    public Message(String[] stringArray, String messageType, String senderOfOriginalMessage, String receipient)
    {
        if(messageType.equals(REQUEST_SEND_MESSAGE_CLIENT))
        {
            this.senderOfOriginalMessage = senderOfOriginalMessage;
            this.timeStamp = stringArray[2];
            this.checkSum = Long.parseLong(stringArray[3]);
            this.receipientOfOriginalMessage = stringArray[4];
            String receiptLine[] = stringArray[5].split("\\|");
            this.sent = Boolean.parseBoolean(receiptLine[0].substring(8));
            this.received = receiptLine[1].substring(9);
            this.text = stringArray[7];          


        }

        if(messageType.equals(RESPONSE_SEND_MESSAGE_SERVER))
        {
            this.senderOfOriginalMessage = senderOfOriginalMessage;
            //String lines[] = stringRepresentation.split("\n");
            this.timeStamp = stringArray[2];
            this.checkSum = Long.parseLong(stringArray[3]);
            this.receipientOfOriginalMessage = receipient;
            String receiptLine[] = stringArray[5].split("\\|");
            this.sent = Boolean.parseBoolean(receiptLine[0].substring(8));
            this.received = receiptLine[1].substring(9);
            this.text = stringArray[7];          

        }

        if(messageType.equals(REQUEST_GET_CLIENTS)) //special msg
        {
            this.senderOfOriginalMessage = senderOfOriginalMessage;
            this.timeStamp = stringArray[0];
            this.checkSum = Long.parseLong(stringArray[1]);
            this.receipientOfOriginalMessage = receipient;
            this.text = REQUEST_GET_CLIENTS;
        }
    }

    /**
     * Function which will send a message to the Server in the form of a string which is then converted to bytes. 
     * On the server this should be converted back to a message and the server should setRecieved to true.
     * 
     * ChatTP v1.0 //protocol and version 0
     * Send-MSG-C //request type          1
     * 29-03-2021 00:30:45 //timestamp    2
     * 12239596532059 //checksum          3
     * Client2 //Receipient               4
     * <received by client & server has msg> 5    
     * <Received & sent count>            6
     * <text>                             7
     * 
     * 
     * @param socket 
     * @param packet
     * @throws IOException
     */
    public void sendMessageC(DatagramSocket socket, DatagramPacket packet, int receivedCount, int sentCount) throws IOException{

        String chatProtocolVersion = "ChatTP v1.0\n";
        String chatRequestType = "Send-MSG-C\n";
        DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String chatDate = df.format(new Date());
        this.timeStamp = chatDate;

        String sentAndReceivedCount = "R: " + Integer.toString(receivedCount) + "|" + "S: " + Integer.toString(sentCount);

        this.checkSum = new Check(receipientOfOriginalMessage + text + chatDate).Checksum();

        String msg = chatProtocolVersion + chatRequestType  + chatDate + "\n" 
        + checkSum + '\n' + receipientOfOriginalMessage + "\n" + serverAndClientReceivedToString() +'\n' + sentAndReceivedCount + "\n"
        + text + '\n';

        this.stringRepresentation = msg;
  

        byte[] buf  = new byte[1024];
        buf = msg.getBytes();
        
        packet.setData(buf); 
        corruptOrDontSend(socket, packet, msg);
        
    }

    public void resendMessageC(DatagramSocket socket, DatagramPacket packet, int receivedCount, int sentCount) throws IOException
    {
        String chatDate = this.timeStamp;
        String sentAndReceivedCount = "R: " + Integer.toString(receivedCount) + "|" + "S: " + Integer.toString(sentCount);
        String checkSum = Long.toString(this.checkSum);
        String msg = Message.CHAT_PROTOCOL_VERSION + "\n" +Message.REQUEST_SEND_MESSAGE_CLIENT + "\n"  + chatDate + "\n" 
        + checkSum + '\n' + receipientOfOriginalMessage + "\n" + serverAndClientReceivedToString() +'\n' + sentAndReceivedCount + "\n"
        + text + '\n';
        System.out.println(msg);
        byte[] buf  = new byte[1024];
        buf = msg.getBytes();
        
        packet.setData(buf); 
        corruptOrDontSend(socket, packet, msg);


    }

     /**
     * Forwards a client's message to the intended recipient. NOTE: Uses same checksum and timestamp as the corresponding msg-c.
     * 
     * Example:
     * 
     * ChatTP v1.0
     * Send-MSG-S
     * 29-03-2021 00:30:45
     * <Checksum>
     * <senderOfOriginalMessage>
     * <Received by client and server has message>
     * <Received and sent count>
     * <Text>
     * 
     * 
     * 
     * @param socket
     * @param address
     * @param port
     * @param senderOfOriginalMessage
     * @param text
     * @param packet
     * @throws IOException
     */
    public void messageForward(DatagramSocket socket, InetAddress address, int port, DatagramPacket packet, int receivedFromClient, int sentToClient) throws IOException
    {
        String chatDate = this.timeStamp + "\n";
        String sndr = this.senderOfOriginalMessage + "\n";
        String checksum = Long.toString(this.checkSum)  + '\n';
        String body = this.text + "\n";
        String msg = CHAT_PROTOCOL_VERSION + "\n" + RESPONSE_SEND_MESSAGE_SERVER + "\n" + chatDate + checksum + sndr + "Server: " + sent 
        + "|" + "Received: " + received + "\n" + "R: " + Integer.toString(receivedFromClient) + "|" + "S: " 
        + Integer.toString(sentToClient) + "\n" + body;
        byte[] buf = new byte[1024];
        buf = msg.getBytes();
        packet.setAddress(address);
        packet.setPort(port);
        packet.setData(buf);
        socket.send(packet);
        
    }


    /**
     * Sends confirmation back to the senderOfOriginalMessage that the server has forwared their message.
     * 
     * Example:
     * 
     * ChatTP v1.0
     * Send-MSG-S-SENT
     * 29-03-2021 00:30:45
     * <Checksum>
     * <senderOfOriginalMessage>
     * <Received by client and server has message>
     * <Received and sent count>
     * <Text>
     * 
     * @param socket
     * @param address
     * @param port
     * @param packet
     * @param receivedFromClient
     * @param sentToClient
     * @throws IOException
     */
    public void messageSentConfirmation(DatagramSocket socket, InetAddress address, int port, DatagramPacket packet, int receivedFromClient, int sentToClient) throws IOException 
    {
        String chatDate = this.timeStamp + "\n";
        String sndr = this.senderOfOriginalMessage + "\n";
        String checksum = Long.toString(this.checkSum) + '\n';
        String body = this.text + "\n";
        String msg = CHAT_PROTOCOL_VERSION + "\n" + RESPONSE_SEND_MESSAGE_SERVER_SENT + "\n" + chatDate + checksum + sndr + "Server: " + sent 
        + "|" + "Received: " + received + "\n" + "R: " + Integer.toString(receivedFromClient) + "|" + "S: " 
        + Integer.toString(sentToClient) + "\n" + body;

        byte[] buf = new byte[1024];
        buf = msg.getBytes();
        packet.setAddress(address);
        packet.setPort(port);
        packet.setData(buf);
        socket.send(packet);
    }


    
    /**
     * Sends confirmation back to server that the receipient has received the message.
     * 
     * Example:
     * 
     * ChatTP v1.0
     * Send-MSG-C-RECEIPT
     * 29-03-2021 00:30:45
     * <Checksum>
     * <Recipient> //senderOfOriginalMessage
     * <Received by client and server has message>
     * <Received and sent count>
     * <Text>
     * 
     */
    public void sendMessageReceiptClient(DatagramSocket socket, String originalSender, DatagramPacket packet, int receivedFromServer, int sentToServer) throws IOException
    {
        String chatDate = this.timeStamp + "\n";
        String recip = originalSender + "\n";
        String checksum = Long.toString(this.checkSum) + '\n';
        String body = this.text + "\n";

        //String msg = chatProtocolVersion + chatRequestType + chatDate + recip + body;
        String msg = CHAT_PROTOCOL_VERSION + "\n" + RESPONSE_SEND_MESSAGE_CLIENT_RECEIPT + "\n" + chatDate + checksum + recip  + "Server: " + sent 
        + "|" + "Received: " + received + "\n" + "R: " + Integer.toString(receivedFromServer) + "|" + "S: " 
        + Integer.toString(sentToServer) + "\n" + body;
        byte[] buf = new byte[1024];
        buf = msg.getBytes();
        packet.setData(buf);
        
        corruptOrDontSend(socket, packet, msg);
    }


    /**
     * Sends a confirmation from the server back to a senderOfOriginalMessage that the message has been received by the receipient.\
     * 
     * Example:
     * ChatTP v1.0
     * Send-MSG-S-RECEIPT
     * 29-03-2021 00:30:45
     * <Checksum>
     * <Receipient> // the original senderOfOriginalMessage
     * <Received by client and server has message>
     * <Received and sent count>
     * <Text>
     * 
     * 
     * @param socket
     * @param address
     * @param port
     * @param senderOfOriginalMessage
     * @param id
     * @param packet
     * @throws IOException
     */
    public void sendMessageReceiptServer(DatagramSocket socket, InetAddress address, int port, String senderOfOriginalMessage, DatagramPacket packet, int receivedFromClient, int sentToClient) throws IOException 
    {
        String chatDate = this.timeStamp + "\n";
        String sndr = senderOfOriginalMessage + "\n"; //send to the original senderOfOriginalMessage
        String checksum = Long.toString(this.checkSum) + "\n";
        String body = this.text + "\n";
        String msg = CHAT_PROTOCOL_VERSION + "\n" + RESPONSE_SEND_MESSAGE_SERVER_RECEIPT + "\n" + chatDate + checksum + sndr + "Server: " + sent 
        + "|" + "Received: " + received + "\n" + "R: " + Integer.toString(receivedFromClient) + "|" + "S: " 
        + Integer.toString(sentToClient) + "\n" + body;

        byte[] buf = new byte[1024];
        buf = msg.getBytes();
        packet.setAddress(address);
        packet.setPort(port);
        packet.setData(buf);
        socket.send(packet);
    }

    public void setRecieved(String recieved) {
        this.received = recieved;
    }

    public void setSent(boolean sent) {
        this.sent = sent;
    }

    public void setSent(String sent) {
        this.sent = (sent.equals("true"));
    }

    public void setCheckSum(long checkSum) {
        this.checkSum = checkSum;
    }

    public void setCheckSum(String checkSum) {
        this.checkSum = Long.parseLong(checkSum);
    }


    public String getText() {
        return text;
    }

    public long getCheckSum() {
        return checkSum;
    }

    public String getReciever() {
        return receipientOfOriginalMessage;
    }

    public String getsenderOfOriginalMessage() {
        return senderOfOriginalMessage;
    }

    public boolean getSent() {
        return sent;
    }

    public String getRecieved() {
        return received;
    }

    public String getTimestamp()
    {
        return timeStamp;
    }

    public boolean testChecksum(String messageType)
    {
        
        if(messageType.equals(Message.REQUEST_SEND_MESSAGE_CLIENT) || messageType.equals(Message.RESPONSE_SEND_MESSAGE_CLIENT_RECEIPT))
        {
            Long tmpChecksum = new Check(receipientOfOriginalMessage + this.text + this.timeStamp).Checksum();
            return tmpChecksum.equals(this.checkSum);
        }
        System.out.println("No recognised client msg type.");
        return false;
       
    }

    private String serverAndClientReceivedToString()
    {
        return "Server: " + sent +"|" + "Received: " + received;
    }


/**
 * the toString is in the format required by the server. 
 */
    @Override
    public String toString() {
        return stringRepresentation;
    }

    /**
     * Static method sending request to server to register the client.
     * 
     * Example:
     * 
     * ChatTP v1.0
     * Register-Client
     * 29-03-2021 00:30:45
     * <Checksum> //(clientName + timestamp)
     * <Sending port>
     * <Client Name>
     * 
     * 
     * @param socket
     * @param sendingPort
     * @param clientName
     * @param packet
     * @throws IOException
     */
    
    public static void registerClient(DatagramSocket socket, int sendingPort,String clientName, DatagramPacket packet) throws IOException
    {

        DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String chatDate = df.format(new Date());
        String checksum = Long.toString(new Check(clientName + chatDate).Checksum()) + "\n";
        String sPort = Integer.toString(sendingPort) + "\n";
        String body = clientName + "\n";
        String msg = CHAT_PROTOCOL_VERSION + "\n" + REQUEST_REGISTER_CLIENT + "\n" + chatDate + "\n" + checksum + sPort + body;
        
        byte[] buf = new byte[1024];
        buf = msg.getBytes();
        packet.setData(buf);
        
        //socket.send(packet);
        corruptOrDontSend(socket, packet, msg);
    }


    public static boolean testRegisterCheckSum(Long checksum, String clientName,String chatDate)
    {
        Long tmpChecksum = new Check(clientName + chatDate).Checksum();
        return checksum.equals(tmpChecksum);
    }

    public static boolean testGetClientCheckSum(Long checksum, String clientName, String chatDate)
    {
        Long tmpChecksum = new Check(clientName + chatDate).Checksum();
        return tmpChecksum.equals(checksum);
    }


       /**
     * This request asks for a list of all other registered clients from the server.
     * Example of this type of request:
     * 
     * ChatTP v1.0
     * Get-Clients
     * 29-03-2021 00:30:45
     * <Checksum> // Client Name + chatDate
     * <Received from server and sent to server count>
     * <Client Name>
     * 
     * @param socket
     * @param clientName
     * @param packet
     * @throws IOException
     */
    public static String[] getOtherClients(DatagramSocket socket, String clientName, DatagramPacket packet, int receivedFromServer, int sentToServer) throws IOException
    {
        DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String chatDate = df.format(new Date());
        String body = clientName + "\n";
        Long checkSum = new Check(clientName + chatDate).Checksum();
        String sentAndReceivedCount = "R: " + Integer.toString(receivedFromServer) + "|" + "S: " + Integer.toString(sentToServer) + "\n";
        String msg = CHAT_PROTOCOL_VERSION + "\n" + REQUEST_GET_CLIENTS + "\n" + chatDate + "\n" + checkSum + "\n" + sentAndReceivedCount + body;
        byte[] buf = new byte[1024];
        buf = msg.getBytes();
        packet.setData(buf);
        
        //socket.send(packet);
        corruptOrDontSend(socket, packet, msg);
        String getOtherClientsDetails[] = {chatDate, Long.toString(checkSum)};
        return getOtherClientsDetails;

    }


    /**
     * Overloaded getOtherClients for resending this message
     * Request looks identical to the other version.
     */
    public static void getOtherClients(DatagramSocket socket,String clientName ,String timestamp, Long checksum, DatagramPacket packet, int receivedFromServer, int sentToServer) throws IOException
    {
        String chatDate = timestamp + "\n";
        String body = clientName + "\n";
        Long checkSum = checksum;
        String sentAndReceivedCount = "R: " + Integer.toString(receivedFromServer) + "|" + "S: " + Integer.toString(sentToServer) + "\n";
        String msg = CHAT_PROTOCOL_VERSION + "\n" + REQUEST_GET_CLIENTS + "\n" + chatDate + checkSum + "\n" + sentAndReceivedCount + body;
        byte[] buf = new byte[1024];
        buf = msg.getBytes();
        packet.setData(buf);
        
        //socket.send(packet);
        corruptOrDontSend(socket, packet, msg);
    }
    /**
     * Sends request to client to send the missing message(s). The body of the message is a list of all the checksums of the messages that 
     * the server has received from the client.
     * 
     * ChatTP v1.0
     * Get-Missing-Messages-S
     * 29-03-2021 00:30:45
     * <Checksum> // checksums of messages received + chatDate
     * <List of received messages by their checksum> //eg.3378710955 2412251111
     */
    public static void getMissingMessages(DatagramSocket socket, DatagramPacket packet, ArrayList<Long> checkSumsOfReceivedMessages, InetAddress address, int port) throws IOException
    {
        DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String chatDate = df.format(new Date()) + "\n";
        String checksums;
        if(checkSumsOfReceivedMessages.isEmpty())
        {
            checksums = "No checksums on server";
        }
       else
       {
            checksums = checkSumsOfReceivedMessages.stream()
                        .map(l -> Long.toString(l)).collect(Collectors.joining(" "));
       } 
        String body = checksums + "\n";
        Long checkSum = new Check(checksums + chatDate).Checksum();

        String msg = CHAT_PROTOCOL_VERSION + "\n" + REQUEST_GET_MISSING_MESSAGES + "\n" + chatDate + checkSum + "\n" + body;
        System.out.println(msg);
        byte[] buf = new byte[1024];
        buf = msg.getBytes();
        packet.setData(buf);

        packet.setAddress(address);
        packet.setPort(port);
        
        socket.send(packet);
    }


    /**
     * 10% of the time it either corrupts or doesnt send the message.
     * Only used in client-side messages.
     * @param socket
     * @param packet
     * @param msg
     */
    private static void corruptOrDontSend(DatagramSocket socket, DatagramPacket packet, String msg) throws IOException 
    {
        Random rand = new Random();
        if(rand.nextInt(10)==0)//generates a pseudorandom int between 0 and 9 incl. The odds of the value being 0 are 1 in 10, or 10%.
        {
            //Here we corrupt or dont send
            int cOrS = rand.nextInt(2); // [0,1]
            if(cOrS==0) //we dont send
            {
                System.out.println("Didn't send this message");
            }
            else //we corrupt
            {
                System.out.println("Corrupt data and send.");
                List<String> messageLines = Arrays.asList(msg.split("\n"));
                String txt = messageLines.get(messageLines.size()-1);
                txt += rand.nextInt(1000) + rand.nextInt(1000);
                messageLines.set(messageLines.size()-1, txt);
                String corruptedMessage = String.join("\n",messageLines) +"\n";
                byte buf[] = new byte[1024];
                buf = corruptedMessage.getBytes();
                packet.setData(buf);
                socket.send(packet);
            }
        }
        else
        {
            byte buf[] = new byte[1024];
            buf = msg.getBytes();
            packet.setData(buf);
            socket.send(packet);
        }
      
    }


    public boolean sameMessage(String sentAndReceivedFlagLine)
    {
        String receiptLine[] = sentAndReceivedFlagLine.split("\\|");
        Boolean tmpSent = Boolean.parseBoolean(receiptLine[0].substring(8));
        String tmpReceived = receiptLine[1].substring(9);
        if(tmpSent.equals(this.sent) && tmpReceived.equals(this.received))
        {
            return true;
        }
        return false;
    }



   

}