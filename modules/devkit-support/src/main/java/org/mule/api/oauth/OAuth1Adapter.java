
package org.mule.api.oauth;

import org.mule.api.oauth.exception.UnableToAcquireRequestTokenException;
import org.mule.common.security.oauth.exception.UnableToAcquireAccessTokenException;

public interface OAuth1Adapter extends OAuthAdapter
{

    public String authorize(java.util.Map<String, String> extraParameters,
                            String requestTokenUrl,
                            String accessTokenUrl,
                            String authorizationUrl,
                            String redirectUri) throws UnableToAcquireRequestTokenException;

    public void fetchAccessToken(String requestTokenUrl,
                                 String accessTokenUrl,
                                 String authorizationUrl,
                                 String redirectUri) throws UnableToAcquireAccessTokenException;

    public String getAccessTokenSecret();

    public void setAccessTokenSecret(String value);
}
