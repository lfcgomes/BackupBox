package bck;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.HashMap;

//classe para ler as mensagens enviadas
public class Message extends Thread {

    MulticastSocket socket = null;
    InetAddress address = null;
    int port;
    int replication_degree;

    public Message(MulticastSocket s, InetAddress ad, int p, int rd) {
        socket = s;
        address = ad;
        port = p;
        replication_degree = rd;
    }

    public void run() {
        while (true) {
            byte[] receive_buffer = new byte[1024];
				
            DatagramPacket receive_packet = new DatagramPacket(receive_buffer, receive_buffer.length);
            String local = "";
            String ip = "";
            
            try {   
                socket.receive(receive_packet);
                local = InetAddress.getLocalHost().getHostName();                         
            } catch (IOException ex) {}
            
            String data = new String(receive_packet.getData(), 0, receive_packet.getLength());
            String[] data_parsed = data.split(" ");
            
            if (!local.equals("") && !receive_packet.getAddress().getHostName().contains(local)) {
                
                //Temos de saber quantas mensagens STORED já recebemos, para saber se ainda temos de guardar
                if(data_parsed[0].equalsIgnoreCase("STORED")){
                    
                    String version = data_parsed[1];
                    String fileID = data_parsed[2];
                    int chunkNO = Integer.parseInt(data_parsed[3].substring(0, data_parsed[3].indexOf("\n")));
                    
                    HashMap<Integer, Integer> missing = new HashMap<Integer, Integer>();
                    missing = Backup.getMissingChunks(fileID);
                    
                    //não vai acontecer
                    if(missing.get(chunkNO) == null){
                        missing.put(chunkNO,replication_degree);
                    }
                    else{//vai diminuir o replication degree obrigatorio para o chunk
                        int old_rep = missing.get(chunkNO);
                        
                        if((old_rep-1) ==0)
                            missing.remove(chunkNO);
                        else
                            missing.put(chunkNO,old_rep-1);
                    }
                }
            }
        }
    }
}
