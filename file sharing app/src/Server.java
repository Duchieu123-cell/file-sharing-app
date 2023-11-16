import java.io.*;
import java.net.*;
import java.util.ArrayList;


public class Server {
    
    private class ClientInfo {
        public final String hostname;
        public final int portNum;
        public final InetAddress addr;
        public final Socket socketOfServer;
        public ArrayList<String> listOfFiles;

        public ClientInfo(String hostname, int portNum, InetAddress addr, Socket socketOfServer) { 
            this.hostname = hostname;
            this.portNum = portNum;
            this.addr = addr;
            this.socketOfServer = socketOfServer;
            listOfFiles = new ArrayList<String>();
        }
    }
    

    private static ServerSocket serverSocket;
    private ArrayList<ClientInfo> listOfClients = new ArrayList<ClientInfo>();

    public void startServer() {
        try {

            System.out.println("Starting Server....");
            serverSocket = new ServerSocket(9000);
            System.out.println("Server Socket has been created. Server is running on port 9000");

        } catch (IOException e) {
			System.out.println("Server Socket can't be created");
		}
        
        try {
            while (true) {
                Socket socketOfServer = serverSocket.accept();
                Thread clientHandler = new Thread(() -> handleClient(socketOfServer));
                clientHandler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleClient(Socket socketOfServer) {
        try {
            ObjectInputStream is = new ObjectInputStream(socketOfServer.getInputStream());
            ObjectOutputStream os = new ObjectOutputStream(socketOfServer.getOutputStream());

            String hostname = (String) is.readObject();

            int portOfClientListener = ((Integer) is.readObject()).intValue();
            
            InetAddress addr = (InetAddress) is.readObject();
            System.out.println("hostname: " + hostname + ", its portOfClientListener: " + portOfClientListener + ", its sockerOfServer's portNum: " + socketOfServer.getLocalPort());
            listOfClients.add(new ClientInfo(hostname, portOfClientListener, addr, socketOfServer));

            os.writeObject("Your information has been recorded. From now on, you will operate under the name: " + hostname);
            os.flush();

            while (true)
            {
                String cmd = (String) is.readObject();
                if (cmd.startsWith("PUBLISH"))
                {   
                    System.out.println(hostname + " is conducting a publish cmd");
                    String[] data = cmd.split(" ");
                    String filename = data[1];
                    for (ClientInfo cur : listOfClients)
                    {
                        if (cur.hostname.equals(hostname) && cur.socketOfServer == socketOfServer)
                        {
                            cur.listOfFiles.add(filename);
                            break;
                        }
                    }
                    os.writeObject(filename + " was published successfully. This information has been recorded by the server.");
                    os.flush();

                }
                else if (cmd.startsWith("FETCH"))
                {
                    System.out.println(hostname + " is conducting a fetch cmd");
                    String[] data = cmd.split(" ");
                    String filename = data[1];

                    String list = SearchClients(filename);
                    os.writeObject(list);
                    os.flush();
                    if (list.equals("")) {
                        continue;
                    }

                    String targetClient = (String) is.readObject();
                    int portNum = getPort(targetClient);
                    InetAddress targetAddr = getAddr(targetClient);
                    System.out.print("targetClient to fetch: " + targetClient + ", its portNum: " + portNum + ", its InetAddress: " + targetAddr);
                    os.writeObject(Integer.valueOf(portNum));
                    os.flush();

                    os.writeObject(targetAddr);
                    os.flush();
                }
                else if (cmd.startsWith("/quit"))
                {
                    os.writeObject("Goodbye " + hostname + ". Thanks for taking your time to use our file sharing app.");
                    os.flush();
                    break;
                }
                else
                {
                    os.writeObject("Your command is invalid.");
                    os.flush();
                }
            }
            
            
            is.close(); 
            os.close();
            socketOfServer.close();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.out.println("Class Not Found.");
        } 

    }
    public String SearchClients(String requestedFile) {
        // Search for clients that have the requested file                          
        StringBuilder response = new StringBuilder("*********************************\nClients with the requested file:");
        boolean sign = false;
        for (ClientInfo cur : listOfClients) {
            if (cur.listOfFiles.indexOf(requestedFile) != -1) {
                response.append("\n- ").append(cur.hostname);
                sign = true;
            }
        } 
        response.append("\n*********************************");

        if (!sign) return "";
        
        return response.toString();
    }
    public int getPort(String hostname)
    {
        int portNum = 0;
        for (ClientInfo client : listOfClients) {
            if (client.hostname.equals(hostname)) {
                portNum = client.portNum;
            }
        }
        return portNum;
    }
    public InetAddress getAddr(String hostname) {
        InetAddress addr = null;
        for (ClientInfo client : listOfClients) {
            if (client.hostname.equals(hostname)) {
                addr = client.addr;
            }
        }
        return addr;
    }
    public static void main(String[] args){
        Server server = new Server();
        server.startServer();
    }
}