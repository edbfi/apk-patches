// SPDX-License-Identifier: AGPL-3.0-or-later

package app.edbfi.extension.shared.oauth;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class OAuthRedirectTest {
    private static final String REDIRECT = "edbfi.fixture://oauth/callback";

    @Test
    public void recognizesAuthorizationAndDenialResponses() {
        assertTrue(OAuthRedirect.matches(REDIRECT + "?state=sample&code=sample", REDIRECT));
        assertTrue(OAuthRedirect.matches(REDIRECT + "?error=access_denied&state=sample", REDIRECT));

        String registered = REDIRECT + "?channel=one%20two";
        assertTrue(OAuthRedirect.matches(registered + "&state=sample&code=sample", registered));
        assertFalse(OAuthRedirect.matches(REDIRECT + "?state=sample&code=sample", registered));
    }

    @Test
    public void rejectsOtherEndpointsAndMalformedResponses() {
        assertFalse(OAuthRedirect.matches(null, REDIRECT));
        assertFalse(OAuthRedirect.matches(REDIRECT + "/other?state=s&code=c", REDIRECT));
        assertFalse(OAuthRedirect.matches("edbfi.fixture://oauth.example/callback?state=s&code=c", REDIRECT));
        assertFalse(OAuthRedirect.matches(REDIRECT + "?code=c", REDIRECT));
        assertFalse(OAuthRedirect.matches(REDIRECT + "?state=&code=c", REDIRECT));
        assertFalse(OAuthRedirect.matches(REDIRECT + "?state=s&state=t&code=c", REDIRECT));
        assertFalse(OAuthRedirect.matches(REDIRECT + "?state=s&code=c&code=d", REDIRECT));
        assertFalse(OAuthRedirect.matches(REDIRECT + "?state=s&code=c&error=access_denied", REDIRECT));
        assertFalse(OAuthRedirect.matches(REDIRECT + "?state=s&%73tate=t&code=c", REDIRECT));
        assertFalse(OAuthRedirect.matches(REDIRECT + "?state=s&code=c#fragment", REDIRECT));
    }
}
