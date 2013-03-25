
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

//classe para ler as mensagens enviadas
public class Message extends Thread {

    MulticastSocket socket = null;
    InetAddress address = null;
    int port;

    public Message(MulticastSocket s, InetAddress ad, int p) {
        socket = s;
        address = ad;
        port = p;
    }

    public void run() {
        while (true) {
            byte[] receive_buffer = new byte[1024];
				
            DatagramPacket receive_packet = new DatagramPacket(receive_buffer, receive_buffer.length);
            String local = "";
            String ip = "";
            
            try {   
                socket.receive(receive_packet);
                local = InetAddress.getLocalHost().toString();
                ip = local.substring(local.indexOf("/"));                         
            } catch (IOException ex) {}
            
            String data = new String(receive_packet.getData(), 0, receive_packet.getLength());
            String[] data_parsed = data.split(" ");
            
            if (!ip.equals("") && !receive_packet.getAddress().getHostName().contains(local)) {
                System.out.println("Received "+data_parsed[0]);
            }
        }
    }
}
