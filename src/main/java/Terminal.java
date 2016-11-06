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
public class Terminal extends Thread {
    private String localhost = "127.0.0.1";
    private Integer serverPORT;
    private String terminalId;
    private String terminalType;
    private String outLogPath;
    public List<Transaction> transactions = new ArrayList<Transaction>();
    public List<String> logFile = new ArrayList<String>();

    public void run() {
        try {
            parseXML(Thread.currentThread().getName().toString());
            String serverAddress = localhost;//getServerAddress();
            Socket socket = new Socket(serverAddress, serverPORT);
            System.out.println("Run: ");

            // Process all messages from server
            DataInputStream inputDat = new DataInputStream(socket.getInputStream());
            DataOutputStream outputData = new DataOutputStream(socket.getOutputStream());
            BufferedReader inputBuffer = new BufferedReader(new InputStreamReader(System.in));
            ObjectOutputStream outStreamObject = new ObjectOutputStream(socket.getOutputStream());

            Integer temp = (transactions.size());
            String msg = temp.toString() + "," + terminalId;
            outputData.writeUTF(msg); //Write
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

            if ("received".equals(inputDat.readUTF())) { //Read
                for (int i = 0; i < transactions.size(); i++) {
                    log("thread name " + Thread.currentThread().getName());
                    outStreamObject.writeObject(transactions.get(i));//Write
                    Calendar cal = Calendar.getInstance();
                    logFile.add(dateFormat.format(cal.getTime()) + " Request for Deposit :" + transactions.get(i).getDeposit() + " --transaction-> type: " + transactions.get(i).getType()
                            + " # amount = " + transactions.get(i).getAmount());
                    String ack = inputDat.readUTF(); //Read
                    if (ack.startsWith(",")) {
                        transactions.get(i).setAmount(parseInt(ack.replaceAll(",", "")));
                        ack = ack.replaceAll(",", "New balance is ");
                    } else {
                        log("Error: " + ack);
                    }
                    logFile.add(dateFormat.format(cal.getTime()) + " Response -> " + ack);
                }
                /*
                for (Transaction transaction : transactions) {
                    System.out.println(transaction.getDeposit() + " --> " + transaction.getAmount());
                }
                */
                writeIntoAccessFile(outLogPath);
                writeInXMLFile();
                System.out.println("Stop listening: " + outLogPath);
            } else {
                //connection failled
            }
        } catch (IOException e) {
            System.out.println("Error : " + e.getMessage());
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
                    Transaction transaction = new Transaction(parseInt(eElement.getAttribute("id")),
                            eElement.getAttribute("type"),
                            parseInt(eElement.getAttribute("amount").replaceAll(",", "")),
                            eElement.getAttribute("deposit"));
                    transactions.add(transaction);
                }
            }
            log("XML FILE Successfully parsed..");
        } catch (ParserConfigurationException e) {
            System.out.println("Error : " + e.getMessage());
            System.exit(1);
        } catch (org.xml.sax.SAXException e) {
            System.out.println("Error : " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.out.println("Error : " + e.getMessage());
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

            int i = 0;
            for (String str : logFile) {
                raf.writeBytes("\n id = " + i);
                raf.writeBytes("\n  " + str);
                i++;
            }
            logFile.clear();
            raf.close();
        } catch (IOException e) {
            System.out.println("IOException:");
        }
    }

    private void writeInXMLFile() {
        DocumentBuilderFactory icFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder icBuilder;
        try {
            icBuilder = icFactory.newDocumentBuilder();
            Document doc = icBuilder.newDocument();
            Element mainRootElement = doc.createElement("terminal");
            mainRootElement.setAttribute("id", terminalId);
            mainRootElement.setAttribute("type", terminalType);
            doc.appendChild(mainRootElement);

            Element rootChilde = doc.createElement("transactions");
            mainRootElement.appendChild(rootChilde);

            // append child elements to root element
            for (Transaction transaction : transactions)
                rootChilde.appendChild(getTransaction(doc, transaction.getId().toString(), transaction.getDeposit(), transaction.getType(), transaction.getAmount().toString()));

            // output DOM XML to console
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(doc);
            //StreamResult console = new StreamResult(System.out);
            String name = "terminal" + terminalId + "Response.xml";
            StreamResult sr = new StreamResult(new File(name));
            transformer.transform(source, sr);
            System.out.println("\nXML DOM Created Successfully..");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Node getTransaction(Document doc, String id, String deposit, String type, String balance) {
        Element transaction = doc.createElement("transaction");
        transaction.setAttribute("type", type);
        transaction.setAttribute("deposit", deposit);
        transaction.setAttribute("id", id);
        transaction.setAttribute("balance", balance);
        return transaction;
    }

    public static void main(String[] args) throws IOException {
        new Thread(new Terminal(), "terminal").start();
        new Thread(new Terminal(), "terminal1").start();

        System.out.println("Terminal Stopped");
    }

    private void writeTest(String fileName) {
        DocumentBuilderFactory icFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder icBuilder;
        try {
            icBuilder = icFactory.newDocumentBuilder();
            Document doc = icBuilder.newDocument();
            Element mainRootElement = doc.createElement("terminal");
            mainRootElement.setAttribute("id", "21374");
            mainRootElement.setAttribute("type", "ATM");
            doc.appendChild(mainRootElement);

            Element server = doc.createElement("server");
            server.setAttribute("ip", "localhost");
            server.setAttribute("port", "8080");
            mainRootElement.appendChild(server);

            Element outlog = doc.createElement("outLog");
            outlog.setAttribute("path", "term21374.log");
            mainRootElement.appendChild(outlog);

            Element rootChilde = doc.createElement("transactions");
            mainRootElement.appendChild(rootChilde);

            // append child elements to root element
            for (Integer i = 1; i <= 1000; i++) {
                //rootChilde.appendChild(getTransaction(doc,i.toString(),"withdraw","1,000","33227781"));
                Element transaction = doc.createElement("transaction");
                transaction.setAttribute("id", i.toString());
                transaction.setAttribute("type", "withdraw");
                transaction.setAttribute("amount", "1,000");
                transaction.setAttribute("deposit", "33227781");
                rootChilde.appendChild(transaction);
            }
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
}
