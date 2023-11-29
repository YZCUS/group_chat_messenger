package chat;

import encryption.Encryption;

import javax.swing.*;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.security.Key;
import java.security.PublicKey;
import java.util.Base64;


public class ChatClient extends JFrame {

    private static final String RSA = "RSA";
    private static final String SERVER_PUBLIC_KEY = "MIGeMA0GCSqGSIb3DQEBAQUAA4GMADCBiAKBgGk9wUQ4G9PChyL5SUkCyuHjTNOglEy5h4KEi0xpgjxi/UbIH27NXLXOr94JP1N5pa1BbaVSxlvpuCDF0jF9jlZw5IbBg1OW2R1zUACK+NrUIAYHWtagG7KB/YcyNXHOZ6Icv2lXXd7MbIao3ShrUVXo3u+5BJFCEibd8a/JD/KpAgMBAAE=";
    private PublicKey serverPublicKey;
    private Key communicationKey;

    public ChatClient() {
        super("Chat Client");
        try {
            serverPublicKey = Encryption.readPublicKey(SERVER_PUBLIC_KEY);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("error getting server public key: " + e.getMessage());
        }
        setSize(500, 700);
        setTitle("Chat Client");
        setLayout(new BorderLayout());

        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("File");
        JMenuItem connectServer = new JMenuItem("Connect");
        menu.add(connectServer);
        JMenuItem exitApp = new JMenuItem("Exit");
        menu.add(exitApp);
        menuBar.add(menu);
        setJMenuBar(menuBar);

        JPanel displayPanel = new JPanel();
        displayPanel.setLayout(new BorderLayout());
        displayPanel.setPreferredSize(new Dimension(500, 300));
        displayPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        JTextArea display = new JTextArea();
        display.setLineWrap(true);
        display.setWrapStyleWord(true);
        display.setEditable(false);
        JScrollPane displayScrollPane = new JScrollPane(display);
        displayPanel.add(displayScrollPane, BorderLayout.CENTER);
        add(displayPanel, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BorderLayout());
        inputPanel.setPreferredSize(new Dimension(500, 50));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20));
        JTextField input = new JTextField();
        inputPanel.add(input, BorderLayout.CENTER);
        JButton sendButton = new JButton("Send");
        inputPanel.add(sendButton, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

        connectServer.addActionListener(e -> {
            try {
                Socket socket = new Socket("localhost", 9898);
                DataInputStream inputFromServer = new DataInputStream(socket.getInputStream());
                DataOutputStream outputToServer = new DataOutputStream(socket.getOutputStream());
                String initialGreetingMessage = "HELLO";
                outputToServer.writeUTF(initialGreetingMessage);
                outputToServer.flush();
                String connectedMessage = inputFromServer.readUTF();
                if ("CONNECTED".equals(connectedMessage)) {
                    display.append("Connected To Server\n");
                    byte[] AESSeed = Encryption.generateSeed();
                    communicationKey = Encryption.generateAESKey(AESSeed);
                    byte[] encryptedAESSeed = Encryption.pkEncrypt(serverPublicKey, AESSeed);
                    outputToServer.writeUTF(Base64.getEncoder().encodeToString(encryptedAESSeed));
                    outputToServer.flush();
                } else {
                    display.append("Error connecting to server\n");
                    socket.close();
                }
                sendButton.addActionListener(sent -> {
                    String inputMessage = input.getText();
                    if (!inputMessage.isEmpty()) {
                        try {
                            String encryptedMessage = Encryption.encrypt(communicationKey, inputMessage);
                            display.append("Me: " + inputMessage + "\n");
                            input.setText("");
                            outputToServer.writeUTF(encryptedMessage);
                            outputToServer.flush();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            System.err.println("error encrypting message: " + ex.getMessage());
                        }
                    }
                });
                new Thread(() -> {
                    try {
                        while (true) {
                            String encryptedMessage = inputFromServer.readUTF();
                            String decryptedMessage = Encryption.decrypt(communicationKey, encryptedMessage);
                            display.append(decryptedMessage + "\n");
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        System.err.println("error connecting to server: " + ex.getMessage());
                    }
                }).start();

            } catch (Exception ex) {
                ex.printStackTrace();
                System.err.println("error connecting to server: " + ex.getMessage());
            }

        });

        exitApp.addActionListener(e -> {
            System.exit(0);
        });


        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }


    public static void main(String[] args) {
        ChatClient chatClient = new ChatClient();
    }
}
