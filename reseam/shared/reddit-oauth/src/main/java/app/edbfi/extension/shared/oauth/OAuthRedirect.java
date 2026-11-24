// SPDX-License-Identifier: AGPL-3.0-or-later

package app.edbfi.extension.shared.oauth;

import app.reseam.runtime.Export;
import java.net.URI;
import java.net.URISyntaxException;

public final class OAuthRedirect {
    private OAuthRedirect() {}

    @Export
    public static boolean matchesEndpoint(String callback, String redirect) {
        if (callback == null) return false;
        // Match the address even when the query or fragment is syntactically invalid.
        String actual = endpoint(callback);
        String registered = endpoint(redirect);
        if (actual.equals(registered)) return true;

        try {
            URI origin = new URI(registered);
            boolean https = "https".equalsIgnoreCase(origin.getScheme());
            if (!https && !"http".equalsIgnoreCase(origin.getScheme())) return false;

            URI candidate = new URI(actual);
            int defaultPort = https ? 443 : 80;
            return origin.getScheme().equalsIgnoreCase(candidate.getScheme())
                && origin.getHost() != null && origin.getHost().equalsIgnoreCase(candidate.getHost())
                && candidate.getRawUserInfo() == null
                && port(origin, defaultPort) == port(candidate, defaultPort)
                && path(origin).equals(path(candidate));
        } catch (URISyntaxException ignored) {
            return false;
        }
    }

    private static String endpoint(String address) {
        int end = address.length();
        int query = address.indexOf('?');
        int fragment = address.indexOf('#');
        if (query >= 0) end = query;
        if (fragment >= 0 && fragment < end) end = fragment;
        return address.substring(0, end);
    }

    private static int port(URI uri, int defaultPort) {
        return uri.getPort() == -1 ? defaultPort : uri.getPort();
    }

    private static String path(URI uri) {
        String path = uri.getRawPath();
        return path == null || path.isEmpty() ? "/" : path;
    }

    @Export
    public static boolean validResponse(String callback, String redirect) {
        // Called only after the WebView has matched the registered endpoint.
        String query;
        try {
            URI uri = new URI(callback);
            if (uri.getRawFragment() != null) return false;
            query = uri.getRawQuery();
            if (query == null) return false;
        } catch (URISyntaxException ignored) {
            return false;
        }

        int fixedQuery = redirect.indexOf('?');
        if (fixedQuery >= 0) {
            String prefix = redirect.substring(fixedQuery + 1) + "&";
            if (!query.startsWith(prefix)) return false;
            query = query.substring(prefix.length());
        }
        boolean state = false;
        boolean result = false;
        for (String parameter : query.split("&", -1)) {
            int separator = parameter.indexOf('=');
            String name = separator < 0 ? parameter : parameter.substring(0, separator);
            // The native handlers look up the literal query names.
            if (name.isEmpty() || name.indexOf('%') >= 0) return false;
            if (name.equals("state")) {
                if (state || separator < 0 || separator == parameter.length() - 1) return false;
                state = true;
            } else if (name.equals("code") || name.equals("error")) {
                if (result || separator < 0 || separator == parameter.length() - 1) return false;
                result = true;
            }
        }
        return state && result;
    }
}
