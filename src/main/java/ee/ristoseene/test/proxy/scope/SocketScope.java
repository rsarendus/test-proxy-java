package ee.ristoseene.test.proxy.scope;

import java.net.Socket;
import java.util.Objects;

public abstract class SocketScope implements AutoCloseable {

    private final Socket socket;

    protected SocketScope(final Socket socket) {
        this.socket = Objects.requireNonNull(socket, "Socket not provided");
    }

    public Socket getSocket() {
        return socket;
    }

}
