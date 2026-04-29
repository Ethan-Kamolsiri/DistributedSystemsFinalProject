import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Semaphore;

public class FittingRoom {
    private static int fittingRoomNumber;
    private static int waitingChairNumber;
    private static Semaphore fittingRoomSemaphore;
    private static Semaphore waitingChairSemaphore;
    private static Inet4Address host;

    public static void main(String[] args) throws Exception {
        fittingRoomNumber = Integer.parseInt(args[0]);
        int port = Integer.parseInt(args[1]);
        waitingChairNumber = fittingRoomNumber * 2;

        fittingRoomSemaphore = new Semaphore(fittingRoomNumber, true);
        waitingChairSemaphore = new Semaphore(waitingChairNumber, true);


        FittingRoom fittingRoom = new FittingRoom();
        try(ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Waiting for connection...");
            System.out.println("Fitting Room's: " + fittingRoomNumber);
            System.out.println("Waiting Chair's: " + waitingChairNumber);

            while(true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> {
                    try {
                        fittingRoom.handleClient(socket);
                    } catch (Exception e) {
                        System.out.println("Error starting customer thread");
                    }
                }).start();
            }
        }catch(Exception e) {
            System.out.println("Program terminated");
        }

    }


    public void handleClient(Socket socket) throws Exception {
        Socket centralServer = socket;
        BufferedReader in = new BufferedReader(new InputStreamReader(centralServer.getInputStream()));
        PrintWriter out = new PrintWriter(centralServer.getOutputStream(), true);

        String line = in.readLine();
        if (line == null) return;
        int customerID = Integer.parseInt(line);
        findspace(customerID, out);
        send(out,"Customer " + customerID + "has been dealt with.");



    }

    public static void findspace(int customerID, PrintWriter out) throws Exception {
        System.out.println("Finding space for customer: " + customerID + "On server IP:" + host.getHostAddress());
       // fitting room has room
        if(fittingRoomSemaphore.tryAcquire()) {
            useFittingRoom(customerID, out);
            return;
        }

        // fitting room is full and no chairs avaliable
        if(!waitingChairSemaphore.tryAcquire()) {
            send(out,"Customer: " + customerID + " Left Frustrated");
            System.out.println("Customer: " + customerID + " Left Frustrated");
            return;
        }

        send(out,"Customer " + customerID + "is now waiting for fitting room");
        System.out.println("Customer: " + customerID + " is now waiting for fitting room");
        fittingRoomSemaphore.acquire();
        send(out,"Customer " + customerID + "has left waiting chair");
        System.out.println("Customer: " + customerID + " has left waiting chair");
        waitingChairSemaphore.release();
        useFittingRoom(customerID, out);





    }
    public static void useFittingRoom(int customerID, PrintWriter out) throws Exception {
        send(out,"Customer: " + customerID + " has entered fitting room");
        System.out.println("Customer: " + customerID + " has entered fitting room");
        Thread.sleep(randomTime());
        fittingRoomSemaphore.release();
        send(out,"Customer: " + customerID + " has left fitting room");
        System.out.println("Customer: " + customerID + " has left fitting room");


    }

    public static void send(PrintWriter out, String message) throws Exception {
        out.println(message);
    }

    public static int randomTime() {
        return (int) (Math.random() * 1000); // 0 to 1000 ms
    }

}

