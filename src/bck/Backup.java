package bck;

//class main do Backup-Box, com a funcionalidade principal de controlar enviar/receber

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class Backup {
    
    private static String version = "1.0";
    private static HashMap<String, File> map_sha_files = new HashMap<String, File>();
    private static HashMap<String, HashMap<Integer, byte[]>> map_chunk_files =
            new HashMap<String, HashMap<Integer, byte[]>>();
    
    private static HashMap<String,ArrayList<Integer>> stored_chunks = new HashMap<String,ArrayList<Integer>>();
    private static HashMap<String,HashMap<Integer, Integer>> missing_chunks = new HashMap<String,HashMap<Integer,Integer>>();
    
    public static ArrayList getStoredChunks(String fileID){
        return stored_chunks.get(fileID);
    }
    
    public static HashMap getMissingChunks(String fileID){
        return missing_chunks;
    }
    
    public static boolean existChunk(String fileID, String chunkNO){
        return stored_chunks.get(fileID).contains(chunkNO);
    }
    public static HashMap<String,ArrayList<Integer>> getStoredChunksMap(){
            return stored_chunks;
    }
    public static void initiateStoredChunk(String fileID){
        stored_chunks.put(fileID, new ArrayList<Integer>());
    }
    public static HashMap<String, File> getMapShaFiles() {
        return map_sha_files;
    }
    public static HashMap<String, HashMap<Integer, byte[]>> getMapChunkFiles() {
        return map_chunk_files;
    }
    
    public static String getVersion(){
        return version;
    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        GUI g = new GUI();
        g.show(); 
    }

    public static void backup(int mc, int md, String ip, String vrs) throws IOException, NoSuchAlgorithmException{

        /*
        int MC = 7777;
        int MD = 7788;
        String ip_address = "224.0.2.11";
         */
        
        int MC = mc;
        int MD = md;
        String ip_address = ip;
        Backup.version = vrs;
        MulticastSocket socket = new MulticastSocket(MC);

        File dir = new File("files"); //substituir com directorio do ficheiro de configuracao

        InetAddress address = InetAddress.getByName(ip_address);
        socket.joinGroup(address);

        
        //Utils.readFromFile("configuration.txt");

        //TODO lançar thread RECEIVER para estar à espera de chunks?
        //Message message = new Message(socket, address, MC);
        Receiver receiver = new Receiver(socket,address, MC, MD);
        //message.start();
        receiver.start();

        int op = 0;
        while (op != 5) {

            menu();

            File[] files = dir.listFiles();  /*      
             for (int i = 0; i < files.length; i++) {
             if(files[i].isFile()){
             map_sha_files.put(Utils.geraHexFormat(files[i].getPath()), files[i]);
             }
             }*/
            for (int i = 0; i < files.length; i++) {

                HashMap<Integer, byte[]> temp_chunk = new HashMap<Integer, byte[]>();

                if (files[i].isFile()) {

                    File aux = new File(files[i].toString());
                    FileInputStream f = new FileInputStream(aux);

                    byte[] dataBytes = new byte[1024];
                    int c = 0;

                    while ((f.read(dataBytes)) != -1) {	//lê todos os chunks do ficheiro
                        temp_chunk.put(c, dataBytes);
                        c++;
                        dataBytes = new byte[1024];
                    }
                    map_sha_files.put(Utils.geraHexFormat(files[i].getPath()), files[i]);
                    map_chunk_files.put(Utils.geraHexFormat(files[i].getPath()), temp_chunk);
                }
               
            }



            Scanner in = new Scanner(System.in);
            op = in.nextInt();
            switch (op) {

                case 1:
                    File backup_f = null;
                    boolean no_files = false;
                    String sha = "";
                    while (true) {

                        if (map_sha_files.isEmpty()) {
                            System.out.println("No files in the directory!\n");
                            no_files = true;
                            break;
                        }
                        System.out.println("\nChoose a file to backup:");
                        for (int i = 0; i < files.length; i++) {
                            if (files[i].isFile()) {
                                System.out.println(i + 1 + ": " + files[i].getName());
                            }
                        }
                        System.out.print("Option: ");
                        int file_choice = in.nextInt();

                        try {
                            backup_f = files[file_choice - 1];
                            sha = Utils.geraHexFormat(backup_f.getPath());
                            break;
                        } catch (Exception ex) {
                            System.out.println("Invalid choice!");
                        }
                    }

                    if (no_files) {
                        break;
                    }

                    int replication_degree = 3;//in.nextInt();

                    //TODO lançar thread Sender?
                    Utils.flag_sending = 1;
                    Sender sender = new Sender(address, MC, MD, sha);
                    sender.start();
                    
                    while(Utils.flag_sending==1){System.out.print("");}
                    /*
                    HashMap<String, ArrayList<Integer>> teste = stored_messages_received.get(sha);
                    
                    int size = teste.size();
                    
                    System.out.println("SIZE: "+size);
                    
                    ArrayList<Integer> chunks_stored = teste.get(version);
                    
                    for(int x=0;x<chunks_stored.size();x++)
                        System.out.println("Stored: "+chunks_stored.get(x));
                    */

                    break;
                case 2:
                    System.out.println("Enter the name of your file:");
                    break;
                case 3:
                    break;
                case 4:
                    break;
                case 5:
                    System.out.println("Goodbye!");
                    break;
                default:
                    System.out.println("Invalid option!");
            }
        }
    }
    public static void menu() {

        System.out.println("Welcome to the BackupBox. Choose an option:\n");
        System.out.println("1. Backup a file");
        System.out.println("2. Restore a file");
        System.out.println("3. Delete a file");
        System.out.println("4. Reclaim space");
        System.out.println("5. Quit\n");
        System.out.print("Option: ");
    }
}
