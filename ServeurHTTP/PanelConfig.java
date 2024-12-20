import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class PanelConfig extends JPanel {

    JTextField[] information = new JTextField[1];
    JLabel labelPort;
    JButton config = new JButton("Configurer");

    public PanelConfig(String txtPath) {
        labelPort = new JLabel("Port : ");
        information[0] = new JTextField(10);

        config.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                
                // Lire et modifier le contenu du fichier
                StringBuilder contenu = new StringBuilder();
                try (BufferedReader lecteur = new BufferedReader(new FileReader(txtPath))) {
                    String ligne;
                    while ((ligne = lecteur.readLine()) != null) {
                        // Vérifier si la ligne contient "Port:"
                        if (ligne.startsWith("Port:")) {
                            // Remplacer la valeur après "Port:"
                            ligne = "Port: " + information[0].getText();
                        }
                        contenu.append(ligne).append(System.lineSeparator());
                    }
                } catch (IOException ex) {
                    System.out.println("Erreur lors de la lecture du fichier : " + ex.getMessage());
                    return;
                }

                 // Réécrire le fichier avec les modifications
                try (BufferedWriter ecrivain = new BufferedWriter(new FileWriter(txtPath))) {
                    ecrivain.write(contenu.toString());
                    // System.out.println("Le numéro de port a été modifié avec succès.");
                } catch (IOException ex) {
                    System.out.println("Erreur lors de l'écriture du fichier : " + ex.getMessage());
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                
            }

            @Override
            public void mouseExited(MouseEvent e) {
                
            }

            @Override
            public void mousePressed(MouseEvent e) {
                
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                
            }
            
        });
        this.add(labelPort);
        this.add(information[0]);
        this.add(config);
    }
}
