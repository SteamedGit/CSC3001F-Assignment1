import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


/**
 * This class defines the functionality of the client.
 */
public class Client
{
    String port, name, hostname;

    AtomicBoolean isRegistered;
    AtomicBoolean isRegisterSocketOpen;
    AtomicInteger numberOfSentMessages;
    Map<Long, Message> messagesReceivedFromServer;
    Map<Long, Message> messagesSentFromClient;
    Map<Long, Message> specialRequestsSentFromClient;
    String otherClients;

    DatagramSocket socket;
    InetAddress address;
    byte[] buf;
    DatagramPacket packet;
    String[] messageArray;
    ReceiverClientThread rThread;
    History clientHistory;

    public Client(String port, String name, String hostname) throws NumberFormatException, IOException, InterruptedException{
        this.port = port;
        this.name = name;
        this.hostname = hostname;

        this.clientHistory = new History();
        this.isRegistered = new AtomicBoolean(false);
        this.isRegisterSocketOpen = new AtomicBoolean(false);
        this.numberOfSentMessages = new AtomicInteger(0);


       this.messagesReceivedFromServer = new HashMap<Long, Message>();
       this.messagesSentFromClient = new HashMap<Long, Message>();
       this.specialRequestsSentFromClient = new HashMap<Long, Message>();
        
        this.otherClients = new String();
        
        //We cannot proceed until we are registered. If we successfully register than the isRegistered flag will be set to
        //true by the current RegisterClientThread
        while(!isRegistered.get())  //sometimes throwing errors now. maybe wait longer
        {
            this.socket = new DatagramSocket(Integer.parseInt(this.port));
            this.isRegisterSocketOpen.set(true);
            this.address = InetAddress.getByName(this.hostname);
            System.out.println("Starting new registration thread.");
            new RegisterClientThread(this.socket, this.isRegistered, this.isRegisterSocketOpen, this.name, Integer.parseInt(this.port)+1, this.address, 4445).start();
            TimeUnit.SECONDS.sleep(2);
            this.socket.close();
            this.isRegisterSocketOpen.set(false);
        }

        this.address = InetAddress.getByName(this.hostname );
        this.socket = new DatagramSocket(Integer.parseInt(port)+1);
        this.buf = new byte[1024];
        this.packet = new DatagramPacket(this.buf, this.buf.length, this.address, 4445);
        
        this.rThread = new ReceiverClientThread(Integer.parseInt(this.port));
        this.rThread.start();


    }


    public  String getOtherClients() throws IOException, InterruptedException{
                //Gets the list of all other registered clients.
        //This tells us who we can message.
        String getClientsDetails[] = Message.getOtherClients(this.socket, this.name, this.packet, this.rThread.numberOfReceivedMessages.get(), 
        this.numberOfSentMessages.incrementAndGet());
        Message msgGetClients = new Message(getClientsDetails, Message.REQUEST_GET_CLIENTS, this.name, "Server");
        this.specialRequestsSentFromClient.put(msgGetClients.getCheckSum(), msgGetClients); 
        //need to add every getClients call to this to the special message list in case its not sent or data corrupted
        TimeUnit.SECONDS.sleep(1);
        return recieveMessage();
    }




    public void sendMessage(String recipient, String text) throws IOException {
            //Send and Receive Messages

            Message msgC = new Message(recipient, this.name, text);
            msgC.sendMessageC(this.socket, this.packet, this.rThread.numberOfReceivedMessages.get(), this.numberOfSentMessages.incrementAndGet());
            this.messagesSentFromClient.put(msgC.getCheckSum(), msgC);
            System.out.println("Checksum of sent msg: " + msgC.getCheckSum() + " timestamp " + msgC.getTimestamp() + " text: " + msgC.getText());
                //continue;
            this.clientHistory.update(msgC);
        }    


    public String getHistory(String otherClient){

        return this.clientHistory.toString(this.name, otherClient);

    }

    





    public String recieveMessage() throws IOException
    {

        

        String returnedMessage = "";
        String returnClients = "";
        while(!this.rThread.messagesFromServer.isEmpty())  //this is how we see all the messages we have been sent
        {
            this.messageArray = this.rThread.messagesFromServer.poll();               
            
            System.out.println("Message from server: " + this.messageArray[1]);
            returnedMessage += "Message from server: " + this.messageArray[1] + '\n';

            if (this.messageArray[1].equals("Send-MSG-S"))
            {
            
                System.out.println("Message from " + this.messageArray[4] + ":");
                System.out.println(this.messageArray[7]+"\n");
                returnedMessage += "Message from " + this.messageArray[4] + ":" + this.messageArray[7]+"\n";

                Message msgS = new Message(this.messageArray, Message.RESPONSE_SEND_MESSAGE_SERVER, this.messageArray[4], this.name);
                msgS.setRecieved("1");
                msgS.sendMessageReceiptClient(this.socket, this.messageArray[4], this.packet, this.rThread.numberOfReceivedMessages.get(), this.numberOfSentMessages.incrementAndGet());
                this.messagesReceivedFromServer.put(msgS.getCheckSum(), msgS);
                this.clientHistory.update(msgS);

            }

            else if (this.messageArray[1].equals("Send-MSG-S-SENT"))
            {
                
                Message msgSentFromThisClient = this.messagesSentFromClient.get(Long.parseLong(this.messageArray[3]));
                if ( msgSentFromThisClient != null){
                    msgSentFromThisClient.setSent(true);
                    System.out.println("Message has been sent, message timestamp " + this.messageArray[2] + " and checksum " + this.messageArray[3]);
                    returnedMessage += "Message has been sent, message timestamp " + this.messageArray[2] + " and checksum " + this.messageArray[3] + '\n';
                    //this.clientHistory.update(msgSentFromThisClient);
                }

            }


            else if (this.messageArray[1].equals("Send-MSG-S-RECEIPT"))
            {
                    Message msgSentFromThisClient = this.messagesSentFromClient.get(Long.parseLong(this.messageArray[3])); 		      
                    if (msgSentFromThisClient!= null){
                        msgSentFromThisClient.setSent(true);
                        msgSentFromThisClient.setRecieved("1");
                        System.out.println("Message " + this.messageArray[3] + " has been received by " + msgSentFromThisClient.getReciever());
                        returnedMessage  += "Message " + this.messageArray[3] + " has been received by " + msgSentFromThisClient.getReciever();
                        //this.clientHistory.update(msgSentFromThisClient);
                    }
            }

            else if(messageArray[1].equals("Get-Missing-Messages-S"))
            {
                
                
                System.out.println("Detected missing messages on client side");
               // System.out.println("Message array[4] is " + messageArray[4]);

                if(messageArray[4] != null && !messageArray[4].isEmpty() && messageArray[4] != "No checksums on server")
                {
                    List<Long> receivedChecksums = Arrays.asList(messageArray[4].split("\\s+")).stream()
                    .map(s -> Long.parseLong(s.trim())).collect(Collectors.toList()); //Checksums now in a list of longs
                    
                    List<Long> missingMessageCChecksums = messagesSentFromClient.keySet().stream().filter(c -> !receivedChecksums.contains(c))
                    .collect(Collectors.toList()); //find messages where this client was the original sender

                    for(Long checkSum : missingMessageCChecksums)
                    {
                        messagesSentFromClient.get(checkSum).resendMessageC(socket, packet, rThread.numberOfReceivedMessages.get(), 
                        numberOfSentMessages.get()); //we're not incrementing our count here, we are trying to 'catch up'.


                    }
                    List<Long> missingMessageRecChecksums = messagesReceivedFromServer.keySet().stream().filter(c -> !receivedChecksums.contains(c))
                    .collect(Collectors.toList()); //find messages where we didnt send a receipt
                    for(Long checkSum : missingMessageRecChecksums)
                    {
                        messagesReceivedFromServer.get(checkSum).sendMessageReceiptClient(socket, messagesReceivedFromServer.get(checkSum).getsenderOfOriginalMessage(), packet, 
                        rThread.numberOfReceivedMessages.get(), numberOfSentMessages.get()); //we're not incrementing our count here, we are trying to 'catch up'.
                    }

                    List<Long> missingSpecialMessageChecksums = specialRequestsSentFromClient.keySet().stream().filter(c -> !receivedChecksums.contains(c))
                    .collect(Collectors.toList()); //find special messages we didnt send
                    for(Long checkSum : missingSpecialMessageChecksums)
                    {
                        Message specMsg = specialRequestsSentFromClient.get(checkSum);
                        if(specMsg.getText().equals(Message.REQUEST_GET_CLIENTS))
                        {
                            Message.getOtherClients(socket, specMsg.getsenderOfOriginalMessage(), 
                            specMsg.getTimestamp(), specMsg.getCheckSum(), packet, rThread.numberOfReceivedMessages.get(), 
                            numberOfSentMessages.get()); //we're not incrementing our count here, we are trying to 'catch up'.
                        }
                    }
                }
                else
                {
                    for(Message msgC : messagesSentFromClient.values())
                    {
                        msgC.resendMessageC(socket, packet, rThread.numberOfReceivedMessages.get(), 
                        numberOfSentMessages.get());
                    }
                    for(Message msgRec : messagesReceivedFromServer.values())
                    {
                        msgRec.sendMessageReceiptClient(socket, msgRec.getsenderOfOriginalMessage(), packet, 
                        rThread.numberOfReceivedMessages.get(), numberOfSentMessages.get());
                    }
                    for(Message specMsg : specialRequestsSentFromClient.values())
                    {
                        if(specMsg.getText().equals(Message.REQUEST_GET_CLIENTS))
                        {
                            Message.getOtherClients(socket, specMsg.getsenderOfOriginalMessage(), 
                            specMsg.getTimestamp(), specMsg.getCheckSum(), packet, rThread.numberOfReceivedMessages.get(), 
                            numberOfSentMessages.get()); //we're not incrementing our count here, we are trying to 'catch up'.
                        }
                    }
                }
            
            }
            else if(messageArray[1].equals("List-Of-Clients"))
            {
                //returnedMessage = messageArray[3];
                returnClients = messageArray[3];
            }

            
        }
        return returnClients;
        //System.out.println(this.name +" has received: " +this.rThread.numberOfReceivedMessages.get());
        //returnedMessage += this.name +" has received: " +this.rThread.numberOfReceivedMessages.get();
    }



    




    public void quit() {

        this.socket.close();
    }


}
