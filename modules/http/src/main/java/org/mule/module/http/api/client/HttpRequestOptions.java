/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.api.client;

import org.mule.api.client.Options;
import org.mule.module.http.api.requester.HttpRequesterConfig;

/**
 * Options that can be configured for making an http request using {@link org.mule.api.client.MuleClient}
 */
public interface HttpRequestOptions extends Options
{

    /**
     * @return HTTP method to send
     */
    String getMethod();

    /**
     * @return true if the request must always send the request using streaming
     */
    boolean alwaysStreamRequest();

    /**
     * @return true if the request must never send the request using streaming
     */
    boolean neverStreamRequest();

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


}
