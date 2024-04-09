import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class SimpleBackendServer {
    public static void main(String[] args) {
        int portNumber = 8080;

        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            System.out.println("Server is running on port " + portNumber);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                handleClient(clientSocket);
            }
        } catch (IOException e) {
            System.err.println("Error occurred while running the server: " + e.getMessage());
        }
    }

    private static void handleW2TaxForm(Map<String, String> queryParams) {
        // Construct a string from query parameters
        StringBuilder dataBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            dataBuilder.append(entry.getKey()).append(": ").append(entry.getValue()).append(", ");
        }

        // Remove the last comma and space
        if (dataBuilder.length() > 0) {
            dataBuilder.setLength(dataBuilder.length() - 2);
        }

        // Log the incoming data to the console
        System.out.println("Incoming data: " + dataBuilder.toString());

        // Write data to a file
        try (FileWriter fw = new FileWriter("w2taxformData.txt", true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw)) {
            out.println(dataBuilder.toString());
        } catch (IOException e) {
            System.err.println("Error writing to the file: " + e.getMessage());
        }
    }


    private static void handleClient(Socket clientSocket) {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
        ) {
            String inputLine;

            if ((inputLine = in.readLine()) != null) {
                String[] requestParts = inputLine.split(" ");
                if (requestParts.length >= 2) {
                    String pathWithParams = requestParts[1];
                    if (pathWithParams.startsWith("/calculateTax")) {
                        double tax = calculateTax(pathWithParams);
                        // Set CORS headers and output the tax
                        out.println("HTTP/1.1 200 OK");
                        out.println("Content-Type: text/plain");
                        out.println("Access-Control-Allow-Origin: *"); // Allow requests from any origin
                        out.println(); // Empty line to indicate end of headers
                        out.println(tax);
                    }

                    if (pathWithParams.startsWith("/w2taxform")) {
                        Map<String, String> queryParams = extractQueryParams(pathWithParams);
                        handleW2TaxForm(queryParams); // Handle the w2taxform request
                        
                        // Respond to the client
                        out.println("HTTP/1.1 200 OK");
                        out.println("Content-Type: text/plain");
                        out.println("Access-Control-Allow-Origin: *");
                        out.println(); // End of headers
                        out.println("Data received and stored successfully.");
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error handling client request: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    private static double calculateTax(String pathWithParams) {
        Map<String, String> queryParams = extractQueryParams(pathWithParams);
        double income = Double.parseDouble(queryParams.getOrDefault("income", "0"));
        int status = Integer.parseInt(queryParams.getOrDefault("status", "0"));
        return getTax(income, status);
    }

    private static Map<String, String> extractQueryParams(String path) {
        Map<String, String> queryParams = new HashMap<>();
        String[] pathParts = path.split("\\?");
        if (pathParts.length > 1) {
            String[] pairs = pathParts[1].split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    queryParams.put(keyValue[0], keyValue[1]);
                }
            }
        }
        return queryParams;
    }

    public static double getTax(double income, int status) {
        double taxRate;
        switch (status) {
            case 1: taxRate = 0.3; break;
            case 2: taxRate = 0.2; break;
            case 3: taxRate = 0.15; break;
            case 4: taxRate = 0.1; break;
            default: taxRate = 0.1;
        }
        return income * taxRate;
    }
}
