/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.api.requester;

import org.mule.api.client.OperationOptionsConfig;

/**
 * Common configuration for an HTTP request operation
 *
 * @param <BuilderType> the builder class
 */
public interface HttpRequestOperationConfig<BuilderType> extends OperationOptionsConfig<BuilderType>
{

    /**
     * @param method HTTP method for the HTTP request
     *
     * @return the builder
     */
    BuilderType method(String method);

    /**
     * Configures the requester to follows redirects
     *
     * @return the builder
     */
    BuilderType enableFollowsRedirect();

    /**
     * Configures the requester to not follow redirects
     *
     * @return the builder
     */
    BuilderType disableFollowsRedirect();

    /**
     * Configures if the HTTP request should do streaming (transfer-encoding chunk).
     *
     * @return the builder
     */
    BuilderType requestStreamingMode(HttpStreamingType mode);

    /**
     * @param requestConfig the config to use for the requester
     * @return the builder
     */
    BuilderType requestConfig(HttpRequesterConfig requestConfig);

    /**
     * Disables the status code validation for the response
     *
     * @return the builder
     */
    BuilderType disableStatusCodeValidation();

    /**
     * Disables the http response parsing
     *
     * @return the builder
     */
    BuilderType disableParseResponse();

}
