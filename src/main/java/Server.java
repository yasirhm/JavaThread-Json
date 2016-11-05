/**
 * Created by Yasi on 10/29/2016.
 */

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

import static java.lang.Integer.parseInt;

public class Server {
    private static Integer PORT;
    // private Socket socket;
    private static HashSet<String> terminalsId = new HashSet<String>();
    public String outLog;
    public List<Deposit> deposits = new ArrayList<Deposit>();
    public List<String> logFile = new ArrayList<String>();

    public String getOutLog() {
        return outLog;
    }

    private class Handler extends Thread {
        private Socket socket;
        String terminalID;

        public Handler(Socket cs) {
            this.socket = cs;
        }
        public void run() {
            String cmd;
            Integer transactionSize;
            try {
                System.out.println("Server handler is running!");
                // Create character streams for the socket.
                BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
                //log("1");
                DataInputStream din = new DataInputStream(socket.getInputStream());
                //log("2");
                DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
                //log("3");
                ObjectInputStream inputStreamObject = new ObjectInputStream(socket.getInputStream());
                //log("4");

               // while (true) {
                    System.out.println("Here : ");
                    cmd = din.readUTF(); //Read
                    String[] words = cmd.split(",");
                    terminalID = words[1];
                    transactionSize = parseInt(words[0]);
                    log("New terminal "+terminalID);
                    if (terminalID == null) {
                        System.out.println("Break : ");
                        return;
                    }
                 //   synchronized (terminalsId) {
                        if (terminalsId.add(terminalID))
                            logFile.add("\nNew Terminal --> ID : "+ words[1]+" Socket : "+socket.getPort());
                        logFile.add(new java.util.Date().toString());
                        dout.writeUTF("received"); //Write

                  //      break;
                  //  }
               // }
                /* Accept messages from this terminal */
                while (!transactionSize.equals(0)) {
                    Transaction transaction = (Transaction) inputStreamObject.readObject();//Read
                    logFile.add("Received Request \n \t Deposit : "+transaction.getDeposit()
                            +" transaction type : "+transaction.getType()+" amount : "+transaction.getAmount() );

                   // System.out.println("Handler recieved : " + transaction.getType());
                    if (transaction == null)
                        return;
                    --transactionSize;
                    for (Deposit deposit : deposits) {
                        if (deposit.getId().equals(transaction.getDeposit())) {
                            try {
log("thread name: "+Thread.currentThread().getName());
                               // synchronized (this) {
                                    /*try {
                                        this.wait();
                                    } catch(InterruptedException e) {
                                    }*/
                                    if (transaction.getType().equals("deposit"))
                                        deposit.deposit(transaction.getAmount());
                                    else {
                                        deposit.withdraw(transaction.getAmount());
                                    }

                                    transaction.setAmount(deposit.getInitialBalance());
                              //  }
                                String msg = (deposit.getInitialBalance()).toString();//Write
                                dout.writeUTF(","+msg);
                                logFile.add("Response Sent \n \t Customer Name : "+deposit.getCustomer()+" type : "+transaction.getType()
                                        +" Balance : "+deposit.getInitialBalance()+"\n");
                            } catch (BalanceException e) {
                                dout.writeUTF(e.getMessage());
                                logFile.add("Response Sent \n \t Error :  "+e.getMessage()+"\n");
                                System.out.println("Error : " + e.getMessage());
                            }
                        }
                    }
                }
                writeIntoAccessFile(outLog);
                log("TERMINAL ID: " + terminalID);
                for (Deposit dep : deposits) {
                    System.out.println(dep.getId() + " --> " + dep.getInitialBalance());
                }
                /*
                String str = "", str2 = "";
                while (!str.equals("stop")) {
                    str = din.readUTF();
                    System.out.println("client " + terminalsId + " says: " + str);
                    System.out.println("Server  listeninggggg to : " + terminalsId);
                    str2 = in.readLine();
                    dout.writeUTF(str2);
                    System.out.println("flushed " + str2);
                    dout.flush();
                    System.out.println("flushed ");
                }
                */
            } catch (ClassNotFoundException e) {
                System.out.println(e.getMessage());
            } catch (IOException e) {
                System.out.println(e.getMessage());
            } finally {
                // This client is going down!  Remove its id
                if (terminalID != null) {
                    terminalsId.remove(terminalID);
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Connection with terminal# " + terminalID + " closed");
                }
            }
        }
        public void sendObject() {
            try {
                DataOutputStream dout = new DataOutputStream(socket.getOutputStream());
                dout.writeUTF("0");
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    void log(String string) {
        System.out.println(string);
    }

    private void readJSONFromFile() {
        @SuppressWarnings("unchecked")
        JSONParser parser = new JSONParser();
        try {
            Object reader = parser.parse(new FileReader("src/main/resources/core.json"));
            JSONObject jsonObject = (JSONObject) reader;
            Long port = (Long) jsonObject.get("port");
            PORT = port.intValue();
            outLog = (String) jsonObject.get("outLog");
            JSONArray depositArray = (JSONArray) jsonObject.get("deposits");
            for (Object obj : depositArray) {
                JSONObject jsonObj = (JSONObject) obj;
                Deposit deposit = new Deposit((String) jsonObj.get("customer"), (String) jsonObj.get("id"),
                        parseInt(((String) jsonObj.get("initialBalance")).replaceAll(",", "")),
                        new BigDecimal(((String) jsonObj.get("upperBound")).replaceAll(",", ""))
                );
                deposits.add(deposit);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void run() throws IOException {
        ServerSocket listener = new ServerSocket(PORT);
        try {
            while (true) {
               new Handler(listener.accept()).start();
                //new Thread(new Handler(listener.accept()), "Handler").start();
                //handler.start();
            }

        } finally {
            listener.close();
        }
    }

    private void writeIntoAccessFile(String fileName) {
        try {
            File file = new File(fileName);
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
           // raf.writeBytes("Terminal ID " + terminalId + "\n");
            raf.seek(file.length());

            //raf.writeBytes("\nThis will complete the Project.");
            for (String str : logFile) {
                raf.writeBytes("\n" + str);
            }
            raf.close();
            logFile.clear();
        } catch (IOException e) {
            System.out.println("IOException:");
        }
    }

    public static void main(String args[]) throws IOException {
        System.out.println("Server runing: ");
        Server server = new Server();
        server.readJSONFromFile();
        server.run();
        System.out.println("STopped: ");
        //server.writeIntoAccessFile(server.getOutLog());
    }

}
