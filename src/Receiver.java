
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/* thread que vai receber chunks para backup enviadas pelo sender*/

public class Receiver extends Thread {

    InetAddress address = null;
    int MD;
    MulticastSocket socket = null;
    
    public Receiver(InetAddress ad, int m_c, int m_d) throws IOException {
        address = ad;
        MD = m_d;
        
        socket = new MulticastSocket(MD);
        socket.joinGroup(ad);
    }

    public void run() {
        while (true) {
            
            byte[] receive_buffer = new byte[2048];
            String local="";
            String ip="";
            System.out.println("A receber");
            DatagramPacket receive_packet = new DatagramPacket(receive_buffer, receive_buffer.length);
            try {
                //socket.setSoTimeout(1000);
                socket.receive(receive_packet);
                local = InetAddress.getLocalHost().toString();
                ip = local.substring(local.indexOf("/"));
            } catch (IOException ex) {
                
            } 
  
            String data = new String(receive_packet.getData(),0, receive_packet.getLength());
            String[] data_parsed = data.split(" ");
   
            
            
           
            FileOutputStream fich = null;
            try {
                fich = new FileOutputStream(data_parsed[2]);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(Receiver.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                fich.write(data_parsed[6].getBytes());
            } catch (IOException ex) {
                Logger.getLogger(Receiver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
