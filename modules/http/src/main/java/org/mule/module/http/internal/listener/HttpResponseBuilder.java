/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener;

import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.module.http.api.HttpHeaders;
import org.mule.module.http.internal.HttpMessageBuilder;
import org.mule.module.http.internal.HttpParser;
import org.mule.module.http.api.requester.HttpStreamingType;
import org.mule.module.http.internal.ParameterMap;
import org.mule.module.http.internal.domain.ByteArrayHttpEntity;
import org.mule.module.http.internal.domain.EmptyHttpEntity;
import org.mule.module.http.internal.domain.HttpEntity;
import org.mule.module.http.internal.domain.InputStreamHttpEntity;
import org.mule.module.http.internal.domain.MultipartHttpEntity;
import org.mule.module.http.internal.domain.response.HttpResponse;
import org.mule.module.http.internal.multipart.HttpMultipartEncoder;
import org.mule.module.http.internal.multipart.HttpPartDataSource;
import org.mule.module.http.internal.HttpParamType;
import org.mule.transport.NullPayload;
import org.mule.util.AttributeEvaluator;
import org.mule.util.IOUtils;
import org.mule.util.UUID;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.activation.DataHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpResponseBuilder extends HttpMessageBuilder implements Initialisable, MuleContextAware
{

    public static final String MULTIPART = "multipart";
    private Logger logger = LoggerFactory.getLogger(getClass());
    private String statusCode = "200";
    private String reasonPhrase;
    private boolean disablePropertiesAsHeaders = false;
    private HttpStreamingType responseStreaming = HttpStreamingType.AUTO;
    private boolean multipartEntityWithNoMultipartContentyTypeWarned;
    private boolean mapPayloadButNoUrlEncodedContentyTypeWarned;
    private AttributeEvaluator statusCodeEvaluator;
    private AttributeEvaluator reasonPhraseEvaluator;
    private MuleContext muleContext;

    public void initialise() throws InitialisationException
    {
        init();
    }

    void init()
    {
        statusCodeEvaluator = new AttributeEvaluator(statusCode).initialize(muleContext.getExpressionManager());
        reasonPhraseEvaluator = new AttributeEvaluator(reasonPhrase).initialize(muleContext.getExpressionManager());
    }

    public HttpResponse build(org.mule.module.http.internal.domain.response.HttpResponseBuilder httpResponseBuilder, MuleEvent event) throws MessagingException
    {
        final HttpResponseHeaderBuilder httpResponseHeaderBuilder = new HttpResponseHeaderBuilder();
        if (!disablePropertiesAsHeaders)
        {
            final Set<String> outboundProperties = event.getMessage().getOutboundPropertyNames();
            for (String outboundPropertyName : outboundProperties)
            {
                final Object outboundPropertyValue = event.getMessage().getOutboundProperty(outboundPropertyName);
                httpResponseHeaderBuilder.addHeader(outboundPropertyName, outboundPropertyValue);
            }
        }

        ParameterMap resolvedHeaders = resolveParams(event, HttpParamType.HEADER);
        for (String name : resolvedHeaders.keySet())
        {
            final Collection<String> paramValues = resolvedHeaders.getAsList(name);
            for (String value : paramValues)
            {
                httpResponseHeaderBuilder.addHeader(name, value);
            }
        }

        final String configuredContentType = httpResponseHeaderBuilder.getContentType();
        final String configuredTransferEncoding = httpResponseHeaderBuilder.getTransferEncoding();

        HttpEntity httpEntity;

        if (!event.getMessage().getOutboundAttachmentNames().isEmpty())
        {
            if (configuredContentType == null)
            {
                httpResponseHeaderBuilder.addContentType(createMultipartFormDataContentType());
            }
            else if (!configuredContentType.startsWith(MULTIPART))
            {
                warnNoMultipartContentTypeButMultipartEntity(httpResponseHeaderBuilder.getContentType());
            }
            httpEntity = createMultipartEntity(event, httpResponseHeaderBuilder.getContentType());
        }
        else
        {
            final Object payload = event.getMessage().getPayload();
            if (payload == NullPayload.getInstance())
            {
                httpEntity = new EmptyHttpEntity();
            }
            else if (payload instanceof Map)
            {
                if (configuredContentType == null)
                {
                    httpResponseHeaderBuilder.addContentType(HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED);
                }
                else if (!configuredContentType.startsWith(HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED))
                {
                    warnMapPayloadButNoUrlEncodedContentType(httpResponseHeaderBuilder.getContentType());
                }
                httpEntity = createUrlEncodedEntity(event, (Map<String, Object>) payload);
            }
            else if (payload instanceof InputStream)
            {
                if (responseStreaming == HttpStreamingType.AUTO)
                {
                    if (configuredTransferEncoding == null)
                    {
                        httpResponseHeaderBuilder.addHeader(HttpHeaders.Names.TRANSFER_ENCODING, HttpHeaders.Values.CHUNKED);
                    }
                    httpResponseHeaderBuilder.removeHeader(HttpHeaders.Names.CONTENT_LENGTH);
                }
                httpEntity = new InputStreamHttpEntity((InputStream) payload);
            }
            else
            {
                try
                {
                    httpEntity = new ByteArrayHttpEntity(event.getMessage().getPayloadAsBytes());
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        }

        if (responseStreaming.equals(HttpStreamingType.ALWAYS))
        {
            if (httpResponseHeaderBuilder.getTransferEncoding() == null)
            {
                httpResponseHeaderBuilder.addHeader(HttpHeaders.Names.TRANSFER_ENCODING, HttpHeaders.Values.CHUNKED);
            }
            httpResponseHeaderBuilder.removeHeader(HttpHeaders.Names.CONTENT_LENGTH);
        }

        if (httpResponseHeaderBuilder.getTransferEncoding() == null && httpResponseHeaderBuilder.getContentLength() == null)
        {
            int calculatedContentLenght = 0;
            if (httpEntity instanceof ByteArrayHttpEntity)
            {
                calculatedContentLenght = ((ByteArrayHttpEntity) httpEntity).getContent().length;
            }
            else if (httpEntity instanceof InputStreamHttpEntity)
            {
                httpEntity = new ByteArrayHttpEntity(IOUtils.toByteArray(((InputStreamHttpEntity) httpEntity).getInputStream()));
                calculatedContentLenght = ((ByteArrayHttpEntity)httpEntity).getContent().length;
            }
            httpResponseHeaderBuilder.addContentLenght(String.valueOf(calculatedContentLenght));
        }

        Collection<String> headerNames = httpResponseHeaderBuilder.getHeaderNames();
        for (String headerName : headerNames)
        {
            Collection<String> values = httpResponseHeaderBuilder.getHeader(headerName);
            for (String value : values)
            {
                httpResponseBuilder.addHeader(headerName, value);
            }
        }

        httpResponseBuilder.setStatusCode(statusCodeEvaluator.resolveIntegerValue(event));
        if (this.reasonPhrase != null)
        {
            httpResponseBuilder.setReasonPhrase(reasonPhraseEvaluator.resolveStringValue(event));
        }
        httpResponseBuilder.setEntity(httpEntity);
        return httpResponseBuilder.build();
    }

    private String createMultipartFormDataContentType()
    {
        return String.format("%s; boundary=%s", HttpHeaders.Values.MULTIPART_FORM_DATA, UUID.getUUID());
    }

    private HttpEntity createUrlEncodedEntity(MuleEvent event, Map<String, Object> payload)
    {
        final Map<String, Object> mapPayload = payload;
        HttpEntity entity = new EmptyHttpEntity();
        if (!mapPayload.isEmpty())
        {
            String encodedBody;
            if (mapPayload instanceof ParameterMap)
            {
                encodedBody = HttpParser.encodeString(event.getEncoding(), ((ParameterMap) mapPayload).toCollectionMap());
            }
            else
            {
                encodedBody = HttpParser.encodeString(event.getEncoding(), mapPayload);
            }
            entity = new ByteArrayHttpEntity(encodedBody.getBytes());
        }
        return entity;
    }

    private void warnMapPayloadButNoUrlEncodedContentType(String contentType)
    {
        if (!mapPayloadButNoUrlEncodedContentyTypeWarned)
        {
            logger.warn(String.format("Payload is a Map which will be used to generate an url encoded http body but Contenty-Type specified is %s and not %s", contentType, HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED));
            mapPayloadButNoUrlEncodedContentyTypeWarned = true;
        }
    }

    private void warnNoMultipartContentTypeButMultipartEntity(String contentType)
    {
        if (!multipartEntityWithNoMultipartContentyTypeWarned)
        {
            logger.warn(String.format("Sending http response with Content-Type %s but the message has attachment and a multipart entity is generated", contentType));
            multipartEntityWithNoMultipartContentyTypeWarned = true;
        }
    }

    private HttpEntity createMultipartEntity(MuleEvent event, String contentType) throws MessagingException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Message contains outbound attachments. Ignoring payload and trying to generate multipart response");
        }
        final HashMap<String, DataHandler> parts = new HashMap<>();
        for (String outboundAttachmentName : event.getMessage().getOutboundAttachmentNames())
        {
            parts.put(outboundAttachmentName, event.getMessage().getOutboundAttachment(outboundAttachmentName));
        }
        final MultipartHttpEntity multipartEntity;
        try
        {
            multipartEntity = new MultipartHttpEntity(HttpPartDataSource.createFrom(parts));
            return new ByteArrayHttpEntity(HttpMultipartEncoder.createMultipartContent(multipartEntity, contentType));
        }
        catch (Exception e)
        {
            throw new MessagingException(event, e);
        }
    }

    public static HttpResponseBuilder emptyInstance(MuleContext muleContext)
    {
        final HttpResponseBuilder httpResponseBuilder = new HttpResponseBuilder();
        httpResponseBuilder.setMuleContext(muleContext);
        httpResponseBuilder.init();
        return httpResponseBuilder;
    }

    public void setReasonPhrase(String reasonPhrase)
    {
        this.reasonPhrase = reasonPhrase;
    }

    public void setStatusCode(String statusCode)
    {
        this.statusCode = statusCode;
    }

    public void setDisablePropertiesAsHeaders(boolean disablePropertiesAsHeaders)
    {
        this.disablePropertiesAsHeaders = disablePropertiesAsHeaders;
    }


    public void setResponseStreaming(HttpStreamingType responseStreaming)
    {
        this.responseStreaming = responseStreaming;
    }

    public HttpStreamingType getResponseStreaming()
    {
        return responseStreaming;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }
}