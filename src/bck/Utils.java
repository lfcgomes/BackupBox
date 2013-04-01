package bck;

//classe com funções utilizadas por outras classes
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class Utils {

    static int flag_sending = 0;
    static int flag_restoring = 0;
    /* */
    static boolean should_stop = false;

    public static String geraHexFormat(String f)
            throws NoSuchAlgorithmException, IOException {

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        FileInputStream fis = new FileInputStream(f);

        byte[] dataBytes = new byte[1024];

        int nread = 0;
        while ((nread = fis.read(dataBytes)) != -1) {
            md.update(dataBytes, 0, nread);
        };

        byte[] mdbytes = md.digest();

        //convert the byte to hex format

        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < mdbytes.length; i++) {
            sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();
    }

    public static int replace(String filename, String chunk_no, String operation) {
      String oldFileName = filename;
      String tmpFileName = "tmp_"+filename;

      BufferedReader br = null;
      BufferedWriter bw = null;
      int novo_count=0;
      try {
         br = new BufferedReader(new FileReader(oldFileName));
         bw = new BufferedWriter(new FileWriter(tmpFileName));
         String line;
         
         while ((line = br.readLine()) != null) {
            String[] data_parsed = line.split(" ");
            if (data_parsed[0].equals("chunk"+chunk_no)){
                
                String old_count = data_parsed[1].substring(data_parsed[1].indexOf("#")+1); 
                if(operation.equalsIgnoreCase("PLUS"))
                    novo_count = Integer.parseInt(old_count)+1;
                else{
                    novo_count = Integer.parseInt(old_count)-1;
                }
                line = line.replace(data_parsed[1], "#"+novo_count);

            }
            bw.write(line+"\n");
         }
         if(novo_count==0){//se estiver a 0 quer dizer que ainda não estava no ficheiro
            bw.write("chunk"+chunk_no+" "+"#1"+"\n");
         }
      } catch (Exception e) {
         return -1;
      } finally {
         try {
            if(br != null)
               br.close();
         } catch (IOException e) {
            //
         }
         try {
            if(bw != null)
               bw.close();
         } catch (IOException e) {
            //
         }
      }
      // Once everything is complete, delete old file..
      File oldFile = new File(oldFileName);
      oldFile.delete();

      // And rename tmp file's name to old file name
      File newFile = new File(tmpFileName);
      newFile.renameTo(oldFile);
      
      return novo_count;
   }
    
    public static String readFromFile(String filename, String chunkNO) {
        try {
            // Open the file that is the first 
            // command line parameter
            FileInputStream fstream = new FileInputStream(filename);
            // Get the object of DataInputStream
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;
            //Read File Line By Line
            while ((strLine = br.readLine()) != null) {
                // Print the content on the console
                String[] data_parsed = strLine.split(" ");
                if(data_parsed[0].equalsIgnoreCase(chunkNO))
                    return data_parsed[1];
                
            }
            //Close the input stream
            in.close();
        } catch (Exception e) {//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }
        return "";
    }

    public static ArrayList xmlParser(String filename) throws ParserConfigurationException {

        ArrayList results = new ArrayList<String>();

        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(new File(filename));

            // normalize text representation
            doc.getDocumentElement().normalize();


            NodeList listOfSettings = doc.getElementsByTagName("Settings");
            int totalSettings = listOfSettings.getLength();

            for (int s = 0; s < listOfSettings.getLength(); s++) {


                Node firstSettingsNode = listOfSettings.item(s);
                if (firstSettingsNode.getNodeType() == Node.ELEMENT_NODE) {


                    Element firstSettingsElement = (Element) firstSettingsNode;

                    //-------
                    NodeList ipList = firstSettingsElement.getElementsByTagName("ipAddress");
                    Element ipElement = (Element) ipList.item(0);

                    NodeList textIPList = ipElement.getChildNodes();
                    results.add(((Node) textIPList.item(0)).getNodeValue().trim());

                    //-------
                    NodeList mcList = firstSettingsElement.getElementsByTagName("MC");
                    Element mcElement = (Element) mcList.item(0);

                    NodeList textMCList = mcElement.getChildNodes();
                    results.add(((Node) textMCList.item(0)).getNodeValue().trim());

                    //----
                    NodeList mdList = firstSettingsElement.getElementsByTagName("MDBackup");
                    Element mdElement = (Element) mdList.item(0);

                    NodeList textMDList = mdElement.getChildNodes();
                    results.add(((Node) textMDList.item(0)).getNodeValue().trim());

                    //------
                    NodeList mdRestoreList = firstSettingsElement.getElementsByTagName("MDRestore");
                    Element mdResElement = (Element) mdRestoreList.item(0);

                    NodeList textMDRList = mdResElement.getChildNodes();
                    results.add(((Node) textMDRList.item(0)).getNodeValue().trim());

                    //------
                    NodeList versionList = firstSettingsElement.getElementsByTagName("Version");
                    Element versionElement = (Element) versionList.item(0);

                    NodeList textVersionList = versionElement.getChildNodes();
                    results.add(((Node) textVersionList.item(0)).getNodeValue().trim());
                    
                    //-------
                    NodeList storageList = firstSettingsElement.getElementsByTagName("Storage");
                    Element storageElement = (Element) storageList.item(0);

                    NodeList storageVersionList = storageElement.getChildNodes();
                    results.add(((Node) storageVersionList.item(0)).getNodeValue().trim());

                }//end of if clause


            }//end of for loop with s var


        } catch (SAXParseException err) {
            System.out.println("** Parsing error" + ", line "
                    + err.getLineNumber() + ", uri " + err.getSystemId());
            System.out.println(" " + err.getMessage());

        } catch (SAXException e) {
            Exception x = e.getException();
            ((x == null) ? e : x).printStackTrace();

        } catch (Throwable t) {
            t.printStackTrace();
        }
        //System.exit (0);
        return results;
    }
}
