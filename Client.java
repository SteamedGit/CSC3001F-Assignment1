import java.io.*;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
        AtomicInteger numberOfSentMessages = new AtomicInteger(0);
        History history = new History();
        String thisClientString ="";


       Map<Long, Message> messagesReceivedFromServer = new HashMap<Long, Message>();
       Map<Long, Message> messagesSentFromClient = new HashMap<Long, Message>();
       Map<Long, Message> specialRequestsSentFromClient = new HashMap<Long, Message>();
        
        String otherClients = new String();

        if (args.length != 3) {
                System.out.println("Usage: java Client <client port> <client name> <hostname>");
                return;
        }
        
        //We cannot proceed until we are registered. If we successfully register than the isRegistered flag will be set to
        //true by the current RegisterClientThread
        while(!isRegistered.get())  //sometimes throwing errors now. maybe wait longer
        {
            DatagramSocket socket = new DatagramSocket(Integer.parseInt(args[0]));
            isRegisterSocketOpen.set(true);
            InetAddress address = InetAddress.getByName(args[2]);
            System.out.println("Starting new registration thread.");
            new RegisterClientThread(socket, isRegistered, isRegisterSocketOpen, args[1], Integer.parseInt(args[0])+1, address, 4445).start();
            TimeUnit.SECONDS.sleep(2);
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
        //ADDED
        thisClientString = args[1]; //name of this client



        
        
        //Gets the list of all other registered clients.
        //This tells us who we can message.
        String getClientsDetails[] = Message.getOtherClients(socket, args[1], packet, rThread.numberOfReceivedMessages.get(), 
        numberOfSentMessages.incrementAndGet());
        Message msgGetClients = new Message(getClientsDetails, Message.REQUEST_GET_CLIENTS, args[1], "Server");
        specialRequestsSentFromClient.put(msgGetClients.getCheckSum(), msgGetClients); 
        //need to add every getClients call to this to the special message list in case its not sent or data corrupted
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
            System.out.println("Do you want to send a message(S), view history(H) or quit?(Q)");
            String sRQuit = input.nextLine();
            System.out.println("");
            if(sRQuit.toLowerCase().equals("s"))
            {
                
                System.out.println("Who do you want to send a message to?");
                String recipient = input.nextLine();
                System.out.println("What is the message?");
                String text = input.nextLine();
                System.out.println("");


                Message msgC = new Message(recipient, args[1], text);
                msgC.sendMessageC(socket, packet, rThread.numberOfReceivedMessages.get(), numberOfSentMessages.incrementAndGet());
                //TODO ADD HISt
                history.update(msgC);
                messagesSentFromClient.put(msgC.getCheckSum(), msgC);


                
                //continue;
            }
            if(sRQuit.toLowerCase().equals("history") || sRQuit.toLowerCase().equals("h")){
                System.out.println("Whose chat history would you like to view?");
                String clientName = input.nextLine();
                System.out.println(history.toString(thisClientString, clientName));
                
            }
            if(sRQuit.toLowerCase().equals("quit") || sRQuit.toLowerCase().equals("q") )
            {
                break;
            }
            while(!rThread.messagesFromServer.isEmpty())  //this is how we see all the messages we have been sent
            {
                messageArray = rThread.messagesFromServer.poll();               
                
                System.out.println("Message from server: " + messageArray[1]);

                if (messageArray[1].equals("Send-MSG-S"))
                {
                
                    System.out.println("Message from " + messageArray[4] + ":");
                    System.out.println(messageArray[7]+"\n");
                    Message msgS = new Message(messageArray, Message.RESPONSE_SEND_MESSAGE_SERVER, messageArray[4], args[1]);
                    msgS.setRecieved("1");
                    msgS.sendMessageReceiptClient(socket, messageArray[4], packet, rThread.numberOfReceivedMessages.get(), numberOfSentMessages.incrementAndGet());
                    messagesReceivedFromServer.put(msgS.getCheckSum(), msgS);
                }

                else if (messageArray[1].equals("Send-MSG-S-SENT"))
                {
                    
                    Message msgSentFromThisClient = messagesSentFromClient.get(Long.parseLong(messageArray[3]));
                    if ( msgSentFromThisClient != null){
                        msgSentFromThisClient.setSent(true);
                        System.out.println("Message has been sent, message timestamp " + messageArray[2] + " and checksum " + messageArray[3]);
                    }

                }


                else if (messageArray[1].equals("Send-MSG-S-RECEIPT"))
                {
                        Message msgSentFromThisClient = messagesSentFromClient.get(Long.parseLong(messageArray[3])); 		      
                        if (msgSentFromThisClient!= null){
                            msgSentFromThisClient.setRecieved("1");
                            System.out.println("Message " + messageArray[3] + " has been received by " + msgSentFromThisClient.getReciever());
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
                            messagesSentFromClient.get(checkSum).sendMessageC(socket, packet, rThread.numberOfReceivedMessages.get(), 
                            numberOfSentMessages.get()); //we're not incrementing our count here, we are trying to 'catch up'.
                        }
                        List<Long> missingMessageRecChecksums = messagesReceivedFromServer.keySet().stream().filter(c -> !receivedChecksums.contains(c))
                        .collect(Collectors.toList()); //find messages where we didnt send a receipt
                        for(Long checkSum : missingMessageRecChecksums)
                        {
                            messagesReceivedFromServer.get(checkSum).sendMessageReceiptClient(socket, messagesReceivedFromServer.get(checkSum).getsenderOfOriginalMessage(), packet, 
                            rThread.numberOfReceivedMessages.get(), numberOfSentMessages.get()); //we're not incrementing our count here, we are trying to 'catch up'.
                        }
    
                        //TODO: add search the special messages...
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
                            msgC.sendMessageC(socket, packet, rThread.numberOfReceivedMessages.get(), 
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
                    System.out.println("Available Clients: " + messageArray[3]);
                }



            }
            System.out.println(args[1] +" has received: " +rThread.numberOfReceivedMessages.get());
        }
      
      

        input.close();
        socket.close();
    }



}
