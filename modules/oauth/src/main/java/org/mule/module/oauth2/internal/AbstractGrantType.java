/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal;

import org.mule.module.http.api.HttpAuthentication;
import org.mule.module.http.api.requester.proxy.ProxyConfig;

/**
 * Common interface for all grant types must extend this interface.
 */
public abstract class AbstractGrantType implements HttpAuthentication, ApplicationCredentials
{

    protected ProxyConfig proxyConfig;

    /**
     * @param accessToken an ouath access token
     * @return the content of the HTTP authentication header.
     */
    public static String buildAuthorizationHeaderContent(String accessToken)
    {
        return "Bearer " + accessToken;
    }


    public ProxyConfig getProxyConfig()
    {
        return proxyConfig;
    }

    public void setProxyConfig(ProxyConfig proxyConfig)
    {
        this.proxyConfig = proxyConfig;
    }

}
