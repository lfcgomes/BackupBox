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
                    //int chunkNO = Integer.parseInt(data_parsed[3]);
                    
                    HashMap<String, ArrayList<Integer>> version_with_chunk = new HashMap<String, ArrayList<Integer>>();
                    version_with_chunk = Backup.getStoredMessagesReceived(fileID);
                    
                    //verificar se já recebemos algum STORE para aquele ficheiro
                    if(version_with_chunk == null){
                        ArrayList<Integer> aux_chunks = new ArrayList<Integer>();
                        aux_chunks.add(chunkNO);
                        version_with_chunk = new HashMap<String, ArrayList<Integer>>();
                        version_with_chunk.put(version, aux_chunks);
                        Backup.getStoredMessagesReceived().put(fileID, version_with_chunk);
                    }
                    
                    else{

                        ArrayList<Integer> chunks_existent = new ArrayList<Integer>();
                        
                        //caso já haja, vamos verificar se para aquela versão já foi guardado aquele chunk
                        chunks_existent = version_with_chunk.get(version);
                        
                    
                        //se existir aquele chunk, adicionamos
                        if(!chunks_existent.contains(chunkNO)){
                            System.out.println("Adicionei: "+chunkNO);
                            chunks_existent.add(chunkNO);
                            Backup.getStoredMessagesReceived(fileID).put(version, chunks_existent);
                        }
                        //caso contenha, é porque já houve algum store para aquele ficheiro
                        else{
                            System.out.println("Já existia um store");
                        }
                    }
                }
            }
        }
    }
}
