import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MainServer {
    static final int clientPort = 32005;
    static final int FittingRoomPort = 32006;

    private static final AtomicInteger customerCounter = new AtomicInteger(0);


    static List<FRControl> fittingRoomsConnected = Collections.synchronizedList(new ArrayList<>());
    static List<ClientHandler> clientsConnected = Collections.synchronizedList(new ArrayList<>());

    static Map<Integer, ClientHandler> customerOwner = new ConcurrentHashMap<>();


    public static void main(String[] args) {
        new Thread(() -> {
            acceptFittingRooms();
        }).start();
        new Thread(() -> {
            acceptClients();
        }).start();


    }

    static void acceptFittingRooms() {
        try (ServerSocket serverSocket = new ServerSocket(FittingRoomPort)) {
            System.out.println("Listening for Fitting Rooms on port " + FittingRoomPort);
            while (true) {
                Socket socket = serverSocket.accept();
                FRControl fr = new FRControl(socket);
                fittingRoomsConnected.add(fr);
                new Thread(fr).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void acceptClients() {
        try (ServerSocket serversocket = new ServerSocket(clientPort)) {
            System.out.println("Listening for Clients on port " + clientPort);
            while (true) {
                Socket socket = serversocket.accept();
                ClientHandler clientHandler = new ClientHandler(socket);
                clientsConnected.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void assignCustomer(ClientHandler client) {
        int id = customerCounter.incrementAndGet();
        customerOwner.put(id, client);
        FRControl fittingroom = fittingRoomsConnected.getFirst();
        fittingroom.send(String.valueOf(id));
    }


    static void RouteMessage(String action, int id) {
        ClientHandler owner = customerOwner.get(id);
        if (owner != null) {
            owner.sendMessage(action);
        }

    }



    static class ClientHandler implements Runnable {
        Socket socket;
        BufferedReader in;
        PrintWriter out;

        ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
        }

        synchronized void sendMessage(String message){
            out.println(message);
        }

        public void run() {
            try{
                sendMessage("Connected to Main Server");
                String line;
                while((line = in.readLine()) != null){
                    assignCustomer(this);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            clientsConnected.remove(this);
            try {
                socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class FRControl implements Runnable {
        String customerID = null;
        Socket socket;
        BufferedReader in;
        PrintWriter out;
        boolean running = true;

        FRControl(Socket socket) throws IOException {
            this.socket = socket;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
        }

        void send(String message) {
            out.println(message);
        }

        public void run(){
            try {
                String line;

                while ((line = in.readLine()) != null) {
                    String[] tokens = line.split(" ");
                    customerID = tokens[1];

                    String action = classify(line);
                    if(action != null){
                        RouteMessage(action, Integer.parseInt(customerID));
                    }



                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String classify(String line) {
            line = line.toLowerCase();
            if (line.contains("entered")) return "ENTERED";
            if (line.contains("frustrated")) return "LEFT_FRUSTRATED";
            if (line.contains("left waiting chair")) return "LEFT_CHAIR";
            if (line.contains("is now waiting")) return "WAITING";
            if (line.contains("left fitting room")) return "lEFT_FITTINGROOM";
            return null;
        }


    }

}
