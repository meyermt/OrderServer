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
     * Constructs an Order handler. Looks for a file of uuids to manage, and if it doesn't exist, will write its own. Expects
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
     * Handles GET with param "method=getCount", where it returns a count of orders
     * POST with param "method=createOrder", which creates a new order id
     * and DELETE with params "method=cancelOrder&uuid=[uuid]", which checks for an order id and removes it if it exists.
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
        } else if (httpExchange.getRequestMethod().equals(("POST"))) {
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

    /*
        streams over String params, parses them into a map
     */
    private Map<String, String> parseParams(String query) {
        return Arrays.asList(query.split("&")).stream()
                .collect(Collectors.toMap(
                        param -> param.split("=")[0],
                        param -> param.split("=")[1]));
    }

    /*
        Response for 404 Not found
     */
    private void sendNotFound(HttpExchange httpExchange, String message) {
        JsonObject returnObject = new JsonObject();
        returnObject.put("status", "404 Not Found");
        returnObject.put("message", message);
        packageAndSendJson(httpExchange, returnObject);
    }

    /*
        get count call and response
     */
    private void getCount(HttpExchange httpExchange) {
        long totalCount = uuidToState.entrySet().stream().count();
        JsonObject returnObject = new JsonObject();
        returnObject.put("status", "200 OK");
        returnObject.put("totalCount", totalCount);
        packageAndSendJson(httpExchange, returnObject);
    }

    /*
        order cancellation call and response
     */
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

    /*
        order creation call and response
     */
    private void createOrder(HttpExchange httpExchange) {
        JsonObject returnObject = new JsonObject();
        returnObject.put("status", "200 OK");
        String uuidStr = UUID.randomUUID().toString();
        uuidToState.put(uuidStr, "created");
        returnObject.put("uuid", uuidStr);
        persist();
        packageAndSendJson(httpExchange, returnObject);
    }

    /*
        helper method to package response and send back to client
     */
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
