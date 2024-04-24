import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class SimpleBackendServer {
    private static final String BASE_PATH = "C:\\Users\\narac\\Desktop\\New folder (3)\\SimpleBackendServer\\frontend";

    public static void main(String[] args) {
        int portNumber = 8080;
        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            System.out.println("Server is running on port " + portNumber);
            // Main Server Loop for iterative pattern
            while (true) {
                try (Socket clientSocket = serverSocket.accept()) {
                    handleClient(clientSocket);
                } catch (IOException e) {
                    System.err.println("Error handling client: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Error occurred while running the server: " + e.getMessage());
        }
    }

    private static void handleClient(Socket clientSocket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

        String requestLine = in.readLine();
        if (requestLine == null) return;
        String[] tokens = requestLine.split(" ");
        String method = tokens[0];
        String path = tokens[1];

        Map<String, String> headers = parseHeaders(in);

        switch (method) {
            case "GET":
                handleGetRequest(path, out);
                break;
            case "POST":
                handlePostRequest(path, in, headers, out);
                break;
            default:
                sendResponse(out, "HTTP/1.1 405 Method Not Allowed", "text/plain", "");
        }

        clientSocket.close();
    }

    private static Map<String, String> parseHeaders(BufferedReader in) throws IOException {
        Map<String, String> headers = new HashMap<>();
        String line;
        while (!(line = in.readLine()).isEmpty()) {
            int separator = line.indexOf(":");
            if (separator != -1) {
                headers.put(line.substring(0, separator), line.substring(separator + 2));
            }
        }
        return headers;
    }

    private static void handleGetRequest(String path, PrintWriter out) throws IOException {
        if (path.startsWith("/calculateTax") || path.startsWith("/w2taxform")) {
            processRequest(path, out);
        } else {
            serveStaticFile(path, out);
        }
    }

    private static void handlePostRequest(String path, BufferedReader in, Map<String, String> headers, PrintWriter out) throws IOException {
        if (path.equals("/uploadDocument")) {
            handleUploadDocument(in, headers.get("Content-Length"), out);
        }
    }

    private static void processRequest(String path, PrintWriter out) {
        // Determine action based on the path
        if (path.startsWith("/calculateTax")) {
            processTaxCalculation(path, out);
        } else if (path.startsWith("/w2taxform")) {
            processW2FormSubmission(path, out);
        } else {
            // Default response if the path is not recognized for specific processing
            sendResponse(out, "HTTP/1.1 404 Not Found", "text/plain", "Resource not found");
        }
    }

    // Method to handle tax calculations based on query parameters in the path
    private static void processTaxCalculation(String path, PrintWriter out) {
        Map<String, String> queryParams = extractQueryParams(path);
        try {
            double wages = Double.parseDouble(queryParams.getOrDefault("wages", "0"));
            double federalTax = Double.parseDouble(queryParams.getOrDefault("federalTax", "0"));
            double stateTax = Double.parseDouble(queryParams.getOrDefault("stateTax", "0"));
            double taxReturn = wages * 0.1 - federalTax - stateTax; // Example simplified calculation

            sendResponse(out, "HTTP/1.1 200 OK", "text/plain", String.format("Estimated Tax Return: $%.2f", taxReturn));
        } catch (NumberFormatException e) {
            sendResponse(out, "HTTP/1.1 400 Bad Request", "text/plain", "Invalid input for tax calculation");
        }
    }

    // Method to process W-2 form submissions based on query parameters
    private static void processW2FormSubmission(String path, PrintWriter out) {
        Map<String, String> queryParams = extractQueryParams(path);
        // acknowledge the receipt of such data for now
        sendResponse(out, "HTTP/1.1 200 OK", "text/plain", "W-2 Form data received and processed.");
    }

    // Helper method to extract query parameters from a URL
    // Iterative pattern used to split the query strings into key-value pairs
    private static Map<String, String> extractQueryParams(String path) {
        Map<String, String> queryParams = new HashMap<>();
        int questionMarkIndex = path.indexOf('?');
        if (questionMarkIndex != -1 && path.length() > questionMarkIndex + 1) {
            String[] pairs = path.substring(questionMarkIndex + 1).split("&");
            for (String pair : pairs) {
                String[] kv = pair.split("=");
                if (kv.length == 2) {
                    queryParams.put(kv[0], kv[1]);
                }
            }
        }
        return queryParams;
    }


    private static void serveStaticFile(String filePath, PrintWriter out) throws IOException {
        if (filePath.equals("/")) filePath = "/index.html";
        File file = new File(BASE_PATH + filePath);
        if (file.exists() && !file.isDirectory()) {
            byte[] fileContent = Files.readAllBytes(Paths.get(BASE_PATH, filePath));
            sendResponse(out, "HTTP/1.1 200 OK", getContentType(filePath), new String(fileContent));
        } else {
            sendResponse(out, "HTTP/1.1 404 Not Found", "text/plain", "404 Not Found");
        }
    }

    private static void handleUploadDocument(BufferedReader in, String contentLength, PrintWriter out) throws IOException {
        if (contentLength == null) {
            sendResponse(out, "HTTP/1.1 400 Bad Request", "text/plain", "Invalid Content Length");
            return;
        }

        int length = Integer.parseInt(contentLength.trim());
        char[] buffer = new char[length];
        in.read(buffer, 0, length);
        String body = new String(buffer);

        // Log received data for debugging
        System.out.println("Received document: " + body);


        sendResponse(out, "HTTP/1.1 200 OK", "application/json", "{\"message\": \"Document processed successfully.\"}");
    }

    private static String getContentType(String path) {
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".js")) return "application/javascript";
        return "text/plain";
    }

    private static void sendResponse(PrintWriter out, String status, String contentType, String content) {
        out.println(status);
        out.println("Content-Type: " + contentType);
        out.println("Content-Length: " + content.length());
        out.println();
        out.println(content);
        out.flush();
    }
}

