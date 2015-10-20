/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.api.requester;

import org.mule.api.AnnotatedObject;
import org.mule.api.NamedObject;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.module.http.api.HttpAuthentication;
import org.mule.module.http.api.requester.proxy.ProxyConfig;
import org.mule.transport.ssl.api.TlsContextFactory;

/**
 * Configuration object for an {@code HttpRequester}.
 *
 * Some of the configuration attributes can be dynamic meaning that the
 * configuration value to use will change based on the message at the moment
 * of the request execution.
 *
 * This object represents the XML request-config elements in the configuration.
 *
 * If a new HttpRequesterConfig needs to be created programmatically then use
 * {@link org.mule.module.http.api.requester.HttpRequesterConfigBuilder}
 */
public interface HttpRequesterConfig extends NamedObject, AnnotatedObject, Startable, Stoppable
{

    /**
     * @return the base path
     */
    String getBasePath();

    /**
     * @return the host to be called
     */
    String getHost();

    /**
     * @return the port to connect to
     */
    String getPort();

    /**
     * @return configuration for enabling or disabling follow redirects. It may be dynamic.
     */
    String getFollowRedirects();

    /**
     * @return the streaming mode for making the request.
     * Defines if the request will be sent using Transfer-Encoding: chunked. It may be dynamic
     */
    String getRequestStreamingMode();

    /**
     * @return if the request will contain or not a body. It may be dynamic.
     */
    String getSendBodyMode();

    /**
     * @return mode for parsing a response. It may contain an expression if it's dynamic.
     */
    String getParseResponse();

    /**
     * @return maximum time to wait for a response
     */
    String getResponseTimeout();

    /**
     * @return authentication mechanism when making request
     */
    HttpAuthentication getAuthentication();

    /**
     * @return the tls configuration when using HTTPS
     */
    TlsContextFactory getTlsContext();

    /**
     * @return http proxy configuration to be used
     */
    ProxyConfig getProxyConfig();

}
