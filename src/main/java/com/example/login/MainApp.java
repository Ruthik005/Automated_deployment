package com.example.login;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
//main function
public class MainApp {
    public static void main(String[] args) throws IOException {
        // Start HTTP server on port 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/validate", new ValidateHandler());
        server.setExecutor(null); // creates a default executor
        System.out.println("Server started at http://localhost:8080");
        server.start();
    }

    // Handles HTTP requests
    static class ValidateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            LoginService service = new LoginService();
            boolean result = service.validateLogin(
                "john",
                "john@example.com",
                "9876543210",
                "Password@123",
                "1995-08-15"
            );

            String response = "Login validation result: " + result;
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
}
