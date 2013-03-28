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
public class Delete extends Thread {

    InetAddress address = null;
    MulticastSocket socket = null;

    public Delete(MulticastSocket s) throws IOException {
        socket = s;
    }

    @Override
    public void run() {
        while (true) {

            byte[] receive_buffer = new byte[128];
            String local = "";

            DatagramPacket receive_packet = new DatagramPacket(receive_buffer, receive_buffer.length);
            try {
                //socket.setSoTimeout(2000);
                Thread.sleep(10);
                socket.receive(receive_packet);
                local = InetAddress.getLocalHost().getHostName();

            } catch (Exception ex) {
            }

            if (!local.equals("")){// && !receive_packet.getAddress().getHostName().contains(local)) {

              
                String data = new String(receive_packet.getData(), 0, receive_packet.getLength());
                String[] data_parsed = data.split(" ");

                if(data_parsed[0].equalsIgnoreCase("DELETE")){
                    
                    
                    String unparsed = data_parsed[1];
                    String fileID = unparsed.substring(0, unparsed.indexOf("\n"));
                    
                    //Se restaurou algum chunk daquele ficheiro, vai apagar
                    if (Backup.getStoredChunks(fileID) != null) {
                        int n=0;
                        while(!Backup.getStoredChunks(fileID).isEmpty()){
                            System.out.println("Chunk "+Backup.getStoredChunks(fileID).get(n));
                            File f = new File(fileID+"_"+Backup.getStoredChunks(fileID).get(n));
                            if(f.exists())
                                f.delete();
                            Backup.getStoredChunks(fileID).remove(n);
                        }

                    }
                }
            }
        }
    }
}
