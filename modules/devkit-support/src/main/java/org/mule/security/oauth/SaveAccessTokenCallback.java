
package org.mule.security.oauth;


public interface SaveAccessTokenCallback
{

    /**
     * Save access token and secret
     * 
     * @param accessToken Access token to be saved
     * @param accessTokenSecret Access token secret to be saved
     */
    void saveAccessToken(String accessToken, String accessTokenSecret);
}
