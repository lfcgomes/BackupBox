package bck;


import bck.Backup;
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
public class Restore extends Thread {

    InetAddress address = null;
    int MDR;
    MulticastSocket socket = null;
    
    public Restore(InetAddress ad, int m_d_r) throws IOException {
        address = ad;
        MDR = m_d_r;
        socket = new MulticastSocket(MDR);
        socket.joinGroup(ad);
    }

    @Override
    public void run() {
        while (true) {
            System.out.println("RESTORE");

            byte[] receive_buffer = new byte[65000];
            String local = "";
                  
            DatagramPacket receive_packet = new DatagramPacket(receive_buffer, receive_buffer.length);
            try {
                //socket.setSoTimeout(2000);
                Thread.sleep(10);
                socket.receive(receive_packet);
                local = InetAddress.getLocalHost().getHostName();
                
            } catch (Exception ex) {
                System.out.println("Failed receive restore");
            }

            if (!local.equals("") && !receive_packet.getAddress().getHostName().contains(local)) {
                
                String data = new String(receive_packet.getData(), 0, receive_packet.getLength());
                String[] data_parsed = data.split(" ");
                System.out.println("data "+data);
                String version = data_parsed[1];
                String fileID = data_parsed[2];
                //String chunkNO = data_parsed[3];
                String unparsed = data_parsed[3];
                String chunkNO = unparsed.substring(0, unparsed.indexOf("\n"));
                String info = unparsed.substring(unparsed.lastIndexOf("\n")+1);
                        
                
                System.out.println("0 -"+data_parsed[0]);
                System.out.println("1 -"+data_parsed[1]);
                System.out.println("2 -"+data_parsed[2]);
                System.out.println("3 -"+chunkNO);
                System.out.println("4 -"+info);
                
                
                //verifica se o chunk que está a tentar receber é da mesma versão do sistema
                if (version.equalsIgnoreCase(Backup.getVersion())) {

                    //verifica se já armazenou esse chunk
                    if (!Backup.existChunk(fileID,chunkNO)) {
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
                            //System.out.println("Guardei: "+chunkNO);
                            //STORED <Version> <FileId> <ChunkNo><CRLF><CRLF>
        
                            String msg="STORED "+version+" "+fileID+" "+chunkNO+"\n\n";
                            DatagramPacket chunk_stored = new DatagramPacket(msg.getBytes(), msg.length(), this.address, this.MC);
                            
                            Random randomGenerator = new Random();
                            int randomDelay = randomGenerator.nextInt(400);
                            
                            try {
                                //Thread.sleep(randomDelay);
                                socket_control.send(chunk_stored);
                            } catch (Exception ex) {
                                Logger.getLogger(Restore.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            
                        } catch (IOException ex) {
                            Logger.getLogger(Restore.class.getName()).log(Level.SEVERE, null, ex);
                        } finally {
                            try {
                                fileWritter.close();
                            } catch (IOException ex) {
                                Logger.getLogger(Restore.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                }*/
            }
        }
    }
}
