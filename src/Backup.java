//class main do Backup-Box, com a funcionalidade principal de controlar enviar/receber

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

public class Backup {

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {

        int port = 7777;
        String ip_address = "224.0.2.10";
        MulticastSocket socket = new MulticastSocket(port);
        
        File dir = new File("files"); //substituir com directorio do ficheiro de configuracao
          
        //ip dele próprio
        InetAddress address = InetAddress.getByName(ip_address);
        socket.joinGroup(address);

        //Utils.readFromFile("configuration.txt");
        
        int op = 0;
        while(op != 5){
        
            menu();
            
            Scanner in = new Scanner(System.in);
            op=in.nextInt();
            switch(op){
                
                case 1:
                    File backup_f = null;
                    while(true){
                        System.out.println("\nChoose a file to backup:");
                        File[] files = dir.listFiles();
                        for(int i=0; i<files.length;i++){
                            System.out.println(i+1 + ": "+files[i].getName());
                            /*if(files[i].isFile()){
                                Utils.geraHexFormat(files[i].toString());
                            }*/
                        }
                        System.out.print("Option: ");
                        int file_choice = in.nextInt();
                        
                        try {
                            backup_f = files[file_choice-1];
                            break;
                        }catch (Exception ex) {
                            System.out.println("Invalid choice!");
                        }
                    }

                    System.out.println(backup_f.getName());
                    //TODO 
                  // break;
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
            
            //
        }
    }
    
    public static void menu(){
        
        System.out.println("Welcome to the BackupBox. Choose an option:\n");
        System.out.println("1. Backup a file");
        System.out.println("2. Restore a file");
        System.out.println("3. Delete a file");
        System.out.println("4. Reclaim space");
        System.out.println("5. Quit\n");
        System.out.print("Option: ");
    }
}
