package bck;

//class main do Backup-Box, com a funcionalidade principal de controlar enviar/receber
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.channels.DatagramChannel;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;

public class Backup {

    private static String version = "1.0";
    private static int disk_space; //substituir por input
    /* HashMaps com info dos ficheiros que eu ENVIO */
    //Guarda o fileID e o ficheiro
    private static HashMap<String, File> map_sha_files = new HashMap<String, File>();
    private static HashMap<String, Integer> file_replication_degree = new HashMap<String, Integer>();
    /* Para cada fileID, guarda o número do chunk, e o respectivo chunk*/
    private static HashMap<String, HashMap<String, byte[]>> map_chunk_files =
            new HashMap<String, HashMap<String, byte[]>>();
    /* Para cada fileID, guarda um HashMap com o número do chunk, e quantas vezes ele ainda precisa de ser
     * enviado para ser guardado na LAN */
    private static HashMap<String, HashMap<Integer, Integer>> missing_chunks = new HashMap<String, HashMap<Integer, Integer>>();
    //Guarda o fileID dos ficheiros ficheiros enviados, que foram armazenados na LAN
    private static ArrayList<String> received_sended_files = new ArrayList<String>();
    //Guarda o fileID dos ficheiros que tentei enviar para a LAN
    private static ArrayList<String> sended_files = new ArrayList<String>();
    /* HashMaps com info dos ficheiros que eu RECEBO */
    //Guarda o fileID e a lista com o número de chunks que já foram armazenados por mim
    //Serve para ir ver se tenho esse chunk, antes de o ir buscar ao ficheiro
    private static HashMap<String, ArrayList<String>> stored_chunks = new HashMap<String, ArrayList<String>>();
    private static HashMap<String, String> stored_file_minimum_degree = new HashMap<String,String>();
    private static HashMap<String, ArrayList<String>> restored_chunks = new HashMap<String, ArrayList<String>>();
    private static HashMap<String, FileOutputStream> restored_files = new HashMap<String, FileOutputStream>();

    public static int getFileReplicationDegree(String sha) {
        return file_replication_degree.get(sha);
    }

    public static ArrayList<String> getReceivedSendedFiles() {
        return received_sended_files;
    }

    public static ArrayList<String> getSendedFiles() {
        return sended_files;
    }

    public static void initiateMissingChunks(String fileID) {
        missing_chunks.put(fileID, new HashMap<Integer, Integer>());
    }

    public static void initiateRestoredChunks(String fileID) {
        restored_chunks.put(fileID, new ArrayList<String>());
    }

    public static ArrayList<String> getStoredChunks(String fileID) {
        return stored_chunks.get(fileID);
    }

    public static ArrayList<String> getRestoredChunks(String fileID) {
        return restored_chunks.get(fileID);
    }

    public static HashMap getMissingChunks(String fileID) {
        return missing_chunks.get(fileID);
    }

    public static HashMap getMissingChunks() {
        return missing_chunks;
    }

    public static boolean existChunk(String fileID, String chunkNO) {
        return stored_chunks.get(fileID).contains(chunkNO);
    }

    public static HashMap<String, ArrayList<String>> getStoredChunksMap() {
        return stored_chunks;
    }

    public static void initiateStoredChunk(String fileID) {
        stored_chunks.put(fileID, new ArrayList<String>());
    }

    public static HashMap<String, File> getMapShaFiles() {
        return map_sha_files;
    }

    public static HashMap<String, FileOutputStream> getRestoredFiles() {
        return restored_files;
    }

    public static HashMap<String, HashMap<String, byte[]>> getMapChunkFiles() {
        return map_chunk_files;
    }

    public static String getVersion() {
        return version;
    }

    public static int getDiskSpace() {
        return disk_space;
    }

    public static void updateDiskSpace(int chunk_size) {
        disk_space -= chunk_size;
    }
    
    public static HashMap<String,String> getStoredFileMinimumDegree(){
        return stored_file_minimum_degree;
    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        GUI g = new GUI();
        g.show();
    }

    @SuppressWarnings("SleepWhileInLoop")
    public static void backup(int mc, int mdb, int mdr, String ip, String vrs, int DiskSpace) throws IOException, NoSuchAlgorithmException, InterruptedException {

        int MC = mc;
        int MDB = mdb;
        int MDR = mdr;
        String ip_address = ip;
        Backup.version = vrs;
        MulticastSocket socket = new MulticastSocket(MC);
        disk_space = DiskSpace;

        File dir = new File("files"); //substituir com directorio do ficheiro de configuracao

        InetAddress address = InetAddress.getByName(ip_address);
        socket.joinGroup(address);

        Receiver receiver = new Receiver(socket, address, MC, MDB);
        Message message = new Message(socket, address, MC, MDR, MDB);
        message.start();
        receiver.start();

        int op = 0;
        while (op != 5) {

            menu();

            File[] files = dir.listFiles();

            for (int i = 0; i < files.length; i++) {

                HashMap<String, byte[]> temp_chunk = new HashMap<String, byte[]>();

                if (files[i].isFile()) {

                    File aux = new File(files[i].toString());
                    FileInputStream f = new FileInputStream(aux);

                    byte[] dataBytes = new byte[64000];
                    int c = 0;
                    int size, lastsize = 0;

                    while ((size = f.read(dataBytes)) != -1) {	//lê todos os chunks do ficheiro
                        temp_chunk.put(String.valueOf(c), dataBytes);
                        c++;
                        dataBytes = new byte[64000];
                        lastsize = size;
                    }
                    if (lastsize % 64000 == 0) {
                        dataBytes = new byte[0];
                        temp_chunk.put(String.valueOf(c - 1), dataBytes);
                    } else {
                        byte[] last_chunk = new byte[lastsize];
                        System.arraycopy(temp_chunk.get(String.valueOf(c - 1)), 0, last_chunk, 0, lastsize);
                        temp_chunk.remove(String.valueOf(c - 1));
                        temp_chunk.put(String.valueOf(c - 1), last_chunk);
                    }

                    map_sha_files.put(Utils.geraHexFormat(files[i].getPath()), files[i]);
                    map_chunk_files.put(Utils.geraHexFormat(files[i].getPath()), temp_chunk);
                }
            }

            Scanner in = new Scanner(System.in);
            op = in.nextInt();
            String sha = "";

            switch (op) {
                case 1:
                    File backup_f;
                    boolean no_files = false;
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
                        System.out.println("0: Back");
                        System.out.print("Option: ");
                        int file_choice = in.nextInt();

                        if (file_choice == 0) {
                            no_files = true;
                            break;
                        }

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

                    System.out.print("Replication degree: ");
                    int replication_degree = in.nextInt();

                    file_replication_degree.put(sha, replication_degree);

                    //TODO lançar thread Sender?
                    Utils.flag_sending = 1;
                    Backup.initiateMissingChunks(sha);
                    

                    Sender sender = new Sender(address, MC, MDB, sha, replication_degree);
                    sender.start();

                    while (Utils.flag_sending == 1) {
                        System.out.print("");
                    }
                    if (Utils.flag_sending == 0) {
                        System.out.println("File sent to the LAN\n");
                    } else if (Utils.flag_sending == 2) {
                        System.out.println("Replication degree below expected! Please try again.\n");
                    }
                    break;
                case 2:
                    no_files = false;
                    while (true) {

                        if (received_sended_files.isEmpty()) {
                            System.out.println("No files to restore!\n");
                            no_files = true;
                            break;
                        }
                        System.out.println("\nChoose a file to restore:");
                        for (int i = 0; i < received_sended_files.size(); i++) {
                            if (map_sha_files.get(received_sended_files.get(i)) != null) {
                                System.out.println((i + 1) + ": " + map_sha_files.get(received_sended_files.get(i)).getName());
                            }
                        }
                        System.out.println("0: Back");
                        System.out.print("Option: ");
                        int file_choice = in.nextInt();

                        if (file_choice == 0) {
                            no_files = true;
                            break;
                        }

                        try {
                            sha = received_sended_files.get(file_choice - 1);
                            break;
                        } catch (Exception ex) {
                            System.out.println("Invalid choice!\n");
                        }
                    }

                    if (no_files) {
                        break;
                    }

                    /* Ciclo para pedir todos os chunks a restaurar */
                    HashMap<String, byte[]> chunks_to_restore = map_chunk_files.get(sha);
                    int n = 0;

                    System.out.println("Restoring...");
                    Restore restore = new Restore(address, mdr);
                    restore.start();

                    while (chunks_to_restore.size() > n) {
                        //GETCHUNK <Version> <FileId> <ChunkNo><CRLF><CRLF>
                        String getchunk = "GETCHUNK " + getVersion() + " " + sha + " " + n + "\n\n";
                        DatagramPacket getchunk_packet = new DatagramPacket(getchunk.getBytes(), getchunk.length(), address, MC);

                        Thread.sleep(10);
                        socket.send(getchunk_packet);
                        n++;
                        Utils.flag_restoring = 1;
                    }
                    while (Utils.flag_restoring == 1) {
                        System.out.print("");
                    }

                    System.out.println("Done!");

                    break;
                case 3:
                    no_files = false;
                    while (true) {
                        if (map_sha_files.isEmpty()) {
                            System.out.println("No files to delete!\n");
                            no_files = true;
                            break;
                        }
                        System.out.println("\nChoose a file to delete:");
                        for (int i = 0; i < files.length; i++) {
                            if (files[i].isFile()) {
                                System.out.println(i + 1 + ": " + files[i].getName());
                            }
                        }
                        System.out.println("0: Back");
                        System.out.print("Option: ");
                        int file_choice = in.nextInt();

                        if (file_choice == 0) {
                            no_files = true;
                            break;
                        }

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

                    String delete_file = "DELETE " + sha + "\n\n";
                    DatagramPacket delete_file_packet = new DatagramPacket(delete_file.getBytes(), delete_file.length(), address, MC);

                    Thread.sleep(10);
                    socket.send(delete_file_packet);

                    System.out.println("Done!");
                    if (getReceivedSendedFiles().contains(sha)) {
                        getReceivedSendedFiles().remove(sha);
                    }

                    break;
                case 4:
                    System.out.println("Are you sure you want to delete all the backed up chunks?");
                    String yes_no = "";
                    while (true) {
                        System.out.print("(yes/no): ");
                        yes_no = in.next();
                        if (yes_no.equalsIgnoreCase("yes") || yes_no.equalsIgnoreCase("no")) {
                            break;
                        }
                    }

                    if (yes_no.equalsIgnoreCase("no")) {
                        break;
                    }
                    disk_space = 0;
                    Set<String> stored_filenames = getStoredChunksMap().keySet();
                    
                    File chunks_dir = new File(".");
                    File[] foundFiles = chunks_dir.listFiles();

                    for (String fileID : stored_filenames) {

                        for (File filename : foundFiles) {
                            if(filename.getName().equals(fileID+".txt"))
                                filename.delete();
                            
                            if (filename.getName().startsWith(fileID + "_")) {
                                //encontrou o ficheiro, vai apagá-lo e enviar REMOVED do chunk respectivo
                                filename.delete();
                                Backup.updateDiskSpace((int) -filename.length());

                                String chunk_no = filename.getName().substring(filename.getName().indexOf("_") + 1);
                                String msg = "REMOVED " + getVersion() + " " + fileID + " " + chunk_no + "\n\n";

                                DatagramPacket removed_packet = new DatagramPacket(msg.getBytes(), msg.length(), address, MC);

                                Thread.sleep(200);
                                socket.send(removed_packet);
                            }
                        }
                        Backup.getStoredChunksMap().remove(fileID);
                    }
                    break;
                case 5:
                    System.out.println("Goodbye!");
                    Utils.should_stop = true;
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
