import java.io.File;  // Import the File class
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Collections; 
import java.util.List;
import java.util.Scanner;
import java.util.ArrayList; 
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

        String fileName = gen(client1, client2);

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

    /**
     * Fetches chat history for c clients.
     * @param c Variable number of clients.
     */
    public void fetch(String ... c){
        String fileName = gen(c);
        boolean exists = false;
        File file = new File(fileName);
        try{
            if (file.createNewFile()){//if a new file was created, that means there's no history
                System.out.println("No chat history exists for these users.");
                file.delete(); //deletes file that was made for the testing
            }
            else{//if a file already exists, we know we can read from it
                Scanner scFile = new Scanner(file);
                //TODO: read in textfile, change "->" into ":" and print to terminal
                while(scFile.hasNextLine()){
                    String line = scFile.nextLine();
                    String[] msg = line.split("->");
                    System.out.println(msg[0] + ": " +msg[1]+"\n"); //Changes format from C1->Hi to C1: Hi
                }
            }

        }
        catch(IOException e){
            e.printStackTrace();
        }

        
    }

    /**
     * Generates a filename.
     * @param c ... Accepts a variable number of client names
     */
    public String gen(String ... c){
        List <String> clientList = new ArrayList<String>();
        int count = 0;
        String fileName = "";

        //loading all clients into a list
        for(String item: c){
            clientList.add(item);
            count ++;
        }

        Collections.sort(clientList); //Sorts client names in ascending order to match filename
        for(String item: clientList){
            fileName = ""+ fileName + item;
        }

        fileName = fileName+".txt";
        return fileName;
    }
}