import java.io.*;
import java.net.*;

public class Client implements Runnable {

    private static Socket socketOfClient = null;
    private static ServerSocket serverSocket = null;
	private static ObjectOutputStream os = null;
	private static ObjectInputStream is = null;
	private static BufferedReader inputLine = null;
    private static boolean sign = false;
    private static FileRepository localRepository;
    
    public static void main(String[] args) {
        try {

            socketOfClient = new Socket("localhost", 9000);
            serverSocket = new ServerSocket(0);
            os = new ObjectOutputStream(socketOfClient.getOutputStream());
            is = new ObjectInputStream(socketOfClient.getInputStream());
            inputLine = new BufferedReader(new InputStreamReader(System.in));
            localRepository = new FileRepository();
            System.out.println("listening server on port: " + socketOfClient.getLocalPort() + ", listening peer connection on port: " + serverSocket.getLocalPort());

        } catch (UnknownHostException e) {
			System.err.println("UnknownHost");
		} catch (IOException e) {
			System.err.println("No Server found");
		}

        if (socketOfClient != null && serverSocket != null && os != null && is != null) {
            try {

                System.out.print("Enter your hostname: ");
                String hostname = inputLine.readLine().trim();
                
                os.writeObject(hostname);
                os.flush();
                
                os.writeObject(Integer.valueOf(serverSocket.getLocalPort()));
                os.flush();
                System.out.print("serverSocket: " + serverSocket.getInetAddress());
                
                os.writeObject(serverSocket.getInetAddress());
                os.flush();

                String responseLine = (String) is.readObject();
                System.out.println(responseLine);
                System.out.println("Follow these syntaxes:\n1. PUBLISH filename filepath\n2. FETCH filename\n3. quit: log out");

                new Thread(new Client()).start();
                while (!sign)
                {   
                    
                    System.out.print("Enter your command: ");
                    String cmd = inputLine.readLine().trim();
                
                    if (cmd.startsWith("PUBLISH"))
                    {
                        os.writeObject(cmd);
                        os.flush();
                        String[] data = cmd.split(" ");
                        publish(data[1], data[2]);
                        
                        responseLine = (String) is.readObject();
                        System.out.println(responseLine);
                    }
                    else if (cmd.startsWith("FETCH"))
                    {
                        os.writeObject(cmd);
                        os.flush();

                        String[] data = cmd.split(" ");
                        String filename = data[1];

                        responseLine = (String) is.readObject();
                        if (responseLine.equals("")) {
                            System.out.println("Sorry. No clients have got your requested file");
                            continue;
                        }
                        System.out.println(responseLine);


                        System.out.print("Select one hostname from hostname list above: ");
                        String targetClient = inputLine.readLine().trim();

                        os.writeObject(targetClient);
                        os.flush();

                        int portNum = ((Integer) is.readObject()).intValue();

                        InetAddress targetAddr = (InetAddress) is.readObject();

                        Socket peerClient = new Socket(targetAddr, portNum);
                        System.out.println("Hello : " + peerClient.getLocalPort());
                        ObjectInputStream peerIs = new ObjectInputStream(peerClient.getInputStream());
                        ObjectOutputStream peerOs = new ObjectOutputStream(peerClient.getOutputStream());
                        
                        System.out.println("you are here");
                        peerOs.writeObject(filename);
                        peerOs.flush();

                        byte[] fileContent = (byte[]) peerIs.readObject();
                        localRepository.files.put(filename, fileContent);
                        System.out.println(filename + " was downloaded successfully");

                        peerIs.close();
                        peerOs.close();
                        peerClient.close();
                    }
                    else
                    {
                        responseLine = (String) is.readObject();
                        System.out.println(responseLine);
                        if (responseLine.startsWith("Goodbye")) {
                            sign = true;
                        }
                    }
                }

                os.close();
                is.close();
                socketOfClient.close();

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

    }

    private static void publish(String fileName, String filePath) {
        localRepository.publish(fileName, filePath);
    }

    public void run() {
        try{
            while (!sign) {
                Socket peerServer = serverSocket.accept();
                peerConnection peerHandler = new peerConnection(peerServer);
                peerHandler.start();
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    class peerConnection extends Thread {
        private Socket peerServer;
        private ObjectInputStream is;
        private ObjectOutputStream os;
        public peerConnection(Socket peerServer) {
            this.peerServer = peerServer;
        }
        public void run() {
            try {
                System.out.println("i am here");
                is = new ObjectInputStream(peerServer.getInputStream());
                os = new ObjectOutputStream(peerServer.getOutputStream());
                System.out.println("you are here");
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                String requestedFile = (String) is.readObject();
                System.out.println("The requested file is " + requestedFile);
                byte[] fileContent = localRepository.getFileContent(requestedFile);

                os.writeObject(fileContent);
                os.flush();
                
                is.close();
                is.close();
                peerServer.close();

            } catch (IOException | ClassNotFoundException e) {
                System.err.print("A FileTransfer process went wrong");
            }
        }
    }
    
}