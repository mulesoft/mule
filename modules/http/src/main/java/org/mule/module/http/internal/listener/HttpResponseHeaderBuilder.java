/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener;

import org.mule.api.MuleRuntimeException;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.http.api.HttpHeaders;
import org.mule.util.CaseInsensitiveMapWrapper;

import com.google.common.base.Supplier;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class HttpResponseHeaderBuilder
{

    private List<String> calculatedHeadersNames = Arrays.asList(HttpHeaders.Names.TRANSFER_ENCODING, HttpHeaders.Names.CONTENT_LENGTH);

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

    private void addSimpleValue(String headerName, String headerValue)
    {
        if (headers.containsValue(headerName))
        {
            failIfHeaderDoesNotSupportMultipleValues(headerName);
        }
        headers.put(headerName, headerValue);
    }

    public String getContentType()
    {
        return getSimpleValue(HttpHeaders.Names.CONTENT_TYPE);
    }

    public String getTransferEncoding()
    {
        return getSimpleValue(HttpHeaders.Names.TRANSFER_ENCODING);
    }

    public String getContentLength()
    {
        return getSimpleValue(HttpHeaders.Names.CONTENT_LENGTH);
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
        addSimpleValue(HttpHeaders.Names.CONTENT_TYPE, multipartFormData);
    }

    public void addContentLenght(String calculatedContentLenght)
    {
        addSimpleValue(HttpHeaders.Names.CONTENT_LENGTH, calculatedContentLenght);
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
