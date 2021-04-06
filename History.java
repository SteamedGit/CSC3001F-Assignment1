import java.io.File;  // Import the File class
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket; 
/**
 * Class for managing and updating successfully sent messages
 * between two clients.
 */
public class History {
    String client1, client2; //clients involved in the chat
    String fileName;
    Message message;
    public History(){
    }

    /**
     * Updates the chat history between two clients.  
     * Lines between chats are delimitered using \n.
     * Lines of messages are stored using the client's alias followed by a '->' along with the sent message.
     * 
     * Client1Client2 //Index0 for ID
     * Client1->Hi there! //Chat starts at Index1
     * @param m Sent message
     */
    public void update(Message m){
        message = m;
        client1 = m.getSender();
        client2 = m.getReciever();

        //Doing this so that the same pair has the same file
        //for chat updates
        if(client1.compareTo(client2) > 1){
            fileName = client1+""+client2+".txt";
        }
        else {
            fileName = client2+""+client1+".txt";
        }

        try{
            File file = new File(fileName);
            
            if (file.createNewFile()){//if new file is created, we write to that one
                FileWriter writer = new FileWriter(fileName);
                writer.write(fileName+"\n"); //Used to identify whose chat we're on - the first line in a chat. Chat starts at index 1.
                writer.close();
            }
            FileWriter writer = new FileWriter(fileName, true);
            writer.write(client1+ "->"+m.getText()+"\n");//client1 will ALWAYS be the sender, so we only need to add in stuff from their perspective.
            writer.close();
            
        } catch (IOException e){
            e.printStackTrace();
        }

    }
}