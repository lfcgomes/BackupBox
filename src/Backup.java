//class main do Backup-Box, com a funcionalidade principal de controlar enviar/receber

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;

public class Backup {

	public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
		
		int port = 7777;
		String ip_address = "224.0.2.10";
		MulticastSocket socket = new MulticastSocket(port);
        InetAddress address = InetAddress.getByName(ip_address);
        socket.joinGroup(address);
   
       	Utils.readFromFile("configuration.txt");
	}
}
