import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

public class Panel extends JPanel {

    int port = 0;
    List<String> Information = new ArrayList<>();
    ServerSocket serverSocket = null;
    boolean running = false;

    JButton start = new JButton("START");
    JButton stop = new JButton("STOP");
    JLabel statusLabel = new JLabel("Server is stopped");
    JTextArea console = new JTextArea(10, 30);
    JScrollPane scrollPane = new JScrollPane(console);

    public Panel(String txtPath) {

        try {
            Information = ReadFile(txtPath);
            port = Integer.parseInt(getInformation("Port", Information));
            this.serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.setBackground(Color.white);
        this.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 20));
        this.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel portText = new JLabel("Port : " + port);
        portText.setFont(new Font("Arial", Font.BOLD, 14));

        start.setBackground(new Color(60, 179, 113));
        start.setForeground(Color.white);
        stop.setBackground(new Color(220, 20, 60));
        stop.setForeground(Color.white);

        statusLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        console.setEditable(false);
        console.setFont(new Font("Monospaced", Font.PLAIN, 12));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    
        start.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!running) {
                    running = true;
                    Information = ReadFile(txtPath);
                    port = Integer.parseInt(getInformation("Port", Information));
                    portText.setText("Port : " + String.valueOf(port));
                    statusLabel.setText("Server tourne sur le port: " + port);
                    appendToConsole("Server tourne sur le port: " + port);
      
                    new Thread(() -> {
                        while (running) {
                            try {
                                if (serverSocket.isClosed()) {
                                    portText.setText("Port : " + String.valueOf(port));
                                    serverSocket = new ServerSocket(port);
                                }
                                Socket socket = serverSocket.accept();
                                appendToConsole("Connection accepté: " + socket);
                                new HttpHandler(socket).start();
                            } catch (IOException ex) {
                                if (running) {
                                    ex.printStackTrace();
                                    appendToConsole("Erreur: " + ex.getMessage());
                                }
                            }
                        }
                    }).start();
                    System.out.println("Start !");
                }
            }
        });

        stop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (running) {
                    running = false;
                    portText.setText("Port : " + String.valueOf(port));
                    try {
                        if (serverSocket != null && !serverSocket.isClosed()) {
                            serverSocket.close();
                        }
                        statusLabel.setText("Server a été eteint avec succès");
                        appendToConsole("Server eteint");
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        appendToConsole("Erreur: " + ex.getMessage());
                    }
                    System.out.println("Stop !");
                }
            }
        });        

        this.add(portText);
        this.add(start);
        this.add(stop);
        this.add(statusLabel);
        this.add(scrollPane);
    }

    private void appendToConsole(String message) {
        SwingUtilities.invokeLater(() -> {
            console.append(message + "\n");
            console.setCaretPosition(console.getDocument().getLength());
        });
    }

        //Lire le txt donnéee
    public static List<String> ReadFile(String txtPath)
    {
        List<String> Information = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(txtPath))) {
            String line;
            while ((line = br.readLine()) != null) {
                // System.out.println(line);
                Information.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Information;
    }

    // Avoir l'information depuis le nom de la config
    public static String getInformation(String nomConfig, List<String> Information)
    {
        for(String str : Information)
        {
            if(str.startsWith(nomConfig))
            {
                return str.split(":")[1].trim();
            }
        }

        return "Erreur";
    }

}
