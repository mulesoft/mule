/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.api.requester.authentication;

import static org.mule.module.http.internal.request.HttpAuthenticationType.BASIC;

import org.mule.module.http.api.HttpAuthentication;
import org.mule.module.http.internal.request.DefaultHttpAuthentication;

/**
 * Builder for HTTP basic authentication credentials
 */
public class BasicAuthenticationBuilder
{

    private DefaultHttpAuthentication basicAuthentication = new DefaultHttpAuthentication(BASIC);

    /**
     * @param username basic authentication username
     * @return the builder
     */
    public BasicAuthenticationBuilder setUsername(String username)
    {
        basicAuthentication.setUsername(username);
        return this;
    }

    /**
     * @param password basic authentication password
     * @return this;
     */
    public BasicAuthenticationBuilder setPassword(String password)
    {
        basicAuthentication.setPassword(password);
        return this;
    }

    /**
     * @param preemptive configures preemptive authentication or not (when true, the authentication
     *                   header is sent in the first request).
     * @return this
     */
    public BasicAuthenticationBuilder setPreemptive(boolean preemptive)
    {
        basicAuthentication.setPreemptive(preemptive);
        return this;
    }

    /**
     * @return the authentication configuration
     */
    public HttpAuthentication build()
    {
        return this.basicAuthentication;
    }

}
