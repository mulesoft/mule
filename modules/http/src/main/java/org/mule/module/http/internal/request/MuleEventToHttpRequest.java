/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request;

import static org.mule.module.http.api.HttpHeaders.Names.CONTENT_LENGTH;
import static org.mule.module.http.api.HttpHeaders.Names.TRANSFER_ENCODING;
import static org.mule.module.http.api.HttpHeaders.Values.CHUNKED;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.module.http.api.HttpHeaders;
import org.mule.module.http.api.requester.HttpStreamingType;
import org.mule.module.http.internal.HttpParser;
import org.mule.module.http.internal.domain.ByteArrayHttpEntity;
import org.mule.module.http.internal.domain.EmptyHttpEntity;
import org.mule.module.http.internal.domain.HttpEntity;
import org.mule.module.http.internal.domain.InputStreamHttpEntity;
import org.mule.module.http.internal.domain.MultipartHttpEntity;
import org.mule.module.http.internal.domain.request.HttpRequestBuilder;
import org.mule.module.http.internal.multipart.HttpPartDataSource;
import org.mule.transport.NullPayload;
import org.mule.util.AttributeEvaluator;
import org.mule.util.StringUtils;

import com.google.common.collect.Maps;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.activation.DataHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MuleEventToHttpRequest
{
    private static final Logger logger = LoggerFactory.getLogger(MuleEventToHttpRequest.class);

    private DefaultHttpRequester requester;
    private MuleContext muleContext;

    private AttributeEvaluator requestStreamingMode;
    private AttributeEvaluator sendBody;


    public MuleEventToHttpRequest(DefaultHttpRequester requester, MuleContext muleContext, AttributeEvaluator requestStreamingMode, AttributeEvaluator sendBody)
    {
        this.requester = requester;
        this.muleContext = muleContext;
        this.requestStreamingMode = requestStreamingMode;
        this.sendBody = sendBody;
    }

    public HttpRequestBuilder create(MuleEvent event, String resolvedMethod, String resolvedUri) throws MessagingException
    {
        HttpRequesterRequestBuilder requestBuilder = requester.getRequestBuilder();
        HttpRequestBuilder builder = new HttpRequestBuilder();

        builder.setUri(resolvedUri);
        builder.setMethod(resolvedMethod);
        builder.setHeaders(requestBuilder.getHeaders(event));
        builder.setQueryParams(requestBuilder.getQueryParams(event));

        for (String outboundProperty : event.getMessage().getOutboundPropertyNames())
        {
            builder.addHeader(outboundProperty, event.getMessage().getOutboundProperty(outboundProperty).toString());
        }

        builder.setEntity(createRequestEntity(builder, event, resolvedMethod));

        return builder;
    }


    private HttpEntity createRequestEntity(HttpRequestBuilder requestBuilder, MuleEvent muleEvent, String resolvedMethod) throws MessagingException
    {
        if (isEmptyBody(muleEvent, resolvedMethod))
        {
            return new EmptyHttpEntity();
        }
        else
        {
            boolean customSource = false;
            Object oldPayload = null;

            if (!StringUtils.isEmpty(requester.getSource()) && !(DefaultHttpRequester.DEFAULT_PAYLOAD_EXPRESSION.equals(requester.getSource())))
            {
                Object newPayload = muleContext.getExpressionManager().evaluate(requester.getSource(), muleEvent);
                oldPayload = muleEvent.getMessage().getPayload();
                muleEvent.getMessage().setPayload(newPayload);
                customSource = true;
            }

            HttpEntity entity = createRequestEntityFromPayload(requestBuilder, muleEvent);

            if (customSource)
            {
                muleEvent.getMessage().setPayload(oldPayload);
            }

            return entity;
        }
    }

    private boolean isEmptyBody(MuleEvent event, String method)
    {
        HttpSendBodyMode sendBodyMode = resolveSendBodyMode(event);

        boolean emptyBody;

        if (event.getMessage().getPayload() instanceof NullPayload && event.getMessage().getOutboundAttachmentNames().isEmpty())
        {
            emptyBody = true;
        }
        else
        {
            emptyBody = DefaultHttpRequester.DEFAULT_EMPTY_BODY_METHODS.contains(method);

            if (sendBodyMode != HttpSendBodyMode.AUTO)
            {
                emptyBody = (sendBodyMode == HttpSendBodyMode.NEVER);
            }
        }

        return emptyBody;
    }

    private HttpEntity createRequestEntityFromPayload(HttpRequestBuilder requestBuilder, MuleEvent muleEvent) throws MessagingException
    {
        Object payload = muleEvent.getMessage().getPayload();

        if (!muleEvent.getMessage().getOutboundAttachmentNames().isEmpty())
        {
            try
            {
                return createMultiPart(muleEvent.getMessage());
            }
            catch (IOException e)
            {
                throw new MessagingException(muleEvent, e);
            }
        }

        if (doStreaming(requestBuilder, muleEvent))
        {

            if (payload instanceof InputStream)
            {
                return new InputStreamHttpEntity((InputStream) payload);
            }
            else
            {
                try
                {
                    return new InputStreamHttpEntity(new ByteArrayInputStream(muleEvent.getMessage().getPayloadAsBytes()));
                }
                catch (Exception e)
                {
                    throw new MessagingException(muleEvent, e);
                }
            }

        }
        else
        {
            String contentType = requestBuilder.getHeaders().get(HttpHeaders.Names.CONTENT_TYPE);

            if (contentType == null || contentType.equals(HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED))
            {
                if (muleEvent.getMessage().getPayload() instanceof Map)
                {
                    String body = HttpParser.encodeString(muleEvent.getEncoding(), (Map) payload);
                    requestBuilder.addHeader(HttpHeaders.Names.CONTENT_TYPE, HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED);
                    return new ByteArrayHttpEntity(body.getBytes());
                }
            }

            try
            {
                return new ByteArrayHttpEntity(muleEvent.getMessage().getPayloadAsBytes());
            }
            catch (Exception e)
            {
                throw new MessagingException(muleEvent, e);
            }
        }
    }

    protected MultipartHttpEntity createMultiPart(final MuleMessage msg) throws IOException
    {
        Map<String, DataHandler> attachments = Maps.newHashMap();

        for (String outboundAttachmentName : msg.getOutboundAttachmentNames())
        {
            attachments.put(outboundAttachmentName, msg.getOutboundAttachment(outboundAttachmentName));
        }

        return new MultipartHttpEntity(HttpPartDataSource.createFrom(attachments));
    }


    private boolean doStreaming(HttpRequestBuilder requestBuilder, MuleEvent event) throws MessagingException
    {
        String transferEncodingHeader = requestBuilder.getHeaders().get(TRANSFER_ENCODING);
        String contentLengthHeader = requestBuilder.getHeaders().get(CONTENT_LENGTH);

        HttpStreamingType requestStreamingMode = resolveStreamingType(event);

        Object payload = event.getMessage().getPayload();

        if (requestStreamingMode == HttpStreamingType.AUTO)
        {
            if (contentLengthHeader != null)
            {
                if (transferEncodingHeader != null)
                {
                    requestBuilder.removeHeader(TRANSFER_ENCODING);

                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Cannot send both Transfer-Encoding and Content-Length headers. Transfer-Encoding will not be sent.");
                    }
                }
                return false;
            }

            if (transferEncodingHeader == null || !transferEncodingHeader.equalsIgnoreCase(CHUNKED))
            {
                return payload instanceof InputStream;
            }
            else
            {
                return true;
            }
        }
        else if (requestStreamingMode == HttpStreamingType.ALWAYS)
        {
            if (contentLengthHeader != null)
            {
                requestBuilder.removeHeader(CONTENT_LENGTH);

                if (logger.isDebugEnabled())
                {
                    logger.debug("Content-Length header will not be sent, as the configured requestStreamingMode is ALWAYS");
                }
            }

            if (transferEncodingHeader != null && !transferEncodingHeader.equalsIgnoreCase(CHUNKED))
            {
                requestBuilder.removeHeader(TRANSFER_ENCODING);

                if (logger.isDebugEnabled())
                {
                    logger.debug("Transfer-Encoding header will be sent with value 'chunked' instead of {}, as the configured " +
                                 "requestStreamingMode is NEVER", transferEncodingHeader);
                }

            }
            return true;
        }
        else
        {
            if (transferEncodingHeader != null)
            {
                requestBuilder.removeHeader(TRANSFER_ENCODING);

                if (logger.isDebugEnabled())
                {
                    logger.debug("Transfer-Encoding header will not be sent, as the configured requestStreamingMode is NEVER");
                }
            }
            return false;
        }
    }

    private HttpStreamingType resolveStreamingType(MuleEvent event)
    {
        return HttpStreamingType.valueOf(requestStreamingMode.resolveStringValue(event));
    }

    private HttpSendBodyMode resolveSendBodyMode(MuleEvent event)
    {
        return HttpSendBodyMode.valueOf(sendBody.resolveStringValue(event));
    }
}
