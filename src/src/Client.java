import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Client {
    public static void main(String[] args) throws Exception {
        int clients = Integer.parseInt(args[0]);
        spawn(clients);

    }

    public static void spawn(int clients) throws Exception {
        Socket socket = new Socket("localhost", 32005);
        for (int i = 0; i < clients; i++) {

        }
    }




}
