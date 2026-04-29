public class MainServer {

    static int fittingRooms ;
    static int chairs ;
    static int waiting = 0 ;
    static int changing = 0 ;

    static List<FittingRoomServerHandler> frServers = new ArrayList<>();
    static List<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        fittingRooms = Integer.parseInt(args[0]);
        chairs = fittingRooms * 2 ;

        System.out.println("Server started");
        System.out.println("Fitting Rooms: " + fittingRooms);
        System.out.println("Chairs: " + chairs);

        Runnable r1 = new Runnable() {
            public void run() {
                acceptFittingServers();
            }
        };

        Runnable r2 = new Runnable() {
            public void run() {
                acceptClients();
            }
        };




        Thread t1 = new Thread(r1);
        Thread t2 = new Thread(r2);

        t1.start();
        t2.start();
    }




    static void acceptFittingServers() {
        try {
            ServerSocket ss = new ServerSocket(32006) ;
            System.out.println("Waiting for fitting room servers");
            while (true) {
                Socket s = ss.accept();
                FittingRoomServerHandler handler = new FittingRoomServerHandler(s);
                frServers.add(handler);
                Thread t = new Thread(handler);
                t.start();
                System.out.println("Fitting room server connected: " + s.getInetAddress().getHostAddress());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    static void acceptClients() {
        try {
            ServerSocket ss = new ServerSocket(32005);
            System.out.println("Waiting for clients on port 32005");
            while (true) {
                Socket s = ss.accept();
                ClientHandler handler = new ClientHandler(s);
                clients.add(handler);
                Thread t = new Thread(handler);
                t.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static String tryGetRoom() {
        for (int i = 0; i < frServers.size(); i++) {
            FittingRoomServerHandler frsh = frServers.get(i);

            if (frsh.alive && frsh.occupied < frsh.totalRooms) {
                String response = frsh.sendRequest("ACQUIRE");
                return response;
            }

        }
        return "WAIT";
    }

    static boolean tryGetChair() {
        if (waiting < chairs) {
            waiting++;
            return true;
        }
        return false;
    }

    static void customerGotRoom() {
        if (waiting > 0){
		waiting--;
		}
        changing++ ;
        sendStatusToAll();
    }

    static void customerLeftRoom() {
        if (changing > 0){
		changing--;
	}
        sendStatusToAll();
    }

    static void sendStatusToAll() {
        String message = "STATUS changing = " + changing + " waiting = " + waiting;
        for (int i = 0; i < clients.size(); i++) {
            clients.get(i).send(message);
        }
        System.out.println("Broadcast: " + message);
    }
}
