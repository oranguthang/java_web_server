package com.javawebserver;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class HTTPServer {

    private static class SocketProcessor implements Runnable {

        private Socket s;
        private InputStream is;
        private OutputStream os;
        private String prefoxFolder = "web";

        private SocketProcessor(Socket s) throws Throwable {
            this.s = s;
            this.is = s.getInputStream();
            this.os = s.getOutputStream();
        }

        public void run() {
            try {
                String[] request = readInputHeaders();
                String filename = "";
                String responseType = "html";
                if (request[1].equals("/")) {
                    filename = "index.html";
                }
                else {
                    filename = request[1].substring(1, request[1].length());
                    if (filename.contains(".css")) {
                        responseType = "css";
                    }
                    else if (filename.contains(".js")) {
                        responseType = "javascript";
                    }
                }

                String response = "";
                if (request[0].equals("GET")) {
                    try {
                        byte[] encoded = Files.readAllBytes(Paths.get(prefoxFolder, filename));
                        response = new String(encoded, Charset.defaultCharset());
                    }
                    catch (Throwable t) {
                        t.printStackTrace();
                        response = "404 Not Found";
                    }
                }
                else {
                    if (request[2].startsWith("query")) {
                        String geoString = request[2].substring(6, request[2].length());
                        NLParser parser = new NLParser(geoString);
                        response = parser.getResult();
                        responseType = "json";
                    }
                }
                writeResponse(response, responseType);
            }
            catch (Throwable t) {
                t.printStackTrace();
            }
            finally {
                try {
                    s.close();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }

        private void writeResponse(String response, String responseType) throws Throwable {
            String HTTPResponse = "HTTP/1.1 200 OK\r\n" +
                "Server: " + System.getProperty("os.name") + " Java " + System.getProperty("java.version") + "\r\n" +
                "Content-Type: text/" + responseType + "; charset=UTF-8\r\n" +
                "Content-Length: " + response.length() + "\r\n" +
                "Content-Language: en\r\n\r\n";
            HTTPResponse = HTTPResponse + response;
            os.write(HTTPResponse.getBytes());
            os.flush();
        }

        private String[] readInputHeaders() throws Throwable {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String[] request = {"", "", ""};
            final String contentHeader = "Content-Length: ";
            int contentLength = 0;
            boolean isPost = false;
            while (true) {
                String s = br.readLine();
                if (s != null) {
                    if (s.contains("GET") || s.contains("POST")) {
                        int getPostSubstringPos = (s.indexOf("GET") == 0) ? 4 : 5;
                        isPost = s.contains("POST");
                        int httpSubstringPos = s.lastIndexOf("HTTP");
                        request[0] = s.substring(0, getPostSubstringPos - 1);
                        request[1] = s.substring(getPostSubstringPos, httpSubstringPos - 1);
                    }
                    if (isPost && s.startsWith(contentHeader)) {
                        contentLength = Integer.parseInt(s.substring(contentHeader.length()));
                    }

                    System.out.println(s);
                    if (s.trim().length() == 0) {
                        break;
                    }
                }
            }

            if (isPost) {
                StringBuilder queryBody = new StringBuilder();
                int c;
                for (int i = 0; i < contentLength; i++) {
                    c = br.read();
                    queryBody.append((char) c);
                }
                request[2] = queryBody.toString();
                System.out.println("Query: " + request[2]);
            }
            return request;
        }
    }

    public void run(int port) throws Throwable {
        ServerSocket ss = new ServerSocket(port);
        while (true) {
            Socket s = ss.accept();
            new Thread(new SocketProcessor(s)).start();
        }
    }
}
