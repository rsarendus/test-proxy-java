package ee.ristoseene.test.proxy.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HttpRequestHeader extends HttpHeaderBase {

    private static final String GROUP_METHOD = "method";
    private static final String GROUP_URI = "uri";
    private static final String GROUP_VERSION = "version";

    private static final Pattern REQUEST_PATTERN = Pattern.compile(String.format(
            "(?<%s>[A-Z]+) (?<%s>[-+_A-Za-z0-9:/.?=]+) (?<%s>HTTP/[0-9][.][0-9])[\r][\n]",
            GROUP_METHOD, GROUP_URI, GROUP_VERSION
    ));

    private final Matcher requestMatcher;

    public HttpRequestHeader(final InputStream requestInput) throws IOException {
        super(new LinkedHashMap<>());

        readLine(requestInput, headerBuffer);
        requestMatcher = REQUEST_PATTERN.matcher(headerBuffer.toString());
        if (!requestMatcher.matches()) {
            throw new IOException("Invalid request: " + headerBuffer.toString());
        }

        readAndParseHttpHeaders(requestInput);
    }

    public String getRequestMethod() {
        return requestMatcher.group(GROUP_METHOD);
    }

    public String getRequestUri() {
        return requestMatcher.group(GROUP_URI);
    }

    public String getRequestVersion() {
        return requestMatcher.group(GROUP_VERSION);
    }

}
