package chat;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FileWriter;
import java.io.BufferedWriter;

public class ClientHandler extends Thread {
  private String clientName = null;
  private DataInputStream is = null;
  private PrintStream os = null;
  private BufferedWriter bufferedWriter = null;
  private Socket clientSocket = null;
  private final ClientHandler[] threads;
  private int maxClientsCount;

  // Constructeur pour initialiser les propriétés de la classe
  public ClientHandler(Socket clientSocket, ClientHandler[] threads) {
    this.clientSocket = clientSocket;
    this.threads = threads;
    maxClientsCount = threads.length;
  }

  // Méthode d'excecution quand le thread est démarré
  public void run() {
    int maxClientsCount = this.maxClientsCount;
    ClientHandler[] threads = this.threads;

    try {

      // Initialise les flux d'entrée et de sortie pour la communication avec le client
      is = new DataInputStream(clientSocket.getInputStream());
      os = new PrintStream(clientSocket.getOutputStream());
      
      String name;

      // Invite le client à entrer son nom jusqu'à ce qu'un nom valide soit fourni
      while (true) {
        os.println("Entrez votre nom.");
        name = is.readLine().trim();
        if (name.indexOf('@') == -1) {
          break;
        } else {
          os.println("Le nom ne peut pas contenir la caractere '@'.");
        }
      }

      // Accueillir le client et l'informer sur la façon de quitter le chat
      os.println("Bienvenue " + name + ".\nPour quitter, entrez /quit.");

      // Synchronisation pour éviter les conflits lors de la mise à jour de la liste des clients
      synchronized (this) {

        // Enregistre le nom du client dans le format "@nom"
        for (int i = 0; i < maxClientsCount; i++) {
          if (threads[i] != null && threads[i] == this) {
            clientName = "@" + name;
            break;
          }
        }

        // Diffuse le message à tous les autres clients que celui-ci a rejoint le chat
        for (int i = 0; i < maxClientsCount; i++) {
          if (threads[i] != null && threads[i] != this) {
            threads[i].os.println(name + " a rejoint le chat.");
          }
        }
      }

      // Boucle principale pour la communication avec le client
      while (true) {
        String line = is.readLine();

        if (line.startsWith("/quit")) {
          break;
        }

        // Gerer l'envoie de fichier a tous les clients
        if (line.indexOf("FILE:") != -1) {
          synchronized (this) {
            for (int i = 0; i < maxClientsCount; i++) {
              if (threads[i] != null && threads[i].clientName != null) {
                
                // Nom du fichier
                String fileName = line.split("FILE:")[0];

                // Contenu du fichier
                String fileContent = line.split("FILE:")[1];
                
                try {
                    File receivedFile = new File("file/" + fileName);
                    FileWriter writer = new FileWriter(receivedFile, true);
                    threads[i].bufferedWriter = new BufferedWriter(writer);
                    
                    threads[i].bufferedWriter.write(fileContent);
                    
                    bufferedWriter.close();
                    writer.close();
                } catch (IOException ex) {
                    System.out.println(ex.getMessage());
                }

                threads[i].os.println(name + " : a envoye le fichier " + fileName );

              }
            }
          }
        }

        // Gestion des messages privés débutant par "@"
        else if (line.startsWith("@")) {
          String[] words = line.split("\\s", 2);
          if (words.length > 1 && words[1] != null) {
            words[1] = words[1].trim();
            if (!words[1].isEmpty()) {
              synchronized (this) {

                // Envoie le message privé à un client spécifique
                for (int i = 0; i < maxClientsCount; i++) {
                  if (threads[i] != null && threads[i] != this
                      && threads[i].clientName != null
                      && threads[i].clientName.equals(words[0])) {
                        
                    threads[i].os.println(name + " : " + words[1]);
                    
                    this.os.println(name + " : " + words[1]);
                    break;
                  }
                }
              }
            }
          }

        } else {

          // Diffuse le message à tous les clients
          synchronized (this) {
            for (int i = 0; i < maxClientsCount; i++) {
              if (threads[i] != null && threads[i].clientName != null) {
                threads[i].os.println(name + " : " + line);
              }
            }
          }
        }
      }

      // Gestion de la sortie du chat
      synchronized (this) {
        for (int i = 0; i < maxClientsCount; i++) {
          if (threads[i] != null && threads[i] != this && threads[i].clientName != null) {
            threads[i].os.println(name + " a quitte le chat.");
          }
        }
      }
      os.println("Bye " + name + ".");

      // Libération de l'emplacement du thread dans le tableau
      synchronized (this) {
        for (int i = 0; i < maxClientsCount; i++) {
          if (threads[i] == this) {
            threads[i] = null;
          }
        }
      }

      // Fermeture des flux et de la connexion du client
      is.close();
      os.close();
      clientSocket.close();

    } catch (IOException e) {
        System.out.println(e.getMessage());
    }
  }
}
