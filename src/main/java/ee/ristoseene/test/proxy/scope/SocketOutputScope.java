package ee.ristoseene.test.proxy.scope;

import java.io.IOException;
import java.net.Socket;

public class SocketOutputScope extends SocketScope {

    public SocketOutputScope(final Socket socket) {
        super(socket);
    }

    @Override
    public void close() throws IOException {
        getSocket().shutdownOutput();
    }

}
