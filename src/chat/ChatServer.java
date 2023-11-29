package chat;

import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.swing.*;

import encryption.Encryption;

import java.security.*;


public class ChatServer extends JFrame implements Runnable {

    private static final String RSA = "RSA";
    private Key privateKey;
    private JTextArea dialog = new JTextArea();
    private int clientCount = 0;
    private List<ClientHandler> clientHandlers;
    private static final int PORT = 9898;

    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            dialog.append("Chat Server started at " + new Date() + '\n');
            while (true) {
                Socket socket = serverSocket.accept();
                clientCount++;
                dialog.append("Starting thread for client " + clientCount + " at " + new Date() + '\n');

                InetAddress inetAddress = socket.getInetAddress();
                dialog.append("Client " + clientCount + "'s host name is " + inetAddress.getHostName() + '\n');
                dialog.append("Client " + clientCount + "'s IP Address is " + inetAddress.getHostAddress() + '\n');
                ClientHandler newClientHandler = new ClientHandler(socket, privateKey);
                clientHandlers.add(newClientHandler);
                new Thread(newClientHandler).start();
            }
        } catch (IOException ioe) {
            System.err.println(ioe.getMessage());
        } finally {
            clientHandlers.remove(this);
        }
    }

    class ClientHandler implements Runnable {
        private Socket socket;
        private Key privateKey;
        private Key communicationKey;
        private DataOutputStream outputToClient;
        private final int clientNo;

        public ClientHandler(Socket socket, Key privateKey) throws IOException {
            this.socket = socket;
            this.privateKey = privateKey;
            this.outputToClient = new DataOutputStream(socket.getOutputStream());
            this.clientNo = clientCount;
        }

        public void sendMessage(String message) {
            try {
                String encryptedResponse = Encryption.encrypt(this.communicationKey, message);
                this.outputToClient.writeUTF(encryptedResponse);
            } catch (Exception e) {
                System.err.println("Error sending message: " + e.getMessage());
            }
        }

        public void broadcastMessage(String responseMessage) {
            synchronized (clientHandlers) {
                for (ClientHandler clientHandler : clientHandlers) {
                    if (clientHandler != this) {
                        clientHandler.sendMessage(responseMessage);
                    }
                }
            }
        }

        public void run() {
            try {
                DataInputStream inputFromClient = new DataInputStream(socket.getInputStream());


                String initialMessage = inputFromClient.readUTF();
                if (!"HELLO".equals(initialMessage)) {
                    outputToClient.writeUTF("ERROR");
                    outputToClient.flush();
                    socket.close();
                    return;
                } else {
                    outputToClient.writeUTF("CONNECTED");
                    outputToClient.flush();
                }
                byte[] encryptedSeed = Base64.getDecoder().decode(inputFromClient.readUTF());
                byte[] AESSeed = Encryption.pkDecrypt(privateKey, encryptedSeed);
                communicationKey = Encryption.generateAESKey(AESSeed);
                System.out.println("communication key: " + communicationKey);
                while (true) {
                    String encryptedMessage = inputFromClient.readUTF();
                    String decryptedMessage = Encryption.decrypt(communicationKey, encryptedMessage);
                    String responseMessage = "Client " + clientNo + " sent: " + decryptedMessage;
                    dialog.append(responseMessage + '\n');
                    broadcastMessage(responseMessage);
                }
            } catch (IOException ioe) {
                System.err.println(ioe.getMessage());
            } catch (InvalidAlgorithmParameterException | NoSuchPaddingException | IllegalBlockSizeException |
                     NoSuchAlgorithmException | BadPaddingException | InvalidKeyException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public ChatServer() {

        super("Chat Server");
        try {
            privateKey = Encryption.readPrivateKey("group_chat_messenger//keypairs/pkcs8_key");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("problem loading private key: " + e.getMessage());
            System.exit(1);
        }
        clientHandlers = Collections.synchronizedList(new ArrayList<ClientHandler>());
        setSize(500, 700);
        setTitle("Chat Server");
        setLayout(new BorderLayout());

        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("File");
        JMenuItem runServer = new JMenuItem("Run Server");
        menu.add(runServer);
        JMenuItem exitApp = new JMenuItem("Exit");
        menu.add(exitApp);
        menuBar.add(menu);
        setJMenuBar(menuBar);

        JPanel dialogPanel = new JPanel();
        dialogPanel.setLayout(new BorderLayout());
        dialogPanel.setPreferredSize(new Dimension(500, 300));
        dialogPanel.setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));
        dialog.setEditable(false);
        JScrollPane dialogScrollPane = new JScrollPane(dialog);
        dialogPanel.add(dialogScrollPane, BorderLayout.CENTER);
        add(dialogPanel, BorderLayout.CENTER);

        runServer.addActionListener(e -> {
            new Thread(this).start();
        });

        exitApp.addActionListener(e -> {
            System.exit(0);
        });

        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    }

    public static void main(String[] args) {
        ChatServer chatServer = new ChatServer();
    }

}


