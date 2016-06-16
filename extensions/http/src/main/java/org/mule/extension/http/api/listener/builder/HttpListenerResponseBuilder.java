/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.listener.builder;

import org.mule.extension.http.api.HttpMessageBuilder;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.activation.DataHandler;

/**
 * Component that specifies how to create a proper HTTP response.
 *
 * @since 4.0
 */
@Alias("simple-response-builder")
public class HttpListenerResponseBuilder extends HttpMessageBuilder
{
    /**
     * HTTP status code the response should have.
     */
    @Parameter
    @Optional
    private Function<MuleEvent, Integer> statusCode;

    /**
     * HTTP reason phrase the response should have.
     */
    @Parameter
    @Optional
    private Function<MuleEvent, String> reasonPhrase;

    /**
     * HTTP headers the response should have, as an expression. Will override the headers attribute.
     */
    @Parameter
    @Optional
    private Function<MuleEvent, Map> headersRef;

    /**
     * HTTP parts the message should include, as an expression. Will override the parts attribute.
     */
    @Parameter
    @Optional
    private Function<MuleEvent, List> partsRef;

    public Integer getStatusCode(MuleEvent event)
    {
        return statusCode != null ? statusCode.apply(event) : null;
    }

    public String getReasonPhrase(MuleEvent event)
    {
        return reasonPhrase != null ? reasonPhrase.apply(event) : null;
    }

    public Map<String, String> getHeaders(MuleEvent event)
    {
        return headersRef != null ? headersRef.apply(event) : headers;
    }

    public Map<String, DataHandler> getParts(MuleEvent event)
    {
        return partsRef != null ? getResolvedParts(partsRef.apply(event)) : getParts();
    }
}