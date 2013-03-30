package bck;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

//classe para ler as mensagens enviadas
public class Message extends Thread {

    MulticastSocket socket = null;
    InetAddress address = null;
    int MC;
    int MDR;
    MulticastSocket socket_restore = null;

    public Message(MulticastSocket s, InetAddress ad, int p, int mdr) throws IOException {
        socket = s;
        address = ad;
        MC = p;
        MDR = mdr;
        socket_restore = new MulticastSocket(MDR);
        socket_restore.joinGroup(ad);
    }

    @Override
    @SuppressWarnings("SleepWhileInLoop")
    public void run() {
        int i = 0;
        while (true) {
            byte[] receive_buffer = new byte[1024];

            DatagramPacket receive_packet = new DatagramPacket(receive_buffer, receive_buffer.length);
            String local = "";

            try {
                socket.receive(receive_packet);
                local = InetAddress.getLocalHost().getHostName();
            } catch (IOException ex) {
            }

            String data = new String(receive_packet.getData(), 0, receive_packet.getLength());
            String[] data_parsed = data.split(" ");

            if (!local.equals("") && !receive_packet.getAddress().getHostName().contains(local)) {

                //Temos de saber quantas mensagens STORED já recebemos, para saber se ainda temos de guardar
                if (data_parsed[0].equalsIgnoreCase("STORED")) {
                    i++;

                    String version = data_parsed[1];
                    String fileID = data_parsed[2];

                    if (Backup.getVersion().equalsIgnoreCase(version)) {

                        if (Backup.getSendedFiles().contains(fileID)) {
                            if (!Backup.getReceivedSendedFiles().contains(fileID)) {
                                Backup.getReceivedSendedFiles().add(fileID);
                            }

                            int chunkNO = Integer.parseInt(data_parsed[3].substring(0, data_parsed[3].indexOf("\n")));
                            HashMap<Integer, Integer> missing = new HashMap<Integer, Integer>();
                            missing = Backup.getMissingChunks(fileID);

                            //não vai acontecer
                            if (missing.get(chunkNO) == null) {
                                if ((Backup.getFileReplicationDegree(fileID) - 1) == 0) {
                                    missing.remove(chunkNO);
                                } else {
                                    missing.put(chunkNO, Backup.getFileReplicationDegree(fileID) - 1);
                                }
                            } else {
                                //vai diminuir o replication degree obrigatorio para o chunk
                                int old_rep = missing.get(chunkNO);

                                if (old_rep == 1) {
                                    missing.remove(chunkNO);
                                } else {
                                    missing.put(chunkNO, old_rep - 1);
                                }
                            }

                            Backup.getMissingChunks().put(fileID, missing);
                        }
                    }
                } else {
                    //GETCHUNK <Version> <FileId> <ChunkNo><CRLF><CRLF>
                    if (data_parsed[0].equalsIgnoreCase("GETCHUNK")) {

                        String version = data_parsed[1];
                        if (Backup.getVersion().equalsIgnoreCase(version)) {

                            //Vamos enviar o chunk pedido
                            
                            String fileID = data_parsed[2];
                            String unparsed = data_parsed[3];
                            String chunkNO = unparsed.substring(0, unparsed.indexOf("\n"));
                            
                            //System.out.println("vamos ver se tenho o chunk");
                            // Verifica se tenho guardado aquele chunkNO, para o fileID dado.
                            if (Backup.getStoredChunks(fileID).contains(chunkNO)) {                              
                                
                                RandomAccessFile f = null;
                                try {
                                    f = new RandomAccessFile(fileID+"_"+chunkNO, "r");
                                } catch (FileNotFoundException ex) {
                                    Logger.getLogger(Message.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                byte[] chunk = null;
                                try {
                                    chunk = new byte[(int)f.length()];
                                    f.read(chunk);
                                } catch (Exception ex) {
                                    Logger.getLogger(Message.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                
                                
                                //System.out.println("SIZE DO CHUNK A ENVIAR: "+chunk.length);
                                
                                //CHUNK <Version> <FileId> <ChunkNo><CRLF><CRLF><Body>
                                
                                //Se tenho o chunk, tenho que ir ao ficheiro buscar
                                String msg = "CHUNK " + Backup.getVersion() + " "+fileID+" " + chunkNO + "\n\n";
                                
                                byte[] msg_byte = msg.getBytes();
                                
                                //System.out.println("MESSAGE BYTE "+ msg_byte.length);
                                byte[] final_msg = new byte[msg_byte.length + chunk.length];
                                System.arraycopy(msg_byte, 0, final_msg, 0, msg_byte.length);
                                System.arraycopy(chunk, 0, final_msg, msg_byte.length, chunk.length);

                                DatagramPacket chunk_packet = new DatagramPacket(final_msg, final_msg.length, this.address, this.MDR);
                                
                                //Random randomGenerator = new Random();
                                //int randomDelay = randomGenerator.nextInt(400);
                                
                                try {
                                    Thread.sleep(100);
                                    socket_restore.send(chunk_packet);
                                } catch (Exception ex) {
                                    Logger.getLogger(Message.class.getName()).log(Level.SEVERE, null, ex);
                                }

                            }
                        }
                    }
                    else{
                        if (data_parsed[0].equalsIgnoreCase("DELETE")) {

                            String unparsed = data_parsed[1];
                            String fileID = unparsed.substring(0, unparsed.indexOf("\n"));
                         
                            File dir = new File(".");
                            File[] foundFiles = dir.listFiles();

                            for (File filename : foundFiles) {
                                if (filename.getName().startsWith(fileID + "_")) {
                                    filename.delete();
                                }

                            }
                            
                        }
                        
                    }
                }
            }
        }

    }
}
