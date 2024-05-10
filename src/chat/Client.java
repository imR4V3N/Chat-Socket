package chat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Observable;
import java.util.Observer;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class Client {

    // Cette classe gère l'accès au chat (Communication avec le serveur)
    static class ChatAccess extends Observable {
        private Socket socket;
        private OutputStream outputStream;

        @Override
        public void notifyObservers(Object arg) {
            super.setChanged();
            super.notifyObservers(arg);
        }

        // Méthode pour initialiser la communication avec le serveur
        public void getMessage(String server, int port) throws IOException {
            socket = new Socket(server, port);
            outputStream = socket.getOutputStream();

            // Thread pour recevoir les messages du serveur en continu
            Thread receivingThread = new Thread() {
                @Override
                public void run() {
                    try {
                        BufferedReader reader = new BufferedReader( new InputStreamReader(socket.getInputStream()) );
                        String line;
                        while ((line = reader.readLine()) != null){
                            notifyObservers(line);
                        }
                    } catch (IOException ex) {
                        notifyObservers(ex);
                    }
                }
            };
            receivingThread.start();
        }

        private static final String CRLF = "\r\n"; 

        // Méthode pour envoyer un message au serveur
        public void sendMessage(String text) {
            try {
                outputStream.write((text + CRLF).getBytes());
                outputStream.flush();
            } catch (IOException ex) {
                notifyObservers(ex);
            }
        }

        // Méthode pour envoyer un fichier au serveur
        public void sendFile(java.io.File file) {
            try {
                java.nio.file.Path filePath = file.toPath();
                byte[] fileBytes = java.nio.file.Files.readAllBytes(filePath);

                String fileHeader = file.getName() + " FILE: ";

                outputStream.write(fileHeader.getBytes());

                outputStream.write(fileBytes);

                String fileFooter = "\n";

                outputStream.write(fileFooter.getBytes());

                outputStream.flush();
            } catch (IOException ex) {
                notifyObservers(ex);
            }
        }       

        // Méthode pour fermer la connexion
        public void close() {
            try {
                socket.close();
            } catch (IOException ex) {
                notifyObservers(ex);
            }
        }
    }

    // Cette classe représente la fenêtre graphique du client
    static class ChatFrame extends JFrame implements Observer {

        private JTextArea textArea;
        private JTextField inputTextField;
        private JButton sendButton;
        private JButton fileButton;
        private ChatAccess chatAccess;

        // Constructeur prenant un accès au chat en paramètre
        public ChatFrame(ChatAccess chatAccess) {
            this.chatAccess = chatAccess;
            // Ajoute cette fenêtre comme observateur
            chatAccess.addObserver(this);
            // Initialise l'interface graphique
            chatDesign();
        }

        // Méthode pour concevoir l'interface graphique de la fenêtre du chat
        private void chatDesign() {
            textArea = new JTextArea(20, 50);
            textArea.setEditable(false);
            textArea.setLineWrap(true);
            add(new JScrollPane(textArea), BorderLayout.CENTER);

            Box box = Box.createHorizontalBox();
            add(box, BorderLayout.SOUTH);
            inputTextField = new JTextField();
            sendButton = new JButton("Send");
            fileButton = new JButton("file");
            box.add(fileButton);
            box.add(inputTextField);
            box.add(sendButton);

            // ActionListener pour le bouton "Send" et champ de texte pour l'envoie de message
            ActionListener sendListener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String str = inputTextField.getText();
                    if (str != null && str.trim().length() > 0){
                        // Envoie le message au serveur 
                        chatAccess.sendMessage(str);
                    }
                    inputTextField.selectAll();
                    inputTextField.requestFocus();
                    inputTextField.setText("");
                }
            };

            // Ajoute les listeners aux composants
            inputTextField.addActionListener(sendListener);
            sendButton.addActionListener(sendListener);

            // ActionListener pour le bouton "fileButton" pour l'envoie de fichier 
            fileButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {

                    // Ouvre une boîte de dialogue pour choisir un fichier
                    JFileChooser fileChooser = new JFileChooser();
                    FileNameExtensionFilter filter = new FileNameExtensionFilter("Fichiers textes", "txt");
                    fileChooser.setFileFilter(filter);

                    int returnVal = fileChooser.showOpenDialog(ChatFrame.this);

                    // Si un fichier est sélectionné, envoie le fichier au serveur
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        java.io.File file = fileChooser.getSelectedFile();
                        if (file != null && file.toString().trim().length() > 0) {
                            chatAccess.sendFile(file);
                        }
                    }
                }
            });

            // Listener pour la fermeture de la fenêtre
            this.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    // Ferme la connexion lorsque la fenêtre est fermée
                    chatAccess.close();
                }
            });
        }

        // Méthode appelée lorsqu'une mise à jour est reçue de l'Observable (ChatAccess)
        public void update(Observable o, Object arg) {
            final Object finalArg = arg;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    textArea.append(finalArg.toString());
                    textArea.append("\n");
                }
            });
        }
    }

    // Méthode principale du programme
    public static void main(String[] args) {
        String server = args[0];
        int port = Integer.parseInt(args[1]);
        
        // Crée une instance de ChatAccess
        ChatAccess access = new ChatAccess();

        // Crée la fenêtre du chat avec l'accès au chat en paramètre
        JFrame frame = new ChatFrame(access);
        frame.setTitle("Connecte au " + server + ", port : " + port);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(true);
        frame.setVisible(true);

        try {
            // Initialise la communication avec le serveur
            access.getMessage(server, port);
        } catch (IOException ex) {
            System.out.println("Ne peut pas se connecte au " + server + ", port : " + port);
            ex.printStackTrace();
            System.exit(0);
        }
    }
}