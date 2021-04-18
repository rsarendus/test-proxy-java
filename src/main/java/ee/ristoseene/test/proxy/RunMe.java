package ee.ristoseene.test.proxy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class RunMe {

    public static void main(final String[] args) {
        ExecutorService executorService = Executors.newCachedThreadPool();
        ProxyServer proxyServer = new ProxyServer(executorService, 9999);
        executorService.submit(proxyServer);
    }

}
