/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener;

import static java.util.Arrays.asList;
import static org.mule.module.http.api.HttpHeaders.Names.CONTENT_LENGTH;
import static org.mule.module.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.module.http.api.HttpHeaders.Names.TRANSFER_ENCODING;

import org.mule.api.MuleRuntimeException;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.CaseInsensitiveMapWrapper;

import com.google.common.base.Supplier;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpResponseHeaderBuilder
{
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpResponseHeaderBuilder.class);

    private List<String> calculatedHeadersNames = asList(TRANSFER_ENCODING, CONTENT_LENGTH);
    private List<String> uniqueHeadersNames = asList(TRANSFER_ENCODING.toLowerCase(), CONTENT_LENGTH.toLowerCase(), CONTENT_TYPE.toLowerCase());

    Multimap<String, String> headers =
            Multimaps.newMultimap(new CaseInsensitiveMapWrapper<Collection<String>>(HashMap.class), new Supplier<Collection<String>>(){
                @Override
                public Collection<String> get(){ return Sets.newHashSet(); }
            });

    public void addHeader(String headerName, Object headerValue)
    {
        if (headerValue instanceof Iterable)
        {
            failIfHeaderDoesNotSupportMultipleValues(headerName);
            Iterable values = (Iterable) headerValue;
            for (Object value : values)
            {
                addSimpleValue(headerName, value.toString());
            }
        }
        else if (headerValue instanceof String[])
        {
            failIfHeaderDoesNotSupportMultipleValues(headerName);
            String[] values = (String[]) headerValue;
            for (String value : values)
            {
                addSimpleValue(headerName, value);
            }
        }
        else
        {
            addSimpleValue(headerName, headerValue.toString());
        }
    }

    public Collection<String> removeHeader(String headerName)
    {
        return headers.removeAll(headerName);
    }

    private void failIfHeaderDoesNotSupportMultipleValues(String headerName)
    {
        if (calculatedHeadersNames.contains(headerName))
        {
            throw new MuleRuntimeException(CoreMessages.createStaticMessage("Header: " + headerName + " does not support multiple values"));
        }
    }

    /**
     *
     * Check if the given header allows multiple values. In case it doesn't and the header already has a value,
     * a warn message is shown and the older values are removed from the map.
     *
     * @param headerName the header name to check if it has already a value set.
     */
    private void logIfHeaderDoesNotSupportMultipleValues(String headerName)
    {
        if (uniqueHeadersNames.contains(headerName.toLowerCase()))
        {
            final Collection<String> values = headers.removeAll(headerName);
            LOGGER.warn("Header: " + headerName + " does not support multiple values. Removing {}", values);
        }
    }

    /**
     * Adds the given header to the map. In case the header already has a value, {@link #logIfHeaderDoesNotSupportMultipleValues}
     * is called instead of {@link #failIfHeaderDoesNotSupportMultipleValues(String)} to
     * keep backwards compatibility.
     * This behavior will change in Mule 4.
     *
     * @param headerName the header name to be added.
     * @param headerValue the header value to be added.
     */
    private void addSimpleValue(String headerName, String headerValue)
    {
        if (headers.containsKey(headerName))
        {
            logIfHeaderDoesNotSupportMultipleValues(headerName);
        }
        headers.put(headerName, headerValue);
    }

    public String getContentType()
    {
        return getSimpleValue(CONTENT_TYPE);
    }

    public String getTransferEncoding()
    {
        return getSimpleValue(TRANSFER_ENCODING);
    }

    public String getContentLength()
    {
        return getSimpleValue(CONTENT_LENGTH);
    }

    private String getSimpleValue(String header)
    {
        if (!headers.containsKey(header))
        {
            return null;
        }
        return (String)((Collection)headers.get(header)).iterator().next();
    }

    public void addContentType(String multipartFormData)
    {
        addSimpleValue(CONTENT_TYPE, multipartFormData);
    }

    public void setContentLength(String calculatedContentLength)
    {
        removeHeader(CONTENT_LENGTH);
        addSimpleValue(CONTENT_LENGTH, calculatedContentLength);
    }

    public Collection<String> getHeaderNames()
    {
        return headers.keySet();
    }

    public Collection<String> getHeader(String headerName)
    {
        return headers.get(headerName);
    }
}
