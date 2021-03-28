import java.io.*;

/**
 * Starts the server thread.
 */
public class Server {
    public static void main(String[] args) throws IOException {
        new ServerThread().start();
    }
}