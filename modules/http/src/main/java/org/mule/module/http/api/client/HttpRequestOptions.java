/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.api.client;

import org.mule.api.client.OperationOptions;
import org.mule.module.http.api.requester.HttpRequesterConfig;
import org.mule.module.http.api.requester.HttpStreamingType;
import org.mule.transport.ssl.api.TlsContextFactory;

/**
 * Options that can be configured for making an HTTP request using {@link org.mule.api.client.MuleClient}
 *
 * Implementations of this class must redefine {@link Object#hashCode()} and {@link java.lang.Object#equals(Object)} since the may be used as key in a map
 */
public interface HttpRequestOptions extends OperationOptions
{

    /**
     * @return HTTP method to send
     */
    String getMethod();

    /**
     * @return the streaming mode for sending the HTTP request.
     */
    HttpStreamingType getRequestStreamingMode();

    /**
     * @return true if the response from the request should not be parsed and return plain as an input stream.
     */
    boolean isParseResponseDisabled();

    /**
     * @return the {@link org.mule.module.http.api.requester.HttpRequesterConfig} to use for sending the request
     */
    HttpRequesterConfig getRequesterConfig();

    /**
     * @return true if should follows redirect responses
     */
    Boolean isFollowsRedirect();

    /**
     * @return maximum amount of time to wait for the response of the request
     */
    Long getResponseTimeout();

    /**
     * @return true if should not validate the status code
     */
    boolean isStatusCodeValidationDisabled();

    /**
     * @return the TLS configuration for making the request
     */
    TlsContextFactory getTlsContextFactory();


}
