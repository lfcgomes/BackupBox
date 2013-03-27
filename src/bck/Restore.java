package bck;


import bck.Backup;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
                
                String version = data_parsed[1];
                String fileID = data_parsed[2];
                String unparsed = data_parsed[3];
                String chunkNO = unparsed.substring(0, unparsed.indexOf("\n"));             
                
                byte[] info = new byte[Backup.getMapChunkFiles().get(fileID).get(chunkNO).length];
                System.arraycopy(receive_buffer, 78, info, 0, Backup.getMapChunkFiles().get(fileID).get(chunkNO).length);
                
                //verifica se o chunk que está a tentar receber é da mesma versão do sistema
                if (version.equalsIgnoreCase(Backup.getVersion())) {

                    //verifica se o ficheiro a restaurar já foi feito backup
                    if (Backup.getReceivedSendedFiles().contains(fileID)) {
                        
                        //Se é o primeiro chunk a ser restaurado, iniciar o HashMap
                        if(Backup.getRestoredChunks(fileID) == null){
                            
                            Backup.initiateRestoredChunks(fileID);
                            //
                            FileOutputStream file = null;
                            try {
                                file = new FileOutputStream(Backup.getMapShaFiles().get(fileID).getName());
                            } catch (FileNotFoundException ex) {
                                Logger.getLogger(Restore.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            Backup.getRestoredFiles().put(fileID, file);
                        }
                                                
                        if(!Backup.getRestoredChunks(fileID).contains(chunkNO)){
                            try {
                                Backup.getRestoredFiles().get(fileID).write(info);
                                Backup.getRestoredChunks(fileID).add(chunkNO);
                            } catch (IOException ex) {
                                Logger.getLogger(Restore.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            
                        }
                        
                        if(Backup.getMapChunkFiles().get(fileID).size() == Backup.getRestoredChunks(fileID).size()){
                            try {
                                Utils.flag_restoring = 0;
                                Backup.getRestoredFiles().get(fileID).flush();
                                Backup.getRestoredFiles().get(fileID).close();
                                
                            } catch (IOException ex) {
                                Logger.getLogger(Restore.class.getName()).log(Level.SEVERE, null, ex);
                            }
                           
                        }
                    }
                }
            }
        }
    }
}
