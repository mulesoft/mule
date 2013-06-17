
package org.mule.security.oauth;

import oauth.signpost.OAuthConsumer;

import org.mule.common.security.oauth.exception.UnableToAcquireAccessTokenException;
import org.mule.common.security.oauth.exception.UnableToAcquireRequestTokenException;

import java.util.Map;

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

    /**
     * Builds the authorization url to initiate the OAuth dance
     * 
     * @param extraParameters provider specific extra parameters
     * @param requestTokenUrl the url of the request token server
     * @param accessTokenUrl the url of the access token server
     * @param authorizationUrl the url of the authorization server
     * @param redirectUri the redirection uri
     * @return a String with the authorization url
     * @throws UnableToAcquireRequestTokenException
     */
    public String authorize(Map<String, String> extraParameters,
                            String requestTokenUrl,
                            String accessTokenUrl,
                            String authorizationUrl,
                            String redirectUri) throws UnableToAcquireRequestTokenException;

    /**
     * Fetches an access token and stores it into this adapter
     * 
     * @param requestTokenUrl the url of the request token server
     * @param accessTokenUrl the url of the access token server
     * @param authorizationUrl the url of the authorization server
     * @param redirectUri the redirection uri
     * @throws UnableToAcquireAccessTokenException
     */
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
     * 
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
     * 
     * @return an instance of {@link oauth.signpost.OAuthConsumer}
     */
    public OAuthConsumer getConsumer();

    /**
     * Sets the OAuthConsumer
     * 
     * @param consumer an instance of {@link oauth.signpost.OAuthConsumer}
     */
    public void setConsumer(OAuthConsumer consumer);

    /**
     * Sets the adapter to a blank unauthorized state
     */
    public void reset();

    /**
     * Gets an instance of {@link org.mule.security.oauth.OAuth1Manager} serving this
     * adapter
     * 
     * @return an instance of {@link org.mule.security.oauth.OAuth1Manager}
     */
    public OAuth1Manager getOauth1Manager();

}
