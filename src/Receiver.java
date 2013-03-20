
import java.net.InetAddress;
import java.net.MulticastSocket;

/* thread que vai receber chunks para backup enviadas pelo sender*/

public class Receiver extends Thread {

    InetAddress address = null;
    int MD;
    MulticastSocket socket = null;
    
    public Receiver(InetAddress ad, int m_d) {
        address = ad;
        MD = m_d;
    }

    public void run() {
        while (true) {
        }
    }
}
