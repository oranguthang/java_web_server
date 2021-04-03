package com.javawebserver;

public class Main {

    public static void main(String[] args) {
        HTTPServer server = new HTTPServer();
        try {
            server.run(8080);
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
