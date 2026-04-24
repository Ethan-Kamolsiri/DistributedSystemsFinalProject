import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FittingRoom {


    public static void main(String[] args) throws Exception {
        int fittingRoomNumber = Integer.parseInt(args[0]);
        int waitingChairNumber = fittingRoomNumber * 2;


        ServerSocket serverSocket = new ServerSocket(32006);

        ExecutorService threadPool = Executors.newCachedThreadPool();

        while (true) {
            Socket server = serverSocket.accept();
            threadPool.submit(() -> {
                try {
                    handleClient(server);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }


    public static void handleClient(Socket client) throws Exception {


    }

    public static void checkFittingRoom(Socket client) throws Exception {

    }

    public static int randomTime() {
        return (int) (Math.random() * 1000); // 0 to 1000 ms
    }

}
