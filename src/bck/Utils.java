package bck;

//classe com funções utilizadas por outras classes
import java.io.BufferedReader;
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
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class Utils {

    static int flag_sending = 0;
    /* */

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
                System.out.println(strLine);
                String[] data_parsed = strLine.split(" ");
                if(data_parsed[1].equalsIgnoreCase(chunkNO))
                    return data_parsed[0];
                
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
            System.out.println("Root element of the doc is "
                    + doc.getDocumentElement().getNodeName());


            NodeList listOfSettings = doc.getElementsByTagName("Settings");
            int totalSettings = listOfSettings.getLength();
            System.out.println("Total no of settings : " + totalSettings);

            for (int s = 0; s < listOfSettings.getLength(); s++) {


                Node firstSettingsNode = listOfSettings.item(s);
                if (firstSettingsNode.getNodeType() == Node.ELEMENT_NODE) {


                    Element firstSettingsElement = (Element) firstSettingsNode;

                    //-------
                    NodeList ipList = firstSettingsElement.getElementsByTagName("ipAddress");
                    Element ipElement = (Element) ipList.item(0);

                    NodeList textIPList = ipElement.getChildNodes();
                    //System.out.println("IP : "
                      //      + ((Node) textIPList.item(0)).getNodeValue().trim());
                    results.add(((Node) textIPList.item(0)).getNodeValue().trim());

                    //-------
                    NodeList mcList = firstSettingsElement.getElementsByTagName("MC");
                    Element mcElement = (Element) mcList.item(0);

                    NodeList textMCList = mcElement.getChildNodes();
                    //System.out.println("MC : "
                      //      + ((Node) textMCList.item(0)).getNodeValue().trim());
                    results.add(((Node) textMCList.item(0)).getNodeValue().trim());

                    //----
                    NodeList mdList = firstSettingsElement.getElementsByTagName("MDBackup");
                    Element mdElement = (Element) mdList.item(0);

                    NodeList textMDList = mdElement.getChildNodes();
                    //System.out.println("MDBackup : "
                      //      + ((Node) textMDList.item(0)).getNodeValue().trim());
                    results.add(((Node) textMDList.item(0)).getNodeValue().trim());

                    //------
                    NodeList mdRestoreList = firstSettingsElement.getElementsByTagName("MDRestore");
                    Element mdResElement = (Element) mdRestoreList.item(0);

                    NodeList textMDRList = mdResElement.getChildNodes();
                    //System.out.println("MDRestore : "
                        //    + ((Node) textMDRList.item(0)).getNodeValue().trim());
                    results.add(((Node) textMDRList.item(0)).getNodeValue().trim());

                    //------
                    NodeList versionList = firstSettingsElement.getElementsByTagName("Version");
                    Element versionElement = (Element) versionList.item(0);

                    NodeList textVersionList = versionElement.getChildNodes();
                    //System.out.println("Version : "
                      //      + ((Node) textVersionList.item(0)).getNodeValue().trim());
                    results.add(((Node) textVersionList.item(0)).getNodeValue().trim());

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
