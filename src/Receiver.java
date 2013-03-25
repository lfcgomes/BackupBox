
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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
    int MD;
    int MC;
    MulticastSocket socket = null;
    MulticastSocket socket_control = null;
    
    public Receiver(MulticastSocket socket_mc, InetAddress ad, int m_c, int m_d) throws IOException {
        address = ad;
        MD = m_d;
        MC = m_c;
        socket_control = socket_mc;
        
        socket = new MulticastSocket(MD);
        socket.joinGroup(ad);
    }

    @Override
    public void run() {
        while (true) {

            byte[] receive_buffer = new byte[2048];
            String local = "";
            String ip = "";
            
            DatagramPacket receive_packet = new DatagramPacket(receive_buffer, receive_buffer.length);
            try {
                //socket.setSoTimeout(1000);
                socket.receive(receive_packet);
                local = InetAddress.getLocalHost().getHostName();
                //ip = local.substring(local.indexOf("/"));
            } catch (IOException ex) {
            }
            
            //System.out.println("IP LOCAL "+local);
            //System.out.println("IP PACKET "+receive_packet.getAddress().getHostName().toString());

            if (!ip.equals("") && !receive_packet.getAddress().getHostName().contains(local)) {
                System.out.print(".");
                String data = new String(receive_packet.getData(), 0, receive_packet.getLength());
                String[] data_parsed = data.split(" ");

                String version = data_parsed[1];
                String fileID = data_parsed[2];
                String chunkNO = data_parsed[3];
                String degree = data_parsed[4];
                String info = data_parsed[6];

                //verifica se o chunk que está a tentar receber é da mesma versão do sistema
                if (version.equalsIgnoreCase(Backup.getVersion())) {

                    //verifica se ja existe uma key criada no mapa para aquele ficheiro
                    if (!Backup.getStoredChunksMap().containsKey(fileID)) {
                        Backup.initiateStoredChunk(fileID);
                    }
                    //verifica se já armazenou esse chunk
                    if (!Backup.existChunk(fileID, chunkNO)) {
                        //Vamos criar o ficheiro txt para armazenar os chunks desse ficheiro
                        FileWriter fileWritter = null;
                        try {

                            File file = new File(fileID + ".txt");
                            //se o ficheiro não existir, cria-o
                            if (!file.exists()) {
                                file.createNewFile();

                            }


                            fileWritter = new FileWriter(file.getName(), true);
                            BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
                            bufferWritter.write(info + " " + chunkNO + "\n");
                            bufferWritter.close();

                            //adiciona o chunk ao hashmap que contém o número dos chunks armazenados desse ficheiro
                            Backup.getStoredChunks(fileID).add(chunkNO);
                            
                            //STORED <Version> <FileId> <ChunkNo><CRLF><CRLF>
                            socket_control.joinGroup(address);
                            String msg="STORED "+version+" "+fileID+" "+chunkNO+"\n\n";
                            DatagramPacket chunk_stored = new DatagramPacket(msg.getBytes(), msg.length(), this.address, this.MC);
                            
                            Random randomGenerator = new Random();
                            int randomDelay = randomGenerator.nextInt(400);
                            
                            try {
                                Thread.sleep(randomDelay);
                                socket_control.send(chunk_stored);
                            } catch (InterruptedException ex) {
                                Logger.getLogger(Receiver.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            
                        } catch (IOException ex) {
                            Logger.getLogger(Receiver.class.getName()).log(Level.SEVERE, null, ex);
                        } finally {
                            try {
                                fileWritter.close();
                            } catch (IOException ex) {
                                Logger.getLogger(Receiver.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                }
            }
        }
    }
}
