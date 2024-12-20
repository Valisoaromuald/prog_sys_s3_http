import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class HttpHandler extends Thread {
    private Socket socket;
    private String[] informations;

    public HttpHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            OutputStream out = socket.getOutputStream()) {

            // Lire et Traiter la requête HTTP
            String line;
            int contentLength = 0;

            while (!(line = in.readLine()).isBlank()) {
                System.out.println(line);

                if (line.contains("HTTP")) {
                    this.informations = getEntete(line);
                }
                else if (line.startsWith("Content-Length:")) {
                    contentLength = getBodyPost(line);
                }
            }

            // Lire le corps de la requête (données POST)
            String requestBody = null;
            char[] body = new char[contentLength];
            in.read(body, 0, contentLength);
            if(informations[1].contains("?") && informations[1].contains("&")){
                requestBody = informations[1].trim().split("\\?")[1];
                informations[1] = informations[1].split("\\?")[0];
            }
            else requestBody = new String(body);

            // Déterminer le chemin du fichier ou dossier demandé
            String filePath = "options" + this.informations[1];

            byte[] httpResponse;
            // Construire la réponse HTTP
            if(filePath.endsWith(".php"))
            {
                httpResponse = processPHP(this.informations[0], requestBody, this.informations[2], filePath);
            }
            else httpResponse = reponseHTTP(this.informations[2], filePath, this.informations[1]);

            out.write(httpResponse);
            out.flush();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private byte[] processPHP(String Methode, String donnee, String versionHTTP, String filePath) {
        try {
            ProcessBuilder pb = null;
            // Créer un ProcessBuilder pour exécuter le script PHP
            if (Methode.equalsIgnoreCase("POST")) {
                List<String> commande = new ArrayList<>();
                commande.add("php");
                commande.add("-r");
                commande.add("parse_str('" + donnee + "', $_POST); include('" + filePath.replace("'", "\\'") + "');");
                pb = new ProcessBuilder(commande);
            } else if (Methode.equalsIgnoreCase("GET")) {
                List<String> commande = new ArrayList<>();
                commande.add("php");
                commande.add("-r");
                commande.add("parse_str('" + donnee + "', $_GET); include('" + filePath.replace("'", "\\'") + "');");
                pb = new ProcessBuilder(commande);
            } else {
                return generateErrorResponse(versionHTTP, "405 Method Not Allowed", "Method Not Allowed");
            }
            // Démarrer le processus
            Process process = pb.start();
    
            // Lire la sortie du processus (sortie standard du script PHP)
            InputStream inputStream = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
    
            // Construire la réponse HTTP avec la sortie PHP
            String httpResponse = versionHTTP + " 200 OK\r\n";
            httpResponse += "Content-Type: text/html; charset=UTF-8\r\n";
            httpResponse += "\r\n"; // Séparation des en-têtes et du corps
            httpResponse += response.toString();
    
            return httpResponse.getBytes(StandardCharsets.UTF_8);
    
        } catch (IOException e) {
            e.printStackTrace();
            return generateErrorResponse(versionHTTP, "500 Internal Server Error", "Internal Server Error");
        }
    }
    
    // Information Entête
    public String[] getEntete(String reponse) {
        StringTokenizer st = new StringTokenizer(reponse);
        String[] informations = new String[3];
        informations[0] = st.nextToken(); // Méthode (ex: GET)
        informations[1] = st.nextToken(); // URL (ex: /index.html)
        informations[2] = st.hasMoreTokens() ? st.nextToken() : "HTTP/1.1"; // Protocole (par défaut)
        return informations;
    }

    // Information Post ou Get
    public int getBodyPost(String reponse)
    {
        return Integer.parseInt(reponse.split(":")[1].trim());
    }

    // Réponse HTTP
    private byte[] reponseHTTP(String versionHTTP, String chemin, String requestPath) {
        try {
            Path path = Paths.get(chemin);

            if (Files.exists(path) && Files.isDirectory(path)) {
                StringBuilder response = new StringBuilder();
                response.append(versionHTTP).append(" 200 OK\r\n");
                response.append("Content-Type: text/html; charset=UTF-8\r\n");
                response.append("\r\n"); // Séparation des en-têtes et du corps
                response.append("<html>");
                response.append("<body>");
                response.append("<h1>").append("Liste des fichiers").append("</h1>");
                response.append("<table>");
                response.append("<tr>");
                response.append("<th valign=\"top\">").append("</th>");
                response.append("<th>").append("Nom").append("</th>");
                response.append("<th>").append("Derniere Modification").append("</th>");
                response.append("<th>").append("Taille").append("</th>");
                response.append("<th>").append("Description").append("</th>");
                response.append("</tr>");
                
                if(requestPath.equals("/"))
                {
                    Files.list(path).forEach(file -> 
                    {
                        response.append("<tr>");
                        response.append("<th>").append("<img src=\"").append(imageAdequat(file.getFileName().toString())).append("\">").append("</th>");
                        response.append("<th>").append("<a href=\"").append(file.getFileName()).append("\">").append(file.getFileName()).append("</a>").append("</th>");
                        response.append("<th>").append("2024").append("</th>");
                        response.append("<th>").append("-").append("</th>");
                        response.append("<th>").append(" ").append("</th>");
                        response.append("</tr>");
                    } 
                    );   
                }
                else
                {
                    List<Path> fileList = Files.list(path).toList();

                    for(Path file : fileList)
                    {
                        String[] nomFichier = file.getFileName().toString().split("\\.");
    
                        if(nomFichier[0].equalsIgnoreCase("index"))
                        {
                            return renvoyeReponseOK(versionHTTP, chemin + "/" + file.getFileName().toString(), file.getFileName().toString());  
                        }
                        else
                        {
                            String fileName = file.getFileName().toString();
                            response.append("<tr>");
                            response.append("<th>").append("<img src=\"").append(imageAdequat(file.getFileName().toString())).append("\">").append("</th>");
                            response.append("<th>").append("<a href=\"").append(requestPath).append("/").append(fileName).append("\">").append(file.getFileName()).append("</a>").append("</th>");
                            response.append("<th>").append("2024").append("</th>");
                            response.append("<th>").append("-").append("</th>");
                            response.append("<th>").append(" ").append("</th>");
                            response.append("</tr>");
                        }
                    }
                }
                

                response.append("<tr>").append("<th colspan=\"5\">").append("<hr> </th>").append("</tr>");
                response.append("</table>");
                response.append("</body>");
                response.append("</html>");
                return response.toString().getBytes(StandardCharsets.UTF_8);

            } else if (Files.exists(path) && Files.isReadable(path)) {
                return renvoyeReponseOK(versionHTTP, chemin, requestPath);

            } else {
                return generateErrorResponse(versionHTTP, "404 Not Found", "404 Not Found");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return generateErrorResponse(versionHTTP, "500 Internal Server Error", "Internal Server Error");
        }
    }

    private String imageAdequat(String filename)
    {
        String[] nomFichier = filename.split("\\.");

        if(filename.contains("."))
        {
            if(nomFichier[1].equals("png"))
            {
                return "/icons/image2.png";
            }
            else if(nomFichier[1].equals("zip"))
            {
                return "/icons/compressed.png";
            }
            else return "/icons/xml.png";
        }
        else
        {
            return "/icons/folder.png";
        }
        
    }

    // Rêquete pour le Client
    private byte[] renvoyeReponseOK(String versionHTTP, String chemin, String requestPath) {
        try {
            Path path = Paths.get(chemin);
            String mimeType = Files.probeContentType(path);

            byte[] fileBytes = Files.readAllBytes(path);

            StringBuilder response = new StringBuilder();
            response.append(versionHTTP).append(" 200 OK\r\n");
            response.append("Content-Type: ").append(mimeType).append("\r\n");
            response.append("Content-Length: ").append(fileBytes.length).append("\r\n");
            response.append("\r\n"); // Séparation des en-têtes et du corps

            byte[] headerBytes = response.toString().getBytes(StandardCharsets.UTF_8);
            byte[] httpResponse = new byte[headerBytes.length + fileBytes.length];

            System.arraycopy(headerBytes, 0, httpResponse, 0, headerBytes.length);
            System.arraycopy(fileBytes, 0, httpResponse, headerBytes.length, fileBytes.length);

            return httpResponse;

        } catch (IOException e) {
            e.printStackTrace();
            return generateErrorResponse(versionHTTP, "500 Internal Server Error", "Internal Server Error");
        }
    }

    // Génération (Généralisation) de Message d'Erreur
    private byte[] generateErrorResponse(String versionHTTP, String status, String message) {
        StringBuilder response = new StringBuilder();
        response.append(versionHTTP).append(" ").append(status).append("\r\n");
        response.append("Content-Type: text/html\r\n");
        response.append("\r\n"); // Séparation des en-têtes et du corps
        response.append("<html><body><h1>").append(message).append("</h1></body></html>");

        return response.toString().getBytes(StandardCharsets.UTF_8);
    }
}
