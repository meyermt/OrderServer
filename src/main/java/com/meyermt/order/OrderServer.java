package com.meyermt.order;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Basic HttpServer acting as a pseudo-REST api server. Takes requests to one context, "/orders"
 * Created by michaelmeyer on 4/14/17.
 */
public class OrderServer {

    private static final String PORT_ARG = "--port";

    /**
     * Main driver for the server. Must include port on running.
     * @param args
     */
    public static void main(String[] args) {
        if (args.length == 2 && args[0].startsWith(PORT_ARG)) {
            try {
                int port = Integer.parseInt(args[1]);
                HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
                OrderHandler handler = new OrderHandler();
                server.createContext("/orders", handler);
                server.start();
            } catch (IOException e) {
                throw new RuntimeException("Unable to create order server", e);
            }
        } else {
            System.out.println("Illegal arguments. Should be run with arguments: --port <port>");
            System.exit(1);
        }
    }
}
