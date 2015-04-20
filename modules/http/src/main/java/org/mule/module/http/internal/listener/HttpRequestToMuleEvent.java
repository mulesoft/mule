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
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstruct;
import org.mule.endpoint.URIBuilder;
import org.mule.module.http.api.HttpHeaders;
import org.mule.module.http.internal.HttpParser;
import org.mule.module.http.internal.domain.EmptyHttpEntity;
import org.mule.module.http.internal.domain.HttpEntity;
import org.mule.module.http.internal.domain.InputStreamHttpEntity;
import org.mule.module.http.internal.domain.MultipartHttpEntity;
import org.mule.module.http.internal.domain.request.HttpRequest;
import org.mule.module.http.internal.domain.request.HttpRequestContext;
import org.mule.module.http.internal.multipart.HttpPartDataSource;
import org.mule.session.DefaultMuleSession;
import org.mule.transport.NullPayload;
import org.mule.util.IOUtils;
import org.mule.util.StringUtils;

import com.google.common.net.MediaType;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.activation.DataHandler;

public class HttpRequestToMuleEvent
{

    public static MuleEvent transform(final HttpRequestContext requestContext, final MuleContext muleContext, final FlowConstruct flowConstruct, Boolean parseRequest, ListenerPath listenerPath) throws HttpRequestParsingException
    {
        final HttpRequest request = requestContext.getRequest();
        final Collection<String> headerNames = request.getHeaderNames();
        Map<String, Object> inboundProperties = new HashMap<>();
        Map<String, Object> outboundProperties = new HashMap<>();
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
                .setRemoteHostAddress(resolveRemoteHostAddress(requestContext))
                .setScheme(requestContext.getScheme())
                .setClientCertificate(requestContext.getClientConnection().getClientCertificate())
                .addPropertiesTo(inboundProperties);

        final Map<String, DataHandler> inboundAttachments = new HashMap<>();
        Object payload = NullPayload.getInstance();
        if (parseRequest)
        {
            final HttpEntity entity = request.getEntity();
            if (entity != null && !(entity instanceof EmptyHttpEntity))
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
                        String encoding = mediaType.charset().isPresent() ? mediaType.charset().get().name() : Charset.defaultCharset().name();
                        outboundProperties.put(MuleProperties.MULE_ENCODING_PROPERTY, encoding);
                        if ((mediaType.type() + "/" + mediaType.subtype()).equals(HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED))
                        {
                            try
                            {
                                payload = HttpParser.decodeUrlEncodedBody(IOUtils.toString(((InputStreamHttpEntity) entity).getInputStream()), encoding);
                            }
                            catch (IllegalArgumentException e)
                            {
                                throw new HttpRequestParsingException("Cannot decode x-www-form-urlencoded payload", e);
                            }
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

        final DefaultMuleMessage defaultMuleMessage = new DefaultMuleMessage(payload, inboundProperties, outboundProperties, inboundAttachments, muleContext);
        return new DefaultMuleEvent(
                defaultMuleMessage,
                resolveUri(requestContext, listenerPath),
                MessageExchangePattern.REQUEST_RESPONSE,
                flowConstruct,
                new DefaultMuleSession());
    }

    private static URI resolveUri(final HttpRequestContext requestContext, final ListenerPath listenerPath)
    {
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setProtocol(requestContext.getScheme());
        uriBuilder.setHost(requestContext.getRequest().getHeaderValue("host"));
        uriBuilder.setPath(listenerPath.getResolvedPath());
        return URI.create(uriBuilder.toString());
    }

    private static String resolveRemoteHostAddress(final HttpRequestContext requestContext)
    {
        return StringUtils.defaultIfEmpty(
                requestContext.getRequest().getHeaderValue(HttpHeaders.Names.X_FORWARDED_FOR),
                requestContext.getClientConnection().getRemoteHostAddress().toString());
    }
}
