/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.construct.FlowConstruct;
import org.mule.module.http.api.HttpHeaders;
import org.mule.module.http.internal.HttpParser;
import org.mule.module.http.internal.domain.HttpEntity;
import org.mule.module.http.internal.domain.InputStreamHttpEntity;
import org.mule.module.http.internal.domain.MultipartHttpEntity;
import org.mule.module.http.internal.domain.request.HttpRequest;
import org.mule.module.http.internal.domain.request.HttpRequestContext;
import org.mule.module.http.internal.multipart.HttpPartDataSource;
import org.mule.transport.NullPayload;
import org.mule.util.IOUtils;

import com.google.common.net.MediaType;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.activation.DataHandler;

public class HttpRequestToMuleEvent
{

    public static MuleEvent transform(final HttpRequestContext requestContext, final MuleContext muleContext, final FlowConstruct flowConstruct, Boolean parseRequest, String listenerPath)
    {
        final HttpRequest request = requestContext.getRequest();
        final Collection<String> headerNames = request.getHeaderNames();
        Map<String, Object> inboundProperties = new HashMap<>();
        for (String headerName : headerNames)
        {
            final Collection<String> values = request.getHeaderValues(headerName);
            if (values.size() == 1)
            {
                inboundProperties.put(headerName, values.iterator().next());
            }
            else
            {
                inboundProperties.put(headerName, values);
            }
        }

        new HttpMessagePropertiesResolver()
                .setMethod(request.getMethod())
                .setProtocol(request.getProtocol().asString())
                .setUri(request.getUri())
                .setListenerPath(listenerPath)
                .setRemoteHostAddress(requestContext.getRemoteHostAddress().toString())
                .addPropertiesTo(inboundProperties);

        final Map<String, DataHandler> inboundAttachments = new HashMap<>();
        Object payload = NullPayload.getInstance();
        if (parseRequest)
        {
            final HttpEntity entity = request.getEntity();
            if (entity != null)
            {
                if (entity instanceof MultipartHttpEntity)
                {
                    inboundAttachments.putAll(HttpPartDataSource.createDataHandlerFrom(((MultipartHttpEntity) entity).getParts()));
                }
                else
                {
                    final String contentTypeValue = request.getHeaderValue(HttpHeaders.Names.CONTENT_TYPE);
                    if (contentTypeValue != null)
                    {
                        final MediaType mediaType = MediaType.parse(contentTypeValue);
                        if ((mediaType.type() + "/" + mediaType.subtype()).equals(HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED))
                        {
                            Charset charset = mediaType.charset().isPresent() ? mediaType.charset().get() : Charset.defaultCharset();
                            payload = HttpParser.decodeUrlEncodedBody(IOUtils.toString(((InputStreamHttpEntity) entity).getInputStream()), charset.name());
                        }
                        else if (entity instanceof InputStreamHttpEntity)
                        {
                            payload = ((InputStreamHttpEntity) entity).getInputStream();
                        }
                    }
                    else if (entity instanceof InputStreamHttpEntity)
                    {
                        payload = ((InputStreamHttpEntity) entity).getInputStream();
                    }
                }
            }
        }
        else
        {
            final InputStreamHttpEntity inputStreamEntity = request.getInputStreamEntity();
            if (inputStreamEntity != null)
            {
                payload = inputStreamEntity.getInputStream();
            }
        }
        final DefaultMuleMessage defaultMuleMessage = new DefaultMuleMessage(payload, inboundProperties, Collections.<String, Object>emptyMap(), inboundAttachments, muleContext);
        return new DefaultMuleEvent(defaultMuleMessage, MessageExchangePattern.REQUEST_RESPONSE, flowConstruct);
    }

}
