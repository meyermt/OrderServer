package com.meyermt.order;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Basic HttpServer acting as a pseudo-REST api server. Takes requests to one context, "/orders"
 * Created by michaelmeyer on 4/14/17.
 */
public class OrderServer {

    /**
     * Main driver for the server. Defaults to run on port 8080.
     * @param args
     */
    public static void main(String[] args) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            OrderHandler handler = new OrderHandler();
            server.createContext("/orders", handler);
            server.start();
        } catch (IOException e) {
            throw new RuntimeException("Unable to create order server", e);
        }

    }
}
