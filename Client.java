import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) throws Exception{

        Scanner s = new Scanner(System.in);
            try{

                for(int i = 0; i > Integer.parseInt(args[0]); i++){

                    Socket x = new Socket("localhost", 32005);
                    System.out.println("Socket " + i + " connected!");
                    new Thread(new clientThread(x, i)).start();
                    
                }
            } catch(Exception e){
                e.printStackTrace();
            }
     }
}

class clientThread implements Runnable{

    Socket xsoc;
    int num;

    public clientThread(Socket x, int y){
        xsoc = x;
        num = y;
    }

    public void run(){
        try {
        BufferedReader br = new BufferedReader(new InputStreamReader(xsoc.getInputStream()));

        while (true) { 
            System.out.println(br.readLine());
            }
        } catch (Exception e) {
        }
        
    }

}