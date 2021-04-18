package ee.ristoseene.test.proxy.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HttpResponseHeader extends HttpHeaderBase {

    private static final String GROUP_VERSION = "version";
    private static final String GROUP_STATUS = "status";
    private static final String GROUP_DESCRIPTION = "description";

    private static final Pattern RESPONSE_PATTERN = Pattern.compile(String.format(
            "(?<%s>HTTP/[0-9][.][0-9]) (?<%s>[0-9]{3}) (?<%s>[A-Za-z ]+)[\r][\n]",
            GROUP_VERSION, GROUP_STATUS, GROUP_DESCRIPTION
    ));

    private final Matcher responseMatcher;

    public HttpResponseHeader(InputStream responseInput) throws IOException {
        super(new LinkedHashMap<>());

        readLine(responseInput, headerBuffer);
        responseMatcher = RESPONSE_PATTERN.matcher(headerBuffer.toString());
        if (!responseMatcher.matches()) {
            throw new IOException("Invalid response: " + headerBuffer.toString());
        }

        readAndParseHttpHeaders(responseInput);
    }

    public String getResponseVersion() {
        return responseMatcher.group(GROUP_VERSION);
    }

    public String getResponseStatusCode() {
        return responseMatcher.group(GROUP_STATUS);
    }

    public String getResponseStatusText() {
        return responseMatcher.group(GROUP_DESCRIPTION);
    }

}
