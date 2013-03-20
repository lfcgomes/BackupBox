
import java.net.InetAddress;
import java.net.MulticastSocket;

/* thread que vai enviar chunks para fazer backup no receiver*/
public class Sender extends Thread {

    MulticastSocket socket = null;
    InetAddress address = null;
    int port;
    String sha = "";
        
    public Sender(MulticastSocket s, InetAddress ad, int p, String sh) {
        
        socket = s;
        address = ad;
        port = p;
        sha = sh;
    }

    public void run() {
        
        while(true){

        }
    }
}
