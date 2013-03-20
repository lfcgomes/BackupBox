
import java.io.File;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.HashMap;

/* thread que vai enviar chunks para fazer backup no receiver*/
public class Sender extends Thread {

    MulticastSocket socket = null;
    InetAddress address = null;
    int MC;
    int MD;
    String sha = "";

    public Sender(MulticastSocket s, InetAddress ad, int m_c, int m_d, String sh) {

        socket = s;
        address = ad;
        MC = m_c;
        MD = m_d;
        sha = sh;
    }

    public void run() {
        System.out.println("Sending: " + Backup.getMapShaFiles().get(this.sha).getName());
        while (true) {
            File file_to_send = Backup.getMapShaFiles().get(this.sha);
            HashMap<Integer, byte[]> file_to_send_chunks = Backup.getMapChunkFiles().get(sha);
        }
    }
}
