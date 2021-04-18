package ee.ristoseene.test.proxy;

import ee.ristoseene.test.proxy.http.HttpRequestHeader;
import ee.ristoseene.test.proxy.http.HttpResponseHeader;
import ee.ristoseene.test.proxy.scope.SocketInputScope;
import ee.ristoseene.test.proxy.scope.SocketOutputScope;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ConnectionHandler {

    private final ExecutorService executorService;

    public ConnectionHandler(final ExecutorService executorService) {
        this.executorService = Objects.requireNonNull(executorService, "No executor service provided!");
    }

    public void initiateConnection(final Socket socket) {
        Objects.requireNonNull(socket, "No socket to connect to!");
        executorService.submit(() -> handleClientToProxy(socket));
    }

    private void handleClientToProxy(final Socket socket) {
        try (final Socket clientSocket = socket) {
            InputStream inputFromClient = clientSocket.getInputStream();
            HttpRequestHeader requestHeader = new HttpRequestHeader(inputFromClient);
            System.out.println(requestHeader);

            switch (requestHeader.getRequestMethod()) {
                case "CONNECT":
                    handleTunnelingToServer(clientSocket, requestHeader, inputFromClient);
                    break;
                default:
                    handleProxyToServer(clientSocket, requestHeader, inputFromClient);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleTunnelingToServer(Socket clientSocket, HttpRequestHeader requestHeader, InputStream inputFromClient) throws Exception {
        String[] hostPort = requestHeader.getRequestUri().split(":");
        if (hostPort.length != 2) {
            throw new IOException("Invalid host for tunneling: " + requestHeader.getRequestUri());
        }

        try (Socket serverSocket = new Socket(hostPort[0], Integer.parseInt(hostPort[1]))) {
            OutputStream outputToServer = serverSocket.getOutputStream();

            Future<?> serverToClientFuture = executorService.submit(() -> handleServerToClientRaw(clientSocket, serverSocket));

            try (
                    SocketInputScope inputFromClientScope = new SocketInputScope(clientSocket);
                    SocketOutputScope outputToServerScope = new SocketOutputScope(serverSocket)
            ) {
                transferAll(inputFromClient, outputToServer);
                // TODO: figure out what to do with reads timing out
            } finally {
                serverToClientFuture.get(10L, TimeUnit.SECONDS);
            }
        }
    }

    private void handleServerToClientRaw(Socket clientSocket, Socket serverSocket) {
        try (SocketOutputScope outputToClientScope = new SocketOutputScope(clientSocket)) {
            OutputStream outputToClient = clientSocket.getOutputStream();

            try (SocketInputScope inputFromServerScope = new SocketInputScope(serverSocket)) {
                InputStream inputFromServer = serverSocket.getInputStream();

                PrintWriter writer = new PrintWriter(outputToClient, false, StandardCharsets.US_ASCII);
                writer.append("HTTP/1.0").append(' ').append("200").append(' ').append("Connection established").append('\r').append('\n');
                writer.append("Proxy-Agent: SuperAwesomeProxyServer").append('\r').append('\n');
                writer.append('\r').append('\n');
                writer.flush();

                transferAll(inputFromServer, outputToClient);
            } // TODO: error response to client on failure
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleProxyToServer(Socket clientSocket, HttpRequestHeader requestHeader, InputStream inputFromClient) throws Exception {
        URL targetURL = new URL(requestHeader.getRequestUri());

        try (Socket serverSocket = new Socket(targetURL.getHost(), getPort(targetURL))) {
            OutputStream outputToServer = serverSocket.getOutputStream();

            Future<?> serverToClientFuture = executorService.submit(() -> handleServerToClient(clientSocket, serverSocket));

            try (
                    SocketInputScope inputFromClientScope = new SocketInputScope(clientSocket);
                    SocketOutputScope outputToServerScope = new SocketOutputScope(serverSocket)
            ) {
                PrintWriter writer = new PrintWriter(outputToServer, false, StandardCharsets.US_ASCII);
                writer.append(requestHeader.getRequestMethod()).append(' ').append(targetURL.getPath()).append(' ').append(requestHeader.getRequestVersion()).append('\r').append('\n');
                requestHeader.getHttpHeaders().forEach((key, value) -> writer.append(key).append(':').append(' ').append(value).append('\r').append('\n'));
                writer.append('\r').append('\n');
                writer.flush();

                OptionalInt contentLength = getContentLength(requestHeader.getHttpHeaders());
                if (contentLength.isPresent()) {
                    transfer(inputFromClient, outputToServer, contentLength.getAsInt());
                }
            } finally {
                serverToClientFuture.get(10L, TimeUnit.SECONDS);
            }
        }
    }

    private void handleServerToClient(Socket clientSocket, Socket serverSocket) {
        try (SocketOutputScope outputToClientScope = new SocketOutputScope(clientSocket)) {
            OutputStream outputToClient = clientSocket.getOutputStream();

            try (SocketInputScope inputFromServerScope = new SocketInputScope(serverSocket)) {
                InputStream inputFromServer = serverSocket.getInputStream();
                HttpResponseHeader responseHeader = new HttpResponseHeader(inputFromServer);
                System.out.println(responseHeader);

                PrintWriter writer = new PrintWriter(outputToClient, false, StandardCharsets.US_ASCII);
                writer.append(responseHeader.getResponseVersion()).append(' ').append(responseHeader.getResponseStatusCode()).append(' ').append(responseHeader.getResponseStatusText()).append('\r').append('\n');
                responseHeader.getHttpHeaders().forEach((key, value) -> writer.append(key).append(':').append(' ').append(value).append('\r').append('\n'));
                writer.append('\r').append('\n');
                writer.flush();

                OptionalInt contentLength = getContentLength(responseHeader.getHttpHeaders());
                if (contentLength.isEmpty()) return;

                transfer(inputFromServer, outputToClient, contentLength.getAsInt());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int getPort(URL url) {
        return (url.getPort() < 0) ? url.getDefaultPort() : url.getPort();
    }

    private static OptionalInt getContentLength(Map<String, String> httpHeaders) {
        return httpHeaders.entrySet().stream()
                .filter(e -> "Content-Length".equalsIgnoreCase(e.getKey()))
                .mapToInt(e -> Integer.parseInt(e.getValue()))
                .findFirst();
    }

    private static void transfer(InputStream in, OutputStream out, int length) throws IOException {
        for (int i = 0; i < length; ++i) {
            int value = in.read();

            if (value < 0) {
                throw new EOFException();
            }

            out.write(value);
        }

        out.flush();
    }

    private static void transferAll(InputStream in, OutputStream out) throws IOException {
        int value;

        while ((value = in.read()) >= 0) {
            out.write(value);
        }

        out.flush();
    }

}
