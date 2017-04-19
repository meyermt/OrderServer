package com.meyermt.order;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import org.json.simple.JsonObject;

/**
 * Handler for the "/orders" context. Uses simple text file to maintain order IDs.
 * Created by michaelmeyer on 4/14/17.
 */
public class OrderHandler implements HttpHandler {

    public static final String UUIDS_FILE = "uuids.txt";
    private Path uuidPath;
    private Map<String, String> uuidToState = new HashMap<>();

    /**
     * Constructs an Order handler. Looks for a file of uuids to manage, and if it doesn't exist, creates one. Expects
     * file to be in root dir for project.
     */
    public OrderHandler() {
        uuidPath = Paths.get(UUIDS_FILE);
        // create new list if first time
        try {
            if (Files.isRegularFile(uuidPath)) {
                String uuidRegEx = "(?<uuid>.+):.+";
                String stateRegEx = ".+:(?<state>)";
                Files.readAllLines(uuidPath).stream().forEach(line -> {
                    String uuid = line.replaceAll(uuidRegEx, "${uuid}");
                    String state = line.replaceAll(stateRegEx, "${state}");
                    uuidToState.put(uuid, state);
                });
            }
        } catch (IOException e) {
            throw new RuntimeException("unable to read in uuids file.", e);
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
        String query = httpExchange.getRequestURI().getQuery();
        Map<String, String> params = parseParams(query);
        if (httpExchange.getRequestMethod().equals("GET")) {
            if (params.get("method").equals("getCount")) {
                getCount(httpExchange);
            } else {
                sendNotFound(httpExchange, "Unable to find method for query: " + query);
            }
        } else if (httpExchange.getRequestMethod().equals("DELETE")) {
            if (params.get("method").equals("cancelOrder")) {
                cancelOrder(httpExchange, params);
            } else {
                sendNotFound(httpExchange, "Unable to find method for query: " + query);
            }
        } else if (httpExchange.getRequestMethod().equals(("PUT"))) {
            if (params.get("method").equals("createOrder")) {
                createOrder(httpExchange);
            } else {
                sendNotFound(httpExchange, "Unable to find method for query: " + query);
            }
        } else {
            // have not received requirements for this case
            httpExchange.sendResponseHeaders(400, 0);
        }
    }

    private Map<String, String> parseParams(String query) {
        return Arrays.asList(query.split("&")).stream()
                .collect(Collectors.toMap(
                        param -> param.split("=")[0],
                        param -> param.split("=")[1]));
    }

    private void sendNotFound(HttpExchange httpExchange, String message) {
        JsonObject returnObject = new JsonObject();
        returnObject.put("status", "404 Not Found");
        returnObject.put("message", message);
        packageAndSendJson(httpExchange, returnObject);
    }

    private void getCount(HttpExchange httpExchange) {
        long totalCount = uuidToState.entrySet().stream().count();
        JsonObject returnObject = new JsonObject();
        returnObject.put("status", "200 OK");
        returnObject.put("totalCount", totalCount);
        packageAndSendJson(httpExchange, returnObject);
    }

    private void cancelOrder(HttpExchange httpExchange, Map<String, String> params) {
        JsonObject returnObject = new JsonObject();
        returnObject.put("status", "200 OK");
        String uuid = params.get("uuid");
        if (uuidToState.containsKey(uuid) && !uuidToState.get(uuid).equals("cancelled")) {
            uuidToState.put(params.get("uuid"), "cancelled");
            persist();
            returnObject.put("cancelStatus", "0");
            packageAndSendJson(httpExchange, returnObject);
        } else {
            returnObject.put("cancelStatus", "1");
            packageAndSendJson(httpExchange, returnObject);
        }
    }

    private void createOrder(HttpExchange httpExchange) {
        JsonObject returnObject = new JsonObject();
        returnObject.put("status", "200 OK");
        String uuidStr = UUID.randomUUID().toString();
        uuidToState.put(uuidStr, "created");
        returnObject.put("uuid", uuidStr);
        System.out.println("created order with uuid " + uuidStr);
        persist();
        packageAndSendJson(httpExchange, returnObject);
    }

    private void packageAndSendJson(HttpExchange httpExchange, JsonObject returnObject) {
        String returnString = returnObject.toJson();
        try {
            httpExchange.sendResponseHeaders(200, returnString.length());
            OutputStream output = httpExchange.getResponseBody();
            output.write(returnString.getBytes());
            output.close();
        } catch (IOException e) {
            throw new RuntimeException("Unable to create json response for " + returnString, e);
        }
    }

    /*
        Closest thing to database. Every time the in memory list of uuids is updated, the uuids text file is written to.
     */
    private void persist() {
        List<String> lines = uuidToState.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.toList());
        try {
            Files.write(uuidPath, lines, Charset.forName("UTF-8"));
        } catch (IOException e) {
            throw new RuntimeException("Unable to persist uuids to file database.", e);
        }
    }
}
