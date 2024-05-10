import chat.*;
import javax.swing.*;

// La classe pour selectionner si on veux se connecter en tant que serveur ou client
public class Main {
	public static void main(String [] args){
		
		Object[] selectionValues = { "Server","Client"};
		String initialSection = "Server";
		
		Object selection = JOptionPane.showInputDialog(null, "Se connecte en tant que : ", "Chat Socket", JOptionPane.QUESTION_MESSAGE, null, selectionValues, initialSection);
		if(selection.equals("Server")){
			String port = JOptionPane.showInputDialog("Port du Server"); 
			String[] arguments = new String[] {port};
			new Server().main(arguments);

		}else if(selection.equals("Client")){
			String IPServer = JOptionPane.showInputDialog("Entez l'IP du Server");
			String portServer = JOptionPane.showInputDialog("Entrez le port du Server");
			String[] arguments = new String[] {IPServer,portServer};
			new Client().main(arguments);
		}	
	}
}
