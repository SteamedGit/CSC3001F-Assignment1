import java.util.Collections; 
import java.util.List;
import java.util.ArrayList; 
/**
 * Class for managing and updating successfully sent messages
 * between two clients.
 * Each History object represents the chat history between
 * at least two clients.
 */
public class History {
    String chatID;
    Message message;
    List<List<Message>> history; //Mother arraylist storing all chat histories
    //List<Message> historyForClient; //history for a particular client
    List<String> indexID; //client ID of those concerned in the chat
    int index = -1; //index of given ID
    int numClients = 0; //number of clients belonging to this history object
    int numMessages = 0;

    /**
     * Creates a History object.
     * This object will store the all Messages sent between relative clients
     * in Message objects passed to this object.
     */
    public History(){
        history = new ArrayList<List<Message>>(); //arraylist storing arraylists
        indexID = new ArrayList<String>();  //Storing related ID for ArrayList
    }

    /**
     * Updates the chat history between two clients with given message.  
     * 
     * @param m Sent message
     */
    public void update(Message m){
        this.message = m;
        String client1 = m.getsenderOfOriginalMessage();
        String client2 = m.getReciever(); 
        
        chatID = gen(client1, client2); //generates ID for given message      
        index = indexID.indexOf(chatID); //finds where in the indexID list this chatID is stored
        //historyForClient.clear();//make sure it's empty before adding stuff into it for a new chat record

        if(index == -1){//then this ID is not in the list
            indexID.add(chatID);//this ID is added into the index array
            index = indexID.indexOf(chatID);//calculates new index
            List<Message> historyForClient = new ArrayList<Message>();
            historyForClient.add(message); 
            history.add(index, historyForClient);//adds new message into correct part of mother array
            //System.out.println("Chat history doesn't exist. New index: "+index+"\n for chatID: "+chatID);
        }
        else{//ID is in the list
            List<Message> historyForClient = new ArrayList<Message>();
            historyForClient = history.get(index); //chat history for these clients fetched
            historyForClient.add(message); //new message added to this particular hist

            //history.add(index, historyForClient);//This new list is appended onto the pre-existing chat history
            //System.out.println("Chat history does exist. Index: "+index+"\n for chatID: "+chatID);
            numMessages++;
        }
    }

    /**
     * Fetches chat history for c clients.
     * @param c Variable number of clients.
     */
    public List<Message> getHistory(String ... c){
        String[] clients;
        String allClients = "";
        int clientCount = 0; //number of clients
        for(String e: c){
            allClients = allClients + e +"\n";
            clientCount++;
        }
        clients = allClients.split("\n"); //splits into a list of clients
        chatID = gen(clients); //chat ID generated with given client names
        index = -1;

        index = indexID.indexOf(chatID); //looks for this chat ID in the index array
        List<Message> msg = new ArrayList<Message>(); //stores fetched arraylist from mother array with found index
        int count = 0; //count number of messages added

            if (history.isEmpty()){//if the arraylist is empty, then no new chat was added to it
                System.out.println("No chat history exists for these users.");
                return null;
            }
            else{//chat history does exist
                msg = history.get(index);
            }
        return msg;
        
    }

    /**
     * Returns a String representing the chat history between the clients
     * given in the parameter.
     */
    public String toString(String ... c){
        String hold = "";
        if(getHistory(c) == null){
            hold = "No chat history exists for these users.";
        }
        else{
            
            List<Message> hist = getHistory(c); //gets history for these clients
            for(Message m: hist){
                String x = "";
                if (m.getSent()){
                    x += "   -(S)";
                }
                if (m.getRecieved().equals("1")){
                    x += "-(R)";
                }
                hold = hold + m.getsenderOfOriginalMessage() +": "+m.getText()+ x + "\n\n";//adds the text into this string, separated by \n
            }
        }
        return hold;
    }

    /**
     * Generates an ID used to determine if the current ArrayList is
     * for the correct clients or not.
     * @param c ... Accepts a variable number of client names
     */
    public String gen(String ... c){
        List <String> clientList = new ArrayList<String>();
        String chatID = "";

        //loading all clients into a list
        for(String item: c){
            clientList.add(item);
            numClients ++;
        }

        Collections.sort(clientList); //Sorts client names in ascending order to match filename
        for(String item: clientList){
            chatID = ""+ chatID + item;
        }

        return chatID;
    }

    public int getNumClients(){
        return numClients;
    }
}