import com.sun.corba.se.spi.servicecontext.SendingContextServiceContext;
import jdk.internal.org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import static java.lang.Integer.parseInt;

import java.io.*;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


/**
 * Created by Yasi on 10/29/2016.
 */
public class Terminal {
    private String localhost = "127.0.0.1";
    private Integer serverPORT;
    private String terminalId;
    private String terminalType;
    private String outLogPath;
    public List<Transaction> transactions = new ArrayList<Transaction>();
    public List<String> logFile = new ArrayList<String>();

    private void run() throws IOException {
        String serverAddress = localhost;//getServerAddress();
        Socket socket = new Socket(serverAddress, serverPORT);
        String ack;
        System.out.println("Run: ");

        // Process all messages from server, according to the protocol
        DataInputStream din = new DataInputStream(socket.getInputStream());
        System.out.println("1: ");
        DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
        System.out.println("2: ");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("3: ");
        ObjectOutputStream outStreamObject = new ObjectOutputStream(socket.getOutputStream());

        System.out.println("4: ");
        System.out.println("befor try: ");

        Integer x = (transactions.size());
        String msg = x.toString() + "," + terminalId;
        dout.writeUTF(msg); //Write
        log("terminal id = " + terminalId);
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        if ("received".equals(din.readUTF())) { //Read
            for (int i = 0; i < transactions.size(); i++) {
                System.out.println(transactions.get(i).getType() + " - " + transactions.get(i).getId());
                outStreamObject.writeObject(transactions.get(i));//write

                Calendar cal = Calendar.getInstance();
                //System.out.println(dateFormat.format(cal.getTime())); //2014/08/06 16:00:22

                logFile.add(dateFormat.format(cal.getTime()) + " Request for Deposit :" + transactions.get(i).getDeposit() + " --transaction-> type: " + transactions.get(i).getType()
                        + " # amount = " + transactions.get(i).getAmount()
                );

                ack = din.readUTF();
                if (ack.startsWith(",")) {
                    log("ackkk : " + ack.replaceAll(",", ""));

                    transactions.get(i).setAmount(parseInt(ack.replaceAll(",", "")));
                    ack = ack.replaceAll(",", "New amount is ");
                   // log("received trans : " + parseInt(ack));
                } else {
                    log("Error: " + ack);
                }

                logFile.add(dateFormat.format(cal.getTime()) + " Response -> "+ ack);
                //log("11111received trans : "+receivedTrans.getAmount());
                        /*
                        if (!receivedTrans.getAmount().equals(0)) {
                            transactions.get(i).setAmount(receivedTrans.getAmount());
                            log("received trans : "+receivedTrans.getAmount());
                        }
                    } catch (ClassNotFoundException e) {
                        System.out.println(e);
                    }
                    */
                log("forrr " + i);
            }
            //Connect
            writeIntoAccessFile(outLogPath);
            for (Transaction transaction : transactions) {
                System.out.println(transaction.getDeposit() + " --> " + transaction.getAmount());
            }
            /*
            String str = "", str2 = "";
            while (!str.equals("stop")) {
                log("Terminal  listeninggggg: ");
                str = br.readLine();
                dout.writeUTF(str);
                dout.flush();
                log("Flushed: ");
                str2 = din.readUTF();
                System.out.println("Server says: " + str2);
            }
            */
            System.out.println("Stop listening: ");

        } else {
            //connection failled
        }
    }

    void log(String string) {
        System.out.println(string);
    }

    private void parseXML(String terminalName) {
        try {
            File inputFile;
            inputFile = new File("src/main/resources/" + terminalName + ".xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();

            terminalId = doc.getDocumentElement().getAttribute("id");
            terminalType = doc.getDocumentElement().getAttribute("type");
            NodeList server = doc.getElementsByTagName("server");
            serverPORT = parseInt(((Element) server.item(0)).getAttribute("port"));
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
        } catch (ParserConfigurationException e) {
            System.exit(1);
        } catch (org.xml.sax.SAXException e) {
            System.exit(1);
        } catch (IOException e) {
            System.exit(1);
        }
    }

    private void writeIntoAccessFile(String fileName) {
        try {
            File file = new File(fileName);
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            raf.writeBytes("Terminal ID " + terminalId + "\n");
            raf.seek(file.length());
            raf.writeBytes("\n");
            //raf.writeBytes("\nThis will complete the Project.");
            for (String str : logFile) {
                raf.writeBytes("\n" + str);
            }
            raf.close();
        } catch (IOException e) {
            System.out.println("IOException:");
        }
    }

    private void writeInXMLFile(String fileName) {
        DocumentBuilderFactory icFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder icBuilder;
        try {
            icBuilder = icFactory.newDocumentBuilder();
            Document doc = icBuilder.newDocument();
            Element mainRootElement = doc.createElementNS("DotinSchool", "terminal");
            mainRootElement.setAttribute("id",terminalId);
            mainRootElement.setAttribute("type",terminalType);
            doc.appendChild(mainRootElement);

            Element rootChilde = doc.createElement("transactions");
            mainRootElement.appendChild(rootChilde);

            // append child elements to root element
            for (Transaction transaction: transactions)
                mainRootElement.appendChild(getTransaction(doc, transaction.getId().toString(), transaction.getDeposit(), transaction.getType(), transaction.getAmount().toString()));

            // output DOM XML to console
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(doc);
            //StreamResult console = new StreamResult(System.out);
            StreamResult sr = new StreamResult(new File(fileName));
            transformer.transform(source, sr);

            System.out.println("\nXML DOM Created Successfully..");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Node getTransaction(Document doc, String id, String deposit, String type, String balance) {
        Element transaction = doc.createElement("transaction");
        transaction.setAttribute("deposit", deposit);
        transaction.setAttribute("type", type);
        transaction.setAttribute("balance", balance);
        transaction.setAttribute("id", id);
        return transaction;
    }

    public static void main(String[] args) throws IOException {
        Terminal terminal = new Terminal();
        terminal.parseXML("terminal");
        terminal.run();
        terminal.writeInXMLFile("response.xml");
        //terminal.writeIntoAccessFile("");

        Terminal terminal1 = new Terminal();
        terminal1.parseXML("terminal1");
        terminal1.run();
        terminal1.writeInXMLFile("response1.xml");
        System.out.println("Out of run ");
    }
}
