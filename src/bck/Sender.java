package bck;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
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

    @Override
    @SuppressWarnings("SleepWhileInLoop")
    public void run() {
        FileOutputStream file = null;

        System.out.println("Sending...");
        int n = 0;
        HashMap<String, byte[]> file_to_send_chunks = Backup.getMapChunkFiles().get(sha);

        while (file_to_send_chunks.get(String.valueOf(n)) != null) {
            //PUTCHUNK <Version> <FileId> <ChunkNo> <ReplicationDeg><CRLF><CRLF><Body>
            String msg = "PUTCHUNK " + Backup.getVersion() + " " + this.sha + " " + n
                    + " " + replication_degree + "\n\n";
            byte[] msg_byte = msg.getBytes();
            byte[] final_msg = new byte[msg_byte.length + file_to_send_chunks.get(String.valueOf(n)).length];
            System.arraycopy(msg_byte, 0, final_msg, 0, msg_byte.length);
            System.arraycopy(file_to_send_chunks.get(String.valueOf(n)), 0, final_msg, msg_byte.length, file_to_send_chunks.get(String.valueOf(n)).length);

            DatagramPacket chunk = new DatagramPacket(final_msg, final_msg.length, this.address, this.MD);

            try {
                Thread.sleep(100);
                socket.send(chunk);
                Backup.getMissingChunks(sha).put(n, replication_degree);
            } catch (Exception ex) {
                Logger.getLogger(Sender.class.getName()).log(Level.SEVERE, null, ex);
            }
            n++;
        }

        try {
            Thread.sleep(10);
        } catch (InterruptedException ex) {
            Logger.getLogger(Sender.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (!Backup.getMissingChunks(sha).isEmpty());
        System.out.print("Tentando enviar chunks em falta... ");
        Utils.flag_sending = 0;
    }
}
