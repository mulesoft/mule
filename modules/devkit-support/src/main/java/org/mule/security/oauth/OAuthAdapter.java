
package org.mule.security.oauth;

import org.mule.common.security.oauth.OAuthConnector;
import org.mule.common.security.oauth.exception.NotAuthorizedException;
import org.mule.common.security.oauth.exception.UnableToAcquireAccessTokenException;

public interface OAuthAdapter extends OAuthConnector
{

    /**
     * Retrieve OAuth verifier
     * 
     * @return A String representing the OAuth verifier
     */
    String getOauthVerifier();

    /**
     * Set OAuth verifier
     * 
     * @param value OAuth verifier to set
     */
    void setOauthVerifier(String value);

    /**
     * Retrieve access token
     */
    String getAccessToken();

    /**
     * Retrieve refresh token
     */
    String getRefreshToken();

    /**
     * Set refresh token
     */
    void setRefreshToken(String refreshToken);

    /**
     * Set access token
     * 
     * @param value
     */
    void setAccessToken(String value);

    /**
     * Set the callback to be called when the access token and secret need to be
     * saved for later restoration
     * 
     * @param saveCallback Callback to be called
     */
    void setOauthSaveAccessToken(SaveAccessTokenCallback saveCallback);

    /**
     * Set the callback to be called when the access token and secret need to be
     * restored
     * 
     * @param restoreCallback Callback to be called
     */
    void setOauthRestoreAccessToken(RestoreAccessTokenCallback restoreCallback);

    /**
     * Get the callback to be called when the access token and secret need to be
     * saved for later restoration
     */
    SaveAccessTokenCallback getOauthSaveAccessToken();

    /**
     * Get the callback to be called when the access token and secret need to be
     * restored
     */
    RestoreAccessTokenCallback getOauthRestoreAccessToken();

    void hasBeenAuthorized() throws NotAuthorizedException;

    public void fetchAccessToken(String accessTokenUrl, String redirectUri)
        throws UnableToAcquireAccessTokenException;

}
