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

    // thread safe to ensure that every customer gets a unique id
    private static final AtomicInteger customerCounter = new AtomicInteger(0);
    private static final AtomicInteger roundRobinIndex = new AtomicInteger(0);

    // creates list of clients and fitting rooms connected thread safe
    static List<FRControl> fittingRoomsConnected = Collections.synchronizedList(new ArrayList<>());
    static List<ClientHandler> clientsConnected = Collections.synchronizedList(new ArrayList<>());


    // stores each active client ID and creates a tuple with the client it originated from
    static Map<Integer, ClientHandler> customerOwner = new ConcurrentHashMap<>();
    static Map<Integer, FRControl> customerInFittingRoom = new ConcurrentHashMap<>();




    public static void main(String[] args) {
        new Thread(() -> {
            acceptFittingRooms();
        }).start();
        new Thread(() -> {
            acceptClients();
        }).start();


    }

    // loops as program runs and listens for fittingrooms connecting to main server then creates a fitting room controller to handle connections between main server and a fittingroom
    static void acceptFittingRooms() {
        try (ServerSocket serverSocket = new ServerSocket(FittingRoomPort)) {
            System.out.println("Listening for Fitting Rooms on port " + FittingRoomPort);
            while (true) {
                Socket socket = serverSocket.accept();
                FRControl fr = new FRControl(socket);
                fittingRoomsConnected.add(fr);
                System.out.print("Accepted Fitting Room: ");
                System.out.println(socket.getRemoteSocketAddress());
                new Thread(fr).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // loops as program runs and listens for clients connecting to main server then creates a client handler to handle all connections between client and main server
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

    // assigns customerID for every customer to ensure that we don't create duplicate customerID's
    static void assignCustomer(ClientHandler client) {
        int id = customerCounter.incrementAndGet();
        customerOwner.put(id, client);
        RouteMessage("Customer Arrives", id);

        if (fittingRoomsConnected.isEmpty()) {
            RouteMessage("Customer: " + id + " left frustrated (no fitting rooms)", id);
            customerOwner.remove(id);
            return;
        }

        int index = Math.floorMod(roundRobinIndex.getAndIncrement(),
                                  fittingRoomsConnected.size());
        FRControl fittingroom = fittingRoomsConnected.get(index);
        fittingroom.activeCustomers.add(id);
        customerInFittingRoom.put(id, fittingroom);
        fittingroom.send(String.valueOf(id));
    }

    // Reroutes customers from a dead FR onto a surviving one. If no FRs left, they go frustrated.
    static void rerouteCustomers(FRControl deadFR) {
        Set<Integer> orphans = new HashSet<>(deadFR.activeCustomers);
        deadFR.activeCustomers.clear();

        if (orphans.isEmpty()) return;
        System.out.println("FR died with " + orphans.size() + " customers in flight");

        for (Integer customerID : orphans) {
            ClientHandler owner = customerOwner.get(customerID);
            if (owner != null) {
                owner.sendMessage("Customer: " + customerID
                    + " is being rerouted (their fitting room crashed)");
            }

            if (fittingRoomsConnected.isEmpty()) {
                if (owner != null) {
                    owner.sendMessage("Customer: " + customerID
                        + " left frustrated (no fitting rooms available)");
                }
                customerOwner.remove(customerID);
                customerInFittingRoom.remove(customerID);
                continue;
            }

            int index = Math.floorMod(roundRobinIndex.getAndIncrement(),
                                      fittingRoomsConnected.size());
            FRControl newFR = fittingRoomsConnected.get(index);
            newFR.activeCustomers.add(customerID);
            customerInFittingRoom.put(customerID, newFR);
            newFR.send(String.valueOf(customerID));
        }
    }

    //For future use with multiple clients connecting will give client that customer originated from
    static void RouteMessage(String action, int id) {
        ClientHandler owner = customerOwner.get(id);
        if (owner != null) {
            owner.sendMessage(action);
        }

    }


    // Does everything for the client and main server connection
    static class ClientHandler implements Runnable {
        Socket socket;
        BufferedReader in;
        PrintWriter out;

        ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
        }

        synchronized void sendMessage(String message) {
            out.println(message);
        }

        public void run() {
            try {
                String line;
                while ((line = in.readLine()) != null) {
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

    // handles the fittingroom and mainserver connection
    static class FRControl implements Runnable {
        String customerID = null;
        Socket socket;
        BufferedReader in;
        PrintWriter out;
        boolean running = true;

        // Tracks which customer IDs this FR currently owns, so we can reroute them on failure.
        final Set<Integer> activeCustomers = ConcurrentHashMap.newKeySet();

        FRControl(Socket socket) throws IOException {
            this.socket = socket;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
        }

        void send(String message) {
            out.println(message);
        }

        public void run() {
            try {
                String line;

                while ((line = in.readLine()) != null) {
                    String[] tokens = line.split(" ");
                    if (tokens.length < 2) continue;
                    customerID = tokens[1];

                    String action = classify(line);
                    if (action != null) {
                        try {
                            int id = Integer.parseInt(customerID);
                            RouteMessage(action, id);

                      
                            if ("LEFT_FITTINGROOM".equals(action) || "LEFT_FRUSTRATED".equals(action)) {
                                activeCustomers.remove(id);
                                customerInFittingRoom.remove(id);
                                customerOwner.remove(id);
                            }
                        } catch (NumberFormatException nfe) {
                            System.out.println("Couldn't parse customer ID from: " + line);
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Fitting room: " + socket.getRemoteSocketAddress() + " failed");
            } finally {
                running = false;
                fittingRoomsConnected.remove(this);
                try { socket.close(); } catch (IOException ignored) {}
                rerouteCustomers(this);
            }
        }

        String classify(String line) {
            line = line.toLowerCase();
            if (line.contains("entered")) return "ENTERED";
            if (line.contains("frustrated")) return "LEFT_FRUSTRATED";
            if (line.contains("left waiting chair")) return "LEFT_CHAIR";
            if (line.contains("is now waiting")) return "WAITING";
            if (line.contains("left fitting room")) return "LEFT_FITTINGROOM";
            return null;
        }
    }
}
