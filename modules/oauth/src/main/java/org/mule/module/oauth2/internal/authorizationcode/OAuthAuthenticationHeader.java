package org.mule.module.oauth2.internal.authorizationcode;

public class OAuthAuthenticationHeader
{

    public static String buildAuthorizationHeaderContent(String accessToken)
    {
        return "Bearer " + accessToken;
    }

}
