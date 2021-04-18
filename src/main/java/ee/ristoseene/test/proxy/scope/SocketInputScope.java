package ee.ristoseene.test.proxy.scope;

import java.io.IOException;
import java.net.Socket;

public class SocketInputScope extends SocketScope {

    public SocketInputScope(final Socket socket) {
        super(socket);
    }

    @Override
    public void close() throws IOException {
        getSocket().shutdownInput();
    }

}
