package com.meyermt.order;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handler for the "/orders" context. Uses simple text file to maintain order IDs.
 * Created by michaelmeyer on 4/14/17.
 */
public class OrderHandler implements HttpHandler {

    public static final String UUIDS_FILE = "uuids.txt";
    private Path uuidPath;
    private List<String> uuids;

    /**
     * Constructs an Order handler. Looks for a file of uuids to manage, and if it doesn't exist, creates one. Expects
     * file to be in root dir for project.
     */
    public OrderHandler() {
        uuidPath = Paths.get(UUIDS_FILE);
        // create new list if first time
        if (!Files.isRegularFile(uuidPath)) {
            uuids = new ArrayList<>();
        } else {
            try {
                uuids = Files.readAllLines(uuidPath);
            } catch (IOException e) {
                throw new RuntimeException("unable to read in uuids file.", e);
            }
        }
    }

    /**
     * Handles GET, where it returns all order ids, PUT, which creates a new order id, and DELETE, which checks for a
     * order id and removes it if it exists.
     * @param httpExchange
     * @throws IOException
     */
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        if (httpExchange.getRequestMethod().equals("GET")) {
            int bodyLength = uuids.stream().map(uuid -> uuid + "\n").mapToInt(String::length).sum();
            httpExchange.sendResponseHeaders(200, bodyLength);
            OutputStream output = httpExchange.getResponseBody();
            for (String uuid : uuids) {
                String uuidLineBr = uuid + "\n";
                output.write(uuidLineBr.getBytes());
            }
            output.close();
        } else if (httpExchange.getRequestMethod().equals("DELETE")) {
            String uuid = httpExchange.getRequestURI().getPath().replaceAll(".*/orders/", "");
            System.out.println("uuid is " + uuid);
            OutputStream output = httpExchange.getResponseBody();
            if (uuids.contains(uuid)) {
                System.out.println("going to send 200");
                httpExchange.sendResponseHeaders(200, 1);
                uuids.remove(uuid);
                output.write("0".getBytes());
                persist();
            } else {
                httpExchange.sendResponseHeaders(404, 1);
                output.write("1".getBytes());
            }
            output.close();
        } else if (httpExchange.getRequestMethod().equals(("PUT"))) {
            String uuidStr = UUID.randomUUID().toString();
            uuids.add(uuidStr);
            persist();
            httpExchange.sendResponseHeaders(200, uuidStr.length());
            OutputStream output = httpExchange.getResponseBody();
            output.write(uuidStr.getBytes());
            output.close();
        } else {
            // have not received requirements for this case
            httpExchange.sendResponseHeaders(400, 0);
        }
    }

    /*
        Closest thing to database. Every time the in memory list of uuids is updated, the uuids text file is written to.
     */
    private void persist() {
        try {
            Files.write(uuidPath, uuids, Charset.forName("UTF-8"));
        } catch (IOException e) {
            throw new RuntimeException("Unable to persist uuids to file database.", e);
        }
    }
}
