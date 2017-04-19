package com.meyermt.order;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.util.Scanner;

/**
 * Created by michaelmeyer on 4/18/17.
 */
public class OrderClient {

    private static String SERVER_PORT_ARG = "--serverPort";
    private static String IP_ARG = "--ip";
    private static String serverUrl;

    public static void main(String[] args) {
        if (args.length == 4 && args[0].startsWith(IP_ARG) && args[2].startsWith(SERVER_PORT_ARG)) {
            String ip = args[1];
            int serverPort = Integer.parseInt(args[3]);
            serverUrl = "http://" + ip + ":" + serverPort + "/orders";
            runClient();
        } else {
            System.out.println("Illegal arguments. Should be run with arguments: --ip <server's ip> --leftPort <order server port>");
            System.exit(1);
        }
    }

    private static void runClient() {
        Scanner scanner = new Scanner(System.in);
        String message = "";
        System.out.println("Welcome to Order client. Directions on communications you can send to the Order server:");
        System.out.println("Type 'create' to create a new order.");
        System.out.println("Type 'cancel <uuid> to cancel an order.");
        System.out.println("Type 'count' to receive a count of all orders (will include cancelled orders in count).");
        System.out.println("When done, type 'exit' to exit out of the client.");
        while (!message.equals("exit")) {
            message = scanner.nextLine();
            if (message.equals("create")) {
                createOrder();
            } else if (message.matches("cancel.*")) {
                cancelOrder(message);
            } else if (message.equals("count")) {
                countOrders();
            }
        }
    }

    private static void countOrders() {
        try {
            HttpResponse<JsonNode> response = Unirest.get(serverUrl)
                    .queryString("method", "getCount")
                    .asJson();
            System.out.println("status is " + response.getBody().getObject().get("status"));
            System.out.println("Count of cancelled and existent orders is " + response.getBody().getObject().get("totalCount"));
        } catch (UnirestException e) {
            throw new RuntimeException("Unable to complete get count request", e);
        }
    }

    private static void cancelOrder(String message) {
        String uuid = message.substring(7);
        try {
            HttpResponse<JsonNode> response = Unirest.delete(serverUrl)
                    .queryString("method", "cancelOrder")
                    .queryString("uuid", uuid)
                    .asJson();
            System.out.println("status is " + response.getBody().getObject().get("status"));
            if (response.getBody().getObject().get("cancelStatus").equals("0")) {
                System.out.println("Order " + uuid + " was cancelled.");
            } else {
                System.out.println("Order " + uuid + " was not cancelled.");
            }
        } catch (UnirestException e) {
            throw new RuntimeException("Unable to complete cancel order request", e);
        }
    }

    private static void createOrder() {
        try {
            HttpResponse<JsonNode> response = Unirest.put(serverUrl)
                    .queryString("method", "createOrder")
                    .asJson();
            System.out.println("status is " + response.getBody().getObject().get("status"));
            System.out.println("Order with UUID " + response.getBody().getObject().get("uuid") + " was created.");
        } catch (UnirestException e) {
            throw new RuntimeException("Unable to complete create order request", e);
        }
    }

}
