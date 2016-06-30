/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.listener;

import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.runtime.core.util.SystemUtils.getDefaultEncoding;
import static org.mule.runtime.module.http.api.HttpConstants.ALL_INTERFACES_IP;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.HOST;
import static org.mule.runtime.module.http.internal.HttpParser.decodeUrlEncodedBody;
import static org.mule.runtime.module.http.internal.domain.HttpProtocol.HTTP_0_9;
import static org.mule.runtime.module.http.internal.domain.HttpProtocol.HTTP_1_0;
import static org.mule.runtime.module.http.internal.multipart.HttpPartDataSource.createDataHandlerFrom;
import static org.mule.runtime.module.http.internal.util.HttpToMuleMessage.buildContentTypeDataType;

import org.mule.runtime.api.message.NullPayload;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeBuilder;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.session.DefaultMuleSession;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.module.http.api.HttpConstants;
import org.mule.runtime.module.http.api.HttpHeaders;
import org.mule.runtime.module.http.internal.ParameterMap;
import org.mule.runtime.module.http.internal.domain.EmptyHttpEntity;
import org.mule.runtime.module.http.internal.domain.HttpEntity;
import org.mule.runtime.module.http.internal.domain.InputStreamHttpEntity;
import org.mule.runtime.module.http.internal.domain.MultipartHttpEntity;
import org.mule.runtime.module.http.internal.domain.request.HttpRequest;
import org.mule.runtime.module.http.internal.domain.request.HttpRequestContext;

import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
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
        Map<String, Serializable> inboundProperties = new HashMap<>();
        Map<String, Serializable> outboundProperties = new HashMap<>();
        for (String headerName : headerNames)
        {
            final Collection<String> values = request.getHeaderValues(headerName);
            if (values.size() == 1)
            {
                inboundProperties.put(headerName, values.iterator().next());
            }
            else
            {
                inboundProperties.put(headerName, new ArrayList<>(values));
            }
        }

        new HttpMessagePropertiesResolver().setMethod(request.getMethod())
                                           .setProtocol(request.getProtocol().asString())
                                           .setUri(request.getUri())
                                           .setListenerPath(listenerPath)
                                           .setRemoteHostAddress(resolveRemoteHostAddress(requestContext))
                                           .setScheme(requestContext.getScheme())
                                           .setClientCertificate(requestContext.getClientConnection().getClientCertificate())
                                           .addPropertiesTo(inboundProperties);

        final Map<String, DataHandler> inboundAttachments = new HashMap<>();
        Object payload = NullPayload.getInstance();

        final DataType dataType = buildContentTypeDataType(request.getHeaderValueIgnoreCase(CONTENT_TYPE), getDefaultEncoding(muleContext));
        DataTypeBuilder dataTypeBuilder = DataType.builder(dataType);
        if (parseRequest)
        {
            final HttpEntity entity = request.getEntity();
            if (entity != null && !(entity instanceof EmptyHttpEntity))
            {
                if (entity instanceof MultipartHttpEntity)
                {
                    inboundAttachments.putAll(createDataHandlerFrom(((MultipartHttpEntity) entity).getParts()));
                }
                else
                {
                    if (dataType.getMediaType() != null)
                    {
                        if (dataType.getMediaType().matches(HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED))
                        {
                            try
                            {
                                payload = decodeUrlEncodedBody(IOUtils.toString(((InputStreamHttpEntity) entity).getInputStream()), dataType.getMediaType().getCharset().get());
                                dataTypeBuilder.type(ParameterMap.class);
                            }
                            catch (IllegalArgumentException e)
                            {
                                throw new HttpRequestParsingException("Cannot decode x-www-form-urlencoded payload", e);
                            }
                        }
                        else if (entity instanceof InputStreamHttpEntity)
                        {
                            payload = ((InputStreamHttpEntity) entity).getInputStream();
                            dataTypeBuilder.type(InputStream.class);
                        }
                    }
                    else if (entity instanceof InputStreamHttpEntity)
                    {
                        payload = ((InputStreamHttpEntity) entity).getInputStream();
                        dataTypeBuilder.type(InputStream.class);
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
                dataTypeBuilder.type(InputStream.class);
            }
        }

        final DefaultMuleMessage defaultMuleMessage = new DefaultMuleMessage(payload, inboundProperties, outboundProperties, inboundAttachments, dataTypeBuilder.build());
        return new DefaultMuleEvent(
                defaultMuleMessage,
                resolveUri(requestContext),
                REQUEST_RESPONSE,
                flowConstruct,
                new DefaultMuleSession());
    }

    private static URI resolveUri(final HttpRequestContext requestContext)
    {
        try
        {
            String hostAndPort = resolveTargetHost(requestContext.getRequest());
            String[] hostAndPortParts = hostAndPort.split(":");
            String host = hostAndPortParts[0];
            int port = requestContext.getScheme().equals(HttpConstants.Protocols.HTTP) ? 80 : 4343;
            if (hostAndPortParts.length > 1)
            {
                port = Integer.valueOf(hostAndPortParts[1]);
            }
            return new URI(requestContext.getScheme(), null, host, port, requestContext.getRequest().getPath(), null, null);
        }
        catch (URISyntaxException e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    /**
     * See <a href="http://www8.org/w8-papers/5c-protocols/key/key.html#SECTION00070000000000000000" >Internet address
     * conservation</a>.
     */
    private static String resolveTargetHost(HttpRequest request)
    {
        String hostHeaderValue = request.getHeaderValueIgnoreCase(HOST);
        if (HTTP_1_0.equals(request.getProtocol()) || HTTP_0_9.equals(request.getProtocol()))
        {
            return hostHeaderValue == null ? ALL_INTERFACES_IP : hostHeaderValue;
        }
        else
        {
            if (hostHeaderValue == null)
            {
                throw new IllegalArgumentException("Missing 'host' header");
            }
            else
            {
                return hostHeaderValue;
            }
        }
    }

    private static String resolveRemoteHostAddress(final HttpRequestContext requestContext)
    {
        return requestContext.getClientConnection().getRemoteHostAddress().toString();
    }
}
