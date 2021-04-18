package ee.ristoseene.test.proxy.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

abstract class HttpHeaderBase {

    protected final StringBuilder headerBuffer;
    protected final Map<String, String> httpHeaders;

    protected HttpHeaderBase(Map<String, String> httpHeaders) {
        this.httpHeaders = Objects.requireNonNull(httpHeaders);
        this.headerBuffer = new StringBuilder();
    }

    protected final void readAndParseHttpHeaders(InputStream inputStream) throws IOException {
        int lineLength;

        while ((lineLength = readLine(inputStream, headerBuffer)) > 2) {
            String[] httpHeader = headerBuffer.substring(headerBuffer.length() - lineLength).split(": ");
            httpHeaders.put(httpHeader[0].trim(), httpHeader[1].trim());
        }
    }

    protected static int readLine(final InputStream input, StringBuilder accumulator) throws IOException {
        int value, length = 0;

        while ((value = input.read()) >= 0) {
            accumulator.append((char) value);
            length += 1;

            if (value == '\n') {
                break;
            }
        }

        return length;
    }

    public Map<String, String> getHttpHeaders() {
        return Collections.unmodifiableMap(httpHeaders);
    }

    @Override
    public String toString() {
        return headerBuffer.toString();
    }

}
