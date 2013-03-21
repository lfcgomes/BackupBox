//class main do Backup-Box, com a funcionalidade principal de controlar enviar/receber

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Scanner;

public class Backup {

    private static HashMap<String, File> map_sha_files = new HashMap<String, File>();
    private static HashMap<String, HashMap<Integer, byte[]>> map_chunk_files =
            new HashMap<String, HashMap<Integer, byte[]>>();

    public static HashMap<String, File> getMapShaFiles() {
        return map_sha_files;
    }
    public static HashMap<String, HashMap<Integer, byte[]>> getMapChunkFiles() {
        return map_chunk_files;
    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {

        int MC = 7777;
        int MD = 7788;
        String ip_address = "224.0.2.11";
        MulticastSocket socket = new MulticastSocket(MC);

        File dir = new File("files"); //substituir com directorio do ficheiro de configuracao

        InetAddress address = InetAddress.getByName(ip_address);
        socket.joinGroup(address);

        String version = "1.0";
        //Utils.readFromFile("configuration.txt");

        //TODO lançar thread RECEIVER para estar à espera de chunks?
        Receiver receiver = new Receiver(address, MC, MD);
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

                        if (map_sha_files.size() == 0) {
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
                    Sender sender = new Sender(socket, address, MC, MD, sha);
                    sender.start();
                    
                    //while(Utils.flag_sending==0){}
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
