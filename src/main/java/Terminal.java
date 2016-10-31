import com.sun.corba.se.spi.servicecontext.SendingContextServiceContext;
import jdk.internal.org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import static java.lang.Integer.parseInt;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by Yasi on 10/29/2016.
 */
public class Terminal {
    private String localhost = "127.0.0.1";
    private Integer serverPORT ;
    private String terminalId;
    private String terminalType;
    private String outLogPath;
    public List<Transaction> transactions = new ArrayList<Transaction>();

    private void run() throws IOException {
        String serverAddress = localhost;//getServerAddress();
        Socket socket = new Socket(serverAddress, serverPORT);

        // Process all messages from server, according to the protocol
        DataInputStream din = new DataInputStream(socket.getInputStream());
        DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        ObjectOutputStream outStreamObject = new ObjectOutputStream(socket.getOutputStream());
        dout.writeUTF(terminalId);
log("terminal id = "+terminalId);
        System.out.println("Sending Request");
        for (Transaction transaction : transactions) {
            System.out.println(transaction.getType() + " - " + transaction.getId());
            outStreamObject.writeObject(transaction);
            //out.println("Sending");
            dout.writeUTF("Sending");
            System.out.println("Sending");
        }
        dout.writeUTF("AllDataSent");
        //out.println("AllDataSent");
        //Connect

        String str="",str2="";
        while(!str.equals("stop")){
            log("Terminal  listeninggggg: ");
            str=br.readLine();
            dout.writeUTF(str);
            dout.flush();
            log("Flushed: ");
            str2=din.readUTF();
            System.out.println("Server says: "+str2);
        }
        System.out.println("Stop listening: ");

    }

    void log(String string){
        System.out.println(string);
    }

    private void parseXML() {
        try {
            File inputFile;
            inputFile = new File("src/main/resources/terminal.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            terminalId = doc.getDocumentElement().getAttribute("id");
            terminalType = doc.getDocumentElement().getAttribute("type");
            NodeList server = doc.getElementsByTagName("server");
            serverPORT = parseInt(((Element)server.item(0)).getAttribute("port"));
            NodeList outlog = doc.getElementsByTagName("outLog");
            outLogPath = ((Element) outlog.item(0)).getAttribute("path");

            NodeList nList = doc.getElementsByTagName("transaction");
           for (int i = 0; i < nList.getLength(); i++) {
                Node nNode = nList.item(i);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    Transaction trnsaction = new Transaction(parseInt(eElement.getAttribute("id")),
                            eElement.getAttribute("type"),
                            parseInt(eElement.getAttribute("amount").replaceAll(",", "")),
                            eElement.getAttribute("deposit"));
                    transactions.add(trnsaction);
                }
            }
        }catch (ParserConfigurationException e){
            System.exit(1);
        } catch (org.xml.sax.SAXException e) {
            System.exit(1);
        } catch (IOException e) {
            System.exit(1);
        }
    }

    public static void main(String[] args) throws IOException {
        Terminal terminal = new Terminal();
        terminal.parseXML();
        terminal.run();
        System.out.println("Out of run ");
    }
}
