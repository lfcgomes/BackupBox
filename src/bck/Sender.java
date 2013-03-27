package bck;

import bck.Backup;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/* thread que vai enviar chunks para fazer backup no receiver*/
public class Sender extends Thread {

    MulticastSocket socket = null;
    InetAddress address = null;
    int MC;
    int MD;
    String sha = "";
    int replication_degree;

    public Sender(InetAddress ad, int m_c, int m_d, String sh, int rd) throws IOException {

        address = ad;
        MC = m_c;
        MD = m_d;
        sha = sh;
        replication_degree = rd;
        socket = new MulticastSocket(MD);
        socket.joinGroup(address);
    }

    public void run() {
        FileOutputStream file = null;
        int m = 0;
        try {
            file = new FileOutputStream("super lindo.pdf");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Sender.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Sending: " + Backup.getMapShaFiles().get(this.sha).getName());
        int n = 0;
        HashMap<String, byte[]> file_to_send_chunks = Backup.getMapChunkFiles().get(sha);
        int total = 0;
        for(int x=0; x<file_to_send_chunks.size();x++)
            total += file_to_send_chunks.get(String.valueOf(x)).length;
        System.out.println("TAMANHO TOTAL: "+total);
        
        while (file_to_send_chunks.get(String.valueOf(n)) != null) {
            //PUTCHUNK <Version> <FileId> <ChunkNo> <ReplicationDeg><CRLF><CRLF><Body>
            String msg = "PUTCHUNK " + Backup.getVersion() + " " + this.sha + " " + n
                    + " " + replication_degree + "\n\n";
            byte[] msg_byte = msg.getBytes();
            
           
            
            byte[] final_msg = new byte[msg_byte.length + file_to_send_chunks.get(String.valueOf(n)).length];
            System.arraycopy(msg_byte, 0, final_msg, 0, msg_byte.length);
            System.arraycopy(file_to_send_chunks.get(String.valueOf(n)), 0, final_msg, msg_byte.length, file_to_send_chunks.get(String.valueOf(n)).length);
            
            DatagramPacket chunk = new DatagramPacket(final_msg, final_msg.length, this.address, this.MD);
            byte[] temp = new byte[file_to_send_chunks.get(String.valueOf(n)).length];
            System.arraycopy(final_msg, msg_byte.length, temp, 0, final_msg.length-msg_byte.length);
            try {
                file.write(temp);
            } catch (IOException ex) {
                Logger.getLogger(Sender.class.getName()).log(Level.SEVERE, null, ex);
            }
            m++;
            
            try {
                Thread.sleep(10);
                socket.send(chunk);
                Backup.getMissingChunks(sha).put(n, replication_degree);
            } catch (Exception ex) {
                Logger.getLogger(Sender.class.getName()).log(Level.SEVERE, null, ex);
            }
            n++;
        }

        try {
            file.flush();
            file.close();
        } catch (IOException ex) {
            Logger.getLogger(Sender.class.getName()).log(Level.SEVERE, null, ex);
        }
       
        
        
        
        
        
        try {
            Thread.sleep(10);
        } catch (InterruptedException ex) {
            Logger.getLogger(Sender.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.print("Tentando enviar chunks em falta... ");
        System.out.println(Backup.getMissingChunks(sha));
        Utils.flag_sending = 0;
    }
}
