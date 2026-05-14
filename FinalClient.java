import java.io.*;
import java.net.Socket;

public class Client {
    public static void main(String[] args) throws Exception {
        int n = Integer.parseInt(args[0]);
        String host = args[1];

        for (int i = 1; i < n + 1; i++) {
            Socket x = new Socket(host, 32005);
            new Thread(new ClientThread(x, i)).start();


            Thread.sleep(randomTime());
        }
    }

    public static int randomTime() {
        return (int) (Math.random() * 1000); // 0 to 1000 ms
    }

}

class ClientThread implements Runnable {
    Socket xsocket;
    int num;

    public ClientThread(Socket x, int y) {
        xsocket = x;
        num = y;
    }

    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(xsocket.getInputStream())); PrintWriter out = new PrintWriter(xsocket.getOutputStream(), true)) {
            // Ask the server for a fitting room doesn't matter what is sent message just creates customer on mainserver side
            out.println("Customer request Room");
            System.out.println(in.readLine());

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("[" + num + "] " + line);
            }
        } catch (IOException e) {
            System.out.println("Server Shutdown");
        }
    }
}
