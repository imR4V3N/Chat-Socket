package chat;

import java.io.PrintStream;
import java.io.IOException;
import java.net.Socket;
import java.net.ServerSocket;

public class Server {
  
  private static ServerSocket serverSocket = null;
  private static Socket clientSocket = null;
  private static final int maxClientsCount = 5;
  private static final ClientHandler[] threads = new ClientHandler[maxClientsCount];

  public static void main(String args[]) {
    // Creation d'un variable portNumber pour stocker le port du serveur
    int portNumber = Integer.parseInt(args[0]);

    // Vérifie si un deuxième argument est fourni pour le numéro de port, 
    // sinon utilise le premier argument et l'afficher dans le terminal
    if (args.length < 2) {
      System.out.println("Le serveur tourne au port : " + portNumber);
    } else {
      portNumber = Integer.valueOf(args[0]).intValue();
    }

    // Crée une nouvelle instance de ServerSocket pour écouter les connexions
    try {
      serverSocket = new ServerSocket(portNumber);
    } catch (IOException e) {
      System.out.println(e);
    }

    while (true) {
      try {
        // Accepte une connexion cliente
        clientSocket = serverSocket.accept();

        // Recherche un emplacement disponible dans le tableau de threads pour gérer la nouvelle connexion
        int i = 0;
        for (i = 0; i < maxClientsCount; i++) {
          if (threads[i] == null) {

            // Crée un nouveau thread (ClientHandler) pour gérer la connexion
            (threads[i] = new ClientHandler(clientSocket, threads)).start();
            break;

          }
        }

        // Si le maximum de client est atteint, informer le client et fermer la connexion
        if (i == maxClientsCount) {
          PrintStream os = new PrintStream(clientSocket.getOutputStream());
          os.println("Le serveur est sature. Reessaye plus tard.");
          os.close();
          clientSocket.close();
        }
      } catch (IOException e) {
        System.out.println(e);
      }
    }
  }  
}