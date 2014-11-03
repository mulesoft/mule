package org.mule.module.oauth2.internal;

public interface ClientApplicationCredentials
{

    /**
     * @return oauth client secret of the hosted application
     */
    String getClientSecret();

    /**
     * @return oauth client id of the hosted application
     */
    String getClientId();

}
