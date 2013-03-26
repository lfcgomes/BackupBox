package bck;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
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

    public void run() {
        int i = 0;
        while (true) {
            byte[] receive_buffer = new byte[1024];

            DatagramPacket receive_packet = new DatagramPacket(receive_buffer, receive_buffer.length);
            String local = "";
            String ip = "";

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
                    System.out.println("numero de stores que recebeu " + i);
                    System.out.println("recebi um stored");
                    String version = data_parsed[1];
                    String fileID = data_parsed[2];

                    if (Backup.getVersion().equalsIgnoreCase(version)) {

                        if (Backup.getSendedFiles().contains(fileID)) {
                            if (!Backup.getReceivedSendedFiles().contains(fileID)) {
                                Backup.getReceivedSendedFiles().add(fileID);
                            }

                            int chunkNO = Integer.parseInt(data_parsed[3].substring(0, data_parsed[3].indexOf("\n")));
                            System.out.println("store do chunkNO " + chunkNO);
                            HashMap<Integer, Integer> missing = new HashMap<Integer, Integer>();
                            missing = Backup.getMissingChunks(fileID);

                            //não vai acontecer
                            if (missing.get(chunkNO) == null) {
                                if ((Backup.getFileReplicationDegree(fileID) - 1) == 0) {
                                    missing.remove(chunkNO);
                                } else {
                                    missing.put(chunkNO, Backup.getFileReplicationDegree(fileID) - 1);
                                }
                                System.out.println("entrou aqui");
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
                        System.out.println("recebi um getchunk");
                        if (Backup.getVersion().equalsIgnoreCase(version)) {

                            //Vamos enviar o chunk pedido
                            
                            
                            String fileID = data_parsed[2];
                            String unparsed = data_parsed[3];
                            String chunkNO = unparsed.substring(0, unparsed.indexOf("\n"));
                            System.out.println("getchunk que recebi: "+fileID+" "+chunkNO);
                            System.out.println("vamos ver se tenho o chunk");
                            // Verifica se tenho guardado aquele chunkNO, para o fileID dado.
                            if (Backup.getStoredChunks(fileID).contains(chunkNO)) {
                                System.out.println("Tenho o chunk pedido");

                                String info = Utils.readFromFile(fileID+".txt", chunkNO);
                                System.out.println("info " + info);
                                
                                //CHUNK <Version> <FileId> <ChunkNo><CRLF><CRLF><Body>
                                
                                //Se tenho o chunk, tenho que ir ao ficheiro buscar
                                String msg = "CHUNK " + Backup.getVersion() + " "+fileID+" " + chunkNO + "\n\n" + info;

                                DatagramPacket chunk = new DatagramPacket(msg.getBytes(), msg.length(), this.address, this.MDR);

                                try {
                                    socket_restore.send(chunk);
                                } catch (IOException ex) {
                                    Logger.getLogger(Message.class.getName()).log(Level.SEVERE, null, ex);
                                }

                            }
                        }
                    }
                }
            }
        }

    }
}
