package ee.ristoseene.test.proxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

public final class ProxyServer implements Runnable {

    private static final int DEFAULT_SO_TIMEOUT_MILLIS = 10_000;

    private final ConnectionHandler connectionHandler;
    private final int port;

    public ProxyServer(final ExecutorService executorService, final int port) {
        this.connectionHandler = new ConnectionHandler(executorService);
        this.port = port;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            do {
                Socket socket = serverSocket.accept();
                socket.setSoTimeout(DEFAULT_SO_TIMEOUT_MILLIS);
                connectionHandler.initiateConnection(socket);
            } while (!Thread.interrupted());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
