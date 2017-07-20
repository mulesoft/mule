/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.api.requester.proxy;

/**
 *  HTTP proxy configuration for making http requests
 */
public interface ProxyConfig
{

    /**
     * @return the global config name. May be null.
     */
    public String getName();

    /**
     * @return the http proxy host
     */
    public String getHost();

    /**
     * @return the http proxy port
     */
    public int getPort();

    /**
     * @return the http proxy authentication username
     */
    public String getUsername();

    /**
     * @return the http proxy authentication password
     */
    public String getPassword();

    /**
     * @return A list of hosts separated by |, which specifies that the proxy must not be used
     */
    public String getNonProxyHosts();

}
