import Controllers.Controller;
import Models.Model;
import View.ClientPaiement;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import static java.lang.System.exit;

public class Main {
    public static void main(String[] args) {
        Security.addProvider(new BouncyCastleProvider()); //sinon exception pas de provider

        Model model = new Model(); // Créez une instance du modèle
        ClientPaiement view = new ClientPaiement(); // Créez une instance de la vue
        Controller controller = new Controller(model, view); // Créez une instance du contrôleur
        JFrame frame = new JFrame("Maraicher en ligne");
        frame.add(view.getPanel_principal());
        frame.pack();
        frame.setSize(850, 250);
        frame.setLocationRelativeTo(null);
        frame.addKeyListener(view);
        frame.setFocusable(true);
        frame.requestFocusInWindow();
        view.setMainWindow(frame);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                model.fermerApp();
                exit(0);
            }
        });
        frame.setVisible(true);
    }
}