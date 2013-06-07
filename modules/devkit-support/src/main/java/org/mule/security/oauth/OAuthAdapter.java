
package org.mule.security.oauth;

import org.mule.common.security.oauth.OAuthConnector;
import org.mule.common.security.oauth.exception.NotAuthorizedException;
import org.mule.common.security.oauth.exception.UnableToAcquireAccessTokenException;
import org.mule.security.oauth.callback.RestoreAccessTokenCallback;
import org.mule.security.oauth.callback.SaveAccessTokenCallback;

import java.util.Date;
import java.util.regex.Pattern;

public interface OAuthAdapter extends OAuthConnector
{

    /**
     * Retrieve OAuth verifier
     * 
     * @return A String representing the OAuth verifier
     */
    public String getOauthVerifier();

    /**
     * Set OAuth verifier
     * 
     * @param value OAuth verifier to set
     */
    public void setOauthVerifier(String value);

    /**
     * Retrieve access token
     */
    public String getAccessToken();

    /**
     * Retrieve refresh token
     */
    public String getRefreshToken();

    /**
     * Set refresh token
     */
    public void setRefreshToken(String refreshToken);

    public void setAccessTokenUrl(String url);

    public void setConsumerKey(String consumerKey);

    public void setConsumerSecret(String consumerSecret);

    public void setAccessToken(String accessToken);

    public void setAuthorizationUrl(String authorizationUrl);

    /**
     * Set the callback to be called when the access token and secret need to be
     * saved for later restoration
     * 
     * @param saveCallback Callback to be called
     */
    public void setOauthSaveAccessToken(SaveAccessTokenCallback saveCallback);

    /**
     * Set the callback to be called when the access token and secret need to be
     * restored
     * 
     * @param restoreCallback Callback to be called
     */
    public void setOauthRestoreAccessToken(RestoreAccessTokenCallback restoreCallback);

    /**
     * Get the callback to be called when the access token and secret need to be
     * saved for later restoration
     */
    public SaveAccessTokenCallback getOauthSaveAccessToken();

    /**
     * Get the callback to be called when the access token and secret need to be
     * restored
     */
    public RestoreAccessTokenCallback getOauthRestoreAccessToken();

    public void hasBeenAuthorized() throws NotAuthorizedException;

    public void fetchAccessToken(String accessTokenUrl, String redirectUri)
        throws UnableToAcquireAccessTokenException;
    
    /**
     * Returns a compiled {@link java.util.regex.Pattern}
     * which can be used to extract the access code from a String 
     */
    public Pattern getAccessCodePattern();
    
    /**
     * Returns a compiled {@link java.util.regex.Pattern}
     * which can be used to extract the refresh token from a String 
     */
    public Pattern getRefreshTokenPattern();
    
    /**
     * Returns a compiled {@link java.util.regex.Pattern}
     * which can be used to extract the expiration time from a String 
     */
    public Pattern getExpirationTimePattern();
    
    /**
     * Sets expiration
     */
    public void setExpiration(Date value);

}
