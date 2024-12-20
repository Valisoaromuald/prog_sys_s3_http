import javax.swing.JFrame;
import javax.swing.JTabbedPane;

public class Fenetre  extends JFrame{
    
    public Fenetre(int width, int height, String txtPath)
    {
        JTabbedPane onglet = new JTabbedPane();

        Panel pan = new Panel(txtPath);
        PanelConfig panConf = new PanelConfig(txtPath);

        onglet.addTab("Controle", pan);
        onglet.addTab("Config", panConf);
        
        this.add(onglet);
        // this.add(pan);
        this.setTitle("Server HTTP");
        this.setSize(width, height);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setVisible(true);
    }

}
