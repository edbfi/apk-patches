// SPDX-License-Identifier: AGPL-3.0-or-later

package app.edbfi.extension.shared.oauth;

import app.reseam.runtime.Export;
import java.net.URI;
import java.net.URISyntaxException;

public final class OAuthRedirect {
    private OAuthRedirect() {}

    @Export
    public static boolean matches(String callback, String redirect) {
        String querySeparator = redirect.contains("?") ? "&" : "?";
        if (callback == null || !callback.startsWith(redirect + querySeparator)) {
            return false;
        }
        try {
            URI uri = new URI(callback);
            if (uri.getRawFragment() != null) {
                return false;
            }
            int states = 0;
            int codes = 0;
            int errors = 0;
            for (String parameter : uri.getRawQuery().split("&")) {
                int separator = parameter.indexOf('=');
                String name = separator < 0 ? parameter : parameter.substring(0, separator);
                // The native handlers look up the literal query names.
                if (name.indexOf('%') >= 0) return false;
                if (name.equals("state") || name.equals("code") || name.equals("error")) {
                    if (separator < 0 || separator == parameter.length() - 1) return false;
                    switch (name) {
                        case "state": states++; break;
                        case "code": codes++; break;
                        case "error": errors++; break;
                        default: break;
                    }
                }
            }
            return states == 1 && codes + errors == 1;
        } catch (URISyntaxException ignored) {
            return false;
        }
    }
}
