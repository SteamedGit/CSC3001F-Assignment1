import java.io.IOException;
import java.net.*;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class Message {

    private String text;
    private String sender;
    private String reciever; 
    private boolean sent;
    private boolean recieved;
    private long checkSum;
    private String id;

/**
 * Constructor for a message object. Object will automatically set sent and recieved to false on creation.
 * It will call the checkSum method to get a long value to validate that information about the parameters is correct. 
 * 
 * @param reciever reciever of the message
 * @param sender sender of the message
 * @param text the message
 */
    public Message(String reciever, String sender, String text){
        
        this.reciever = reciever;
        this.sender = sender;
        this.text = text;
        this.sent = false;
        this.recieved = false;
        this.id = "0";

        Check messageCheck = new Check(reciever + sender + text);
        this.checkSum = messageCheck.Checksum();
        
    }

    /**
     * Function which will send a message to the Server in the form of a string which is then converted to bytes. 
     * On the server this should be converted back to a message and the server should setRecieved to true.
     * 
     * 
     * @param socket 
     * @param packet
     * @throws IOException
     */
    public void sendMessage(DatagramSocket socket, DatagramPacket packet) throws IOException{

        String chatProtocolVersion = "ChatTP v1.0\n";
        String chatRequestType = "Send-MSG-C\n";
        DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String chatDate = df.format(new Date());
        //Add hash...
        this.id = text + chatDate;

        String msg = chatProtocolVersion + chatRequestType + chatDate + "\n" + this.toString();

        byte[] buf  = new byte[1024];
        buf = msg.getBytes();
        
        packet.setData(buf); 
        socket.send(packet);
        
    }

    public void setRecieved(boolean recieved) {
        this.recieved = recieved;
    }

    public void setRecieved(String recieved) {
        this.recieved = (recieved.equals("true"));
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
        return reciever;
    }

    public String getSender() {
        return sender;
    }

    public boolean getSent() {
        return sent;
    }

    public boolean getRecieved() {
        return recieved;
    }

    public String getId() {
        return id;
    }


/**
 * the toString is in the format required by the server. 
 */
    @Override
    public String toString() {
        return "" + checkSum + '\n' + reciever + '\n' + text + '\n' + sent + '\n' + recieved  + '\n' + sender + '\n' + id;
    }

/**
 *  This logic here will convert a String to a Message object provided it is in the correct format.
 *  This method should be moved to the server and the client class. There you will be able to take a string and convert to a message. 
 * It has no use here at the moment as this function is related to String manipulation not Message object manipulation. The logic is correct though.
 * 
 * @param s
 * @return
 */

    public Message stringToMessage(String s){  

        String[] lineSplit = s.split("\n", 3);
        String[] ampSplit = lineSplit[1].split("&", 4);

        Message m = new Message(lineSplit[1], ampSplit[0], ampSplit[1]);

        m.setSent(ampSplit[2]);
        m.setRecieved(ampSplit[3]);
        m.setCheckSum(lineSplit[0]);


        return m;


    }

}