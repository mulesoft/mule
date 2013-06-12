
package org.mule.security.oauth;

import oauth.signpost.OAuthConsumer;

import org.mule.common.security.oauth.exception.UnableToAcquireAccessTokenException;
import org.mule.common.security.oauth.exception.UnableToAcquireRequestTokenException;

public interface OAuth1Adapter extends OAuthAdapter, OAuth1Connector
{

    /**
     * Sets requestTokenUrl
     * 
     * @param value a request token url
     */
    public void setRequestTokenUrl(String value);

    /**
     * Retrieves requestTokenUrl
     */
    public String getRequestTokenUrl();

    public String authorize(java.util.Map<String, String> extraParameters,
                            String requestTokenUrl,
                            String accessTokenUrl,
                            String authorizationUrl,
                            String redirectUri) throws UnableToAcquireRequestTokenException;

    public void fetchAccessToken(String requestTokenUrl,
                                 String accessTokenUrl,
                                 String authorizationUrl,
                                 String redirectUri) throws UnableToAcquireAccessTokenException;

    /**
     * Retrieves the accessTokenSecret
     */
    public String getAccessTokenSecret();

    /**
     * Sets access token secret
     * 
     * @param value an accessTokenSecret
     */
    public void setAccessTokenSecret(String value);
    
    /**
     * Returns the request token
     */
    public String getRequestToken();

    /**
     * Sets the request token
     * @param requestToken a request token
     */
    public void setRequestToken(String requestToken);

    /**
     * Gets the request token secret
     */
    public String getRequestTokenSecret();

    /**
     * Sets the request token secret
     */
    public void setRequestTokenSecret(String requestTokenSecret);
    
    /**
     * Returns an OAuthConsumer for this adapter
     * @return an instance of {@link oauth.signpost.OAuthConsumer}
     */
    public OAuthConsumer getConsumer();

    /**
     * Sets the OAuthConsumer
     * @param consumer an instance of {@link oauth.signpost.OAuthConsumer}
     */
    public void setConsumer(OAuthConsumer consumer);
    
    public OAuth1Manager getOauth1Manager();

}
