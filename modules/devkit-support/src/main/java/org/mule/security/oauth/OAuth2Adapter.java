
package org.mule.security.oauth;

import org.mule.common.security.oauth.exception.UnableToAcquireAccessTokenException;
import org.mule.common.security.oauth.exception.UnableToAcquireRequestTokenException;

public interface OAuth2Adapter extends OAuthAdapter
{

    /**
     * Build authorization URL and create the inbound endpoint for the callback
     * 
     * @param extraParameters Extra query string parameters that should be added to
     *            the authorization URL
     * @return The authorization URL
     */
    String authorize(java.util.Map<String, String> extraParameters, String accessTokenUrl, String redirectUri)
        throws UnableToAcquireRequestTokenException;

    /**
     * Acquire access token and secret
     * 
     * @throws UnableToAcquireAccessTokenException
     */
    void fetchAccessToken(String accessTokenUrl, String redirectUri)
        throws UnableToAcquireAccessTokenException;

    public boolean hasTokenExpired();

    void refreshAccessToken(String accessTokenUrl) throws UnableToAcquireAccessTokenException;

    String getRefreshToken();
}
