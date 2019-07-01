/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener;

import static org.mule.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.api.config.MuleProperties.MULE_CORRELATION_ID_PROPERTY;
import static org.mule.api.config.MuleProperties.MULE_ENCODING_PROPERTY;
import static org.mule.module.http.api.HttpConstants.ALL_INTERFACES_IP;
import static org.mule.module.http.api.HttpConstants.HttpProperties.COMPATIBILITY_IGNORE_CORRELATION_ID;
import static org.mule.module.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.module.http.api.HttpHeaders.Names.HOST;
import static org.mule.module.http.api.HttpHeaders.Names.X_CORRELATION_ID;
import static org.mule.module.http.internal.HttpParser.decodeUrlEncodedBody;
import static org.mule.module.http.internal.domain.HttpProtocol.HTTP_0_9;
import static org.mule.module.http.internal.domain.HttpProtocol.HTTP_1_0;
import static org.mule.module.http.internal.multipart.HttpPartDataSource.createDataHandlerFrom;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleRuntimeException;
import org.mule.api.construct.FlowConstruct;
import org.mule.endpoint.URIBuilder;
import org.mule.module.http.api.HttpHeaders;
import org.mule.module.http.internal.domain.EmptyHttpEntity;
import org.mule.module.http.internal.domain.HttpEntity;
import org.mule.module.http.internal.domain.InputStreamHttpEntity;
import org.mule.module.http.internal.domain.MultipartHttpEntity;
import org.mule.module.http.internal.domain.request.HttpRequest;
import org.mule.module.http.internal.domain.request.HttpRequestContext;
import org.mule.session.DefaultMuleSession;
import org.mule.transport.NullPayload;
import org.mule.util.BackwardsCompatibilityPropertyChecker;
import org.mule.util.IOUtils;

import com.google.common.net.MediaType;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.activation.DataHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpRequestToMuleEvent
{

    private static Logger LOGGER = LoggerFactory.getLogger(HttpRequestToMuleEvent.class);
    public static final BackwardsCompatibilityPropertyChecker
      IGNORE_CORRELATION_ID = new BackwardsCompatibilityPropertyChecker(COMPATIBILITY_IGNORE_CORRELATION_ID);

    private static boolean ignoreCorrelationId = IGNORE_CORRELATION_ID.isEnabled();

    public static void resetIgnoreCorrelationId() {
        ignoreCorrelationId = IGNORE_CORRELATION_ID.isEnabled();
    }
    
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

        new HttpMessagePropertiesResolver().setMethod(request.getMethod())
                                           .setProtocol(request.getProtocol().asString())
                                           .setUri(request.getUri())
                                           .setListenerPath(listenerPath)
                                           .setRemoteHostAddress(resolveRemoteHostAddress(requestContext))
                                           .setScheme(requestContext.getScheme())
                                           .setClientCertificate(requestContext.getClientConnection().getClientCertificate())
                                           .setSslSessionProperties(requestContext.getClientConnection().getSslSession())
                                           .addPropertiesTo(inboundProperties);

        Map<String, DataHandler> inboundAttachments = null;
        Object payload = NullPayload.getInstance();
        if (parseRequest)
        {
            final HttpEntity entity;
            try
            {
                entity = request.getEntity();
            }
            catch (MuleRuntimeException e)
            {
                //Because request.getEntity() will attempt to lazily parse
                //multipart request payloads, if this point is reached it can
                //only be because an error occurred at the parsing stage.
                throw new HttpRequestParsingException("Unable to parse multipart payload", e);
            }
            if (entity != null && !(entity instanceof EmptyHttpEntity))
            {
                if (entity instanceof MultipartHttpEntity)
                {
                    inboundAttachments = createDataHandlerFrom(((MultipartHttpEntity) entity).getParts());
                }
                else
                {
                    final String contentTypeValue = request.getHeaderValueIgnoreCase(CONTENT_TYPE);
                    if (contentTypeValue != null)
                    {
                        final MediaType mediaType = MediaType.parse(contentTypeValue);
                        String encoding = mediaType.charset().isPresent() ? mediaType.charset().get().name() : Charset.defaultCharset().name();
                        outboundProperties.put(MULE_ENCODING_PROPERTY, encoding);
                        if ((mediaType.type() + "/" + mediaType.subtype()).equals(HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED))
                        {
                            try
                            {
                                payload = decodeUrlEncodedBody(IOUtils.toString(((InputStreamHttpEntity) entity).getInputStream()), encoding);
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

        if (!ignoreCorrelationId)
        {
            resolveCorrelationId(defaultMuleMessage);
        }

        return new DefaultMuleEvent(
                defaultMuleMessage,
                resolveUri(requestContext),
                REQUEST_RESPONSE,
                flowConstruct,
                new DefaultMuleSession());
    }

    private static void resolveCorrelationId(DefaultMuleMessage defaultMuleMessage) {
        String xCorrelationId = defaultMuleMessage.getInboundProperty(X_CORRELATION_ID);
        String muleCorrelationId = defaultMuleMessage.getInboundProperty(MULE_CORRELATION_ID_PROPERTY);
        if (muleCorrelationId != null)
        {
            if (xCorrelationId != null)
            {
                LOGGER.warn("'X-Correlation-ID: {}' and 'MULE_CORRELATION_ID: {}' headers found. 'MULE_CORRELATION_ID' will be used.",
                            xCorrelationId, muleCorrelationId);
            }
            defaultMuleMessage.setCorrelationId(muleCorrelationId);
        }
        else if (xCorrelationId != null)
        {
            defaultMuleMessage.setCorrelationId(xCorrelationId);
        }
    }

    private static URI resolveUri(final HttpRequestContext requestContext)
    {
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setProtocol(requestContext.getScheme());
        uriBuilder.setHost(resolveTargetHost(requestContext.getRequest()));
        uriBuilder.setPath(requestContext.getRequest().getPath());
        return uriBuilder.getEndpoint().getUri();
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
