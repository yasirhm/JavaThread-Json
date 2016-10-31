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

public class Server{
    private static Integer PORT ;
    private static HashSet<String> terminalsId = new HashSet<String>();
    public String outLog;
    public List<Deposit> deposits = new ArrayList<Deposit>();

    private class Handler extends Thread{
        private Socket socket ;
        String terminalID;
        public Handler( Socket cs) {this.socket = cs;}
        public void run() {
            String cmd;
            try {
                System.out.println("Server handler is running!");
                // Create character streams for the socket.
                BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
                DataInputStream din=new DataInputStream(socket.getInputStream());
                DataOutputStream dout=new DataOutputStream(socket.getOutputStream());

                ObjectInputStream obj = new ObjectInputStream(socket.getInputStream());
                //InputStream is = s.getInputStream();
               // ObjectInputStream ois = new ObjectIn
                // putStream(is);

                while (true) {
                    terminalID = din.readUTF();
                    System.out.println("client ID: "+terminalID);
                    if (terminalID == null) {
                        return;
                    }
                    synchronized (terminalsId) {
                        terminalsId.add(terminalID);
                        break;
                    }
                }
                // Accept messages from this client
                while(true){
                    try{
                        System.out.println("Here : ");
                        if (in.readLine() == "AllDataSent") {
                            log("ALL DATA SENR");
                            break;
                        }
                        else {
                            Transaction transaction = (Transaction) obj.readObject();
                            System.out.println("Handler recieved : " + transaction.getType());
                            if (transaction == null)
                                return;
                            synchronized (transaction) {
                                for (Deposit deposit : deposits) {
                                    if (deposit.getId().equals(transaction.getDeposit())) {
                                        Method method = deposit.getClass().getMethod(transaction.getType(), Integer.class);
                                        log("method name : " + method.getName());
                                        method.invoke(deposit, parseInt(transaction.getAmount()));
                                    }
                                }

                            }
                        }
                    }catch (ClassNotFoundException e){
                        System.out.println(e);
                    } catch(NoSuchMethodException e) {
                        System.out.println(e);
                    }catch(InvocationTargetException e) {
                        System.out.println(e);
                    }catch (IllegalAccessException e){
                        System.out.println(e);
                    }

                }
                String str="",str2="";
                while(!str.equals("stop")){
                    str=din.readUTF();
                    System.out.println("client "+terminalsId+" says: "+str);
                    System.out.println("Server  listeninggggg to : "+terminalsId);
                    str2=in.readLine();
                    dout.writeUTF(str2);
                    System.out.println("flushed "+str2);
                    dout.flush();
                    System.out.println("flushed ");
                }
                //System.out.println("Stop listening: ");
            } catch (IOException e) {
                System.out.println(e);
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
    }
    void log(String string){
        System.out.println(string);
    }
    private void readJSONFromFile(){
        @SuppressWarnings("unchecked")
            JSONParser parser = new JSONParser();
            try {
                Object reader = parser.parse(new FileReader("src/main/resources/core.json"));
                JSONObject jsonObject = (JSONObject) reader;
                Long port = (Long) jsonObject.get("port");
                PORT = port.intValue();
                outLog = (String) jsonObject.get("outLog");
                JSONArray depositArray = (JSONArray) jsonObject.get("deposits");
                for(Object obj  :depositArray){
                    JSONObject jsonObj = (JSONObject) obj;
                    Deposit deposit = new Deposit((String) jsonObj.get("customer"),(String) jsonObj.get("id"),
                             parseInt(((String) jsonObj.get("initialBalance")).replaceAll(",", "")),
                            new BigDecimal(((String) jsonObj.get("upperBound")).replaceAll(",", ""))
                            );
                    deposits.add(deposit);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

    }
    public void run()throws IOException{
        ServerSocket listener = new ServerSocket(PORT);
        try {
            while (true) {
                new Handler(listener.accept()).start();
                System.out.println("out of handler ");
            }
        } finally {
            listener.close();
        }
    }

    public static void main(String args[]) throws IOException {
        System.out.println("Server runing: ");
        Server server = new Server();
        server.readJSONFromFile();
        server.run();
    }

}
