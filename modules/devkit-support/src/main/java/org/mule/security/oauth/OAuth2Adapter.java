
package org.mule.security.oauth;

import org.mule.common.security.oauth.exception.UnableToAcquireAccessTokenException;
import org.mule.common.security.oauth.exception.UnableToAcquireRequestTokenException;

import java.util.Map;

public interface OAuth2Adapter extends OAuthAdapter
{

    /**
     * Build authorization URL and create the inbound endpoint for the callback
     * 
     * @param extraParameters Extra query string parameters that should be added to
     *            the authorization URL
     * @return The authorization URL
     */
    public String authorize(Map<String, String> extraParameters, String accessTokenUrl, String redirectUri)
        throws UnableToAcquireRequestTokenException;

    public boolean hasTokenExpired();

    public void refreshAccessToken(String accessTokenUrl) throws UnableToAcquireAccessTokenException;

    public String getRefreshToken();
}
