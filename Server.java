import java.io.IOException; 

/**
 * Starts the server thread and creates a file for users' chat history.
 */
public class Server {
    public static void main(String[] args) throws IOException {
        new ServerThread().start();
    }
}