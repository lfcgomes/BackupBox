package bck;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/* thread que vai receber chunks para backup enviadas pelo sender*/
public class Receiver extends Thread {

    InetAddress address = null;
    int MDB;
    int MC;
    MulticastSocket socket = null;
    MulticastSocket socket_control = null;

    public Receiver(MulticastSocket socket_mc, InetAddress ad, int m_c, int m_d_b) throws IOException {
        address = ad;
        MDB = m_d_b;
        MC = m_c;
        socket_control = socket_mc;

        socket = new MulticastSocket(MDB);
        socket.joinGroup(ad);
    }

    @Override
    public void run() {
        while (!Utils.should_stop) {

            byte[] receive_buffer = new byte[65000];
            String local = "";

            DatagramPacket receive_packet = new DatagramPacket(receive_buffer, receive_buffer.length);
            try {
                //socket.setSoTimeout(1000);
                socket.receive(receive_packet);
                local = InetAddress.getLocalHost().getHostName();

            } catch (IOException ex) {
            }

            if (!local.equals("") && !receive_packet.getAddress().getHostName().contains(local)) {

                //TODO: ver o tamanho de "data" até ao index de "\n\n", que é o HEADER
                //pode ser que seja possível ver o tamanho do header assim, e o resto é o body
                String data_total = new String(receive_packet.getData(), 0, receive_packet.getLength());

                String HEADER = data_total.split("\n\n")[0];
                int inicio_body = HEADER.getBytes().length + 2;

                String[] data_parsed = HEADER.split(" ");

                String version = data_parsed[1];
                String fileID = data_parsed[2];
                String chunkNO = data_parsed[3];
                String replication_degree = data_parsed[4];
                
                if(Backup.getStoredFileMinimumDegree().get(fileID) == null){
                     Backup.getStoredFileMinimumDegree().put(fileID, replication_degree);
                }
                
                byte[] info = new byte[64000];

                System.arraycopy(receive_buffer, inicio_body, info, 0, 64000);

                //verifica se é o último chunk para o caso de todos serem de 64K
                if (!isEmptyChunk(info)) {

                    //verifica se tem espaço suficiente em disco para guardar
                    if (Backup.getDiskSpace() - info.length > 0) {
                        
                        //verifica se o chunk que está a tentar receber é da mesma versão do sistema
                        if (version.equalsIgnoreCase(Backup.getVersion())) {

                            //verifica se ja existe uma key criada no mapa para aquele ficheiro
                            if (!Backup.getStoredChunksMap().containsKey(fileID)) {
                                Backup.initiateStoredChunk(fileID);
                            }
                            //verifica se já armazenou esse chunk
                            if (!Backup.existChunk(fileID, chunkNO)) {

                                FileOutputStream f;
                                try {
                                    f = new FileOutputStream(fileID + "_" + chunkNO);
                                    f.write(info);
                                    f.flush();
                                    f.close();
                                    Backup.updateDiskSpace(info.length);
  
                                } catch (Exception ex) {
                                    Logger.getLogger(Receiver.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                
                                //adiciona o chunk ao hashmap que contém o número dos chunks armazenados desse ficheiro
                                Backup.getStoredChunks(fileID).add(chunkNO);

                                //STORED <Version> <FileId> <ChunkNo><CRLF><CRLF>

                                String msg = "STORED " + version + " " + fileID + " " + chunkNO + "\n\n";
                                DatagramPacket chunk_stored = new DatagramPacket(msg.getBytes(), msg.length(), this.address, this.MC);

                                Random randomGenerator = new Random();
                                int randomDelay = randomGenerator.nextInt(400);

                                try {
                                    //Thread.sleep(randomDelay);
                                    socket_control.send(chunk_stored);
                                } catch (Exception ex) {
                                    Logger.getLogger(Receiver.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        }

                    }
                }
            }
        }
    }

    public boolean isEmptyChunk(byte[] chunk) {

        for (int x = 0; x < chunk.length; x++) {
            if (chunk[x] != 0) {
                return false;
            }
        }
        return true;
    }
}
