
package org.mule.security.oauth;

import org.mule.common.security.oauth.exception.NotAuthorizedException;
import org.mule.security.oauth.callback.RestoreAccessTokenCallback;
import org.mule.security.oauth.callback.SaveAccessTokenCallback;

import java.io.Serializable;

public interface OAuthAdapter extends Serializable
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

    public void setAccessTokenUrl(String url);

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

}
