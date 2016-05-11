/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.listener.builder;

import static org.mule.extension.http.api.HttpStreamingType.ALWAYS;
import static org.mule.extension.http.api.HttpStreamingType.AUTO;
import static org.mule.runtime.core.api.config.MuleProperties.CONTENT_TYPE_PROPERTY;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.CONTENT_LENGTH;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.TRANSFER_ENCODING;
import static org.mule.runtime.module.http.api.HttpHeaders.Values.CHUNKED;
import org.mule.extension.http.api.HttpMessageBuilder;
import org.mule.extension.http.api.HttpStreamingType;
import org.mule.runtime.api.message.NullPayload;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.config.i18n.MessageFactory;
import org.mule.runtime.core.transformer.types.MimeTypes;
import org.mule.runtime.core.util.DataTypeUtils;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.core.util.UUID;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.module.http.api.HttpHeaders;
import org.mule.runtime.module.http.internal.HttpParser;
import org.mule.runtime.module.http.internal.ParameterMap;
import org.mule.runtime.module.http.internal.domain.ByteArrayHttpEntity;
import org.mule.runtime.module.http.internal.domain.EmptyHttpEntity;
import org.mule.runtime.module.http.internal.domain.HttpEntity;
import org.mule.runtime.module.http.internal.domain.InputStreamHttpEntity;
import org.mule.runtime.module.http.internal.domain.MultipartHttpEntity;
import org.mule.runtime.module.http.internal.domain.response.HttpResponse;
import org.mule.runtime.module.http.internal.listener.HttpResponseHeaderBuilder;
import org.mule.runtime.module.http.internal.multipart.HttpMultipartEncoder;
import org.mule.runtime.module.http.internal.multipart.HttpPartDataSource;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component that specifies how to create a proper HTTP response.
 *
 * @since 4.0
 */
public class HttpResponseBuilder extends HttpMessageBuilder
{
    public static final String MULTIPART = "multipart";
    private Logger logger = LoggerFactory.getLogger(getClass());

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

    private HttpStreamingType responseStreaming = AUTO;
    private boolean multipartEntityWithNoMultipartContentyTypeWarned;
    private boolean mapPayloadButNoUrlEncodedContentyTypeWarned;

    //TODO: This logic should be in a MuleEventToHttpResponse component instead, just like for HTTP requests
    public HttpResponse build(org.mule.runtime.module.http.internal.domain.response.HttpResponseBuilder httpResponseBuilder, MuleEvent event, boolean supportsTransferEncoding) throws MessagingException
    {
        final HttpResponseHeaderBuilder httpResponseHeaderBuilder = new HttpResponseHeaderBuilder();

        if (!headers.containsKey(CONTENT_TYPE_PROPERTY))
        {
            DataType<?> dataType = event.getMessage().getDataType();
            if (!MimeTypes.ANY.equals(dataType.getMimeType()))
            {
                httpResponseHeaderBuilder.addHeader(CONTENT_TYPE_PROPERTY, DataTypeUtils.getContentType(dataType));
            }
        }

        for (String name : headers.keySet())
        {
            //For now, only support single headers
            if (TRANSFER_ENCODING.equals(name) && !supportsTransferEncoding)
            {
                logger.debug("Client HTTP version is lower than 1.1 so the unsupported 'Transfer-Encoding' header has been removed and 'Content-Length' will be sent instead.");
            }
            else
            {
                httpResponseHeaderBuilder.addHeader(name, headers.get(name));
            }
        }

        final String configuredContentType = httpResponseHeaderBuilder.getContentType();
        final String existingTransferEncoding = httpResponseHeaderBuilder.getTransferEncoding();
        final String existingContentLength = httpResponseHeaderBuilder.getContentLength();

        HttpEntity httpEntity;

        if (!parts.isEmpty())
        {
            if (configuredContentType == null)
            {
                httpResponseHeaderBuilder.addContentType(createMultipartFormDataContentType());
            }
            else if (!configuredContentType.startsWith(MULTIPART))
            {
                warnNoMultipartContentTypeButMultipartEntity(httpResponseHeaderBuilder.getContentType());
            }
            httpEntity = createMultipartEntity(event.getMessage(), httpResponseHeaderBuilder.getContentType());
            resolveEncoding(httpResponseHeaderBuilder, existingTransferEncoding, existingContentLength, supportsTransferEncoding, (ByteArrayHttpEntity) httpEntity);
        }
        else
        {
            final Object payload = event.getMessage().getPayload();
            if (payload == NullPayload.getInstance())
            {
                setupContentLengthEncoding(httpResponseHeaderBuilder, 0);
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
                httpEntity = createUrlEncodedEntity(event, (Map) payload);
            }
            else if (payload instanceof InputStream)
            {
                if (responseStreaming == ALWAYS || (responseStreaming == AUTO && existingContentLength == null))
                {
                    if (supportsTransferEncoding)
                    {
                        setupChunkedEncoding(httpResponseHeaderBuilder);
                    }
                    httpEntity = new InputStreamHttpEntity((InputStream) payload);
                }
                else
                {
                    ByteArrayHttpEntity byteArrayHttpEntity = new ByteArrayHttpEntity(IOUtils.toByteArray(((InputStream) payload)));
                    setupContentLengthEncoding(httpResponseHeaderBuilder, byteArrayHttpEntity.getContent().length);
                    httpEntity = byteArrayHttpEntity;
                }
            }
            else
            {
                try
                {
                    ByteArrayHttpEntity byteArrayHttpEntity = new ByteArrayHttpEntity(event.getMessageAsBytes());
                    resolveEncoding(httpResponseHeaderBuilder, existingTransferEncoding, existingContentLength, supportsTransferEncoding, byteArrayHttpEntity);
                    httpEntity = byteArrayHttpEntity;
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
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

        if (statusCode != null)
        {
            httpResponseBuilder.setStatusCode(statusCode.apply(event));
        }
        if (reasonPhrase != null)
        {
            httpResponseBuilder.setReasonPhrase(reasonPhrase.apply(event));
        }
        httpResponseBuilder.setEntity(httpEntity);
        return httpResponseBuilder.build();
    }

    private void resolveEncoding(HttpResponseHeaderBuilder httpResponseHeaderBuilder,
                                 String existingTransferEncoding,
                                 String existingContentLength,
                                 boolean supportsTranferEncoding,
                                 ByteArrayHttpEntity byteArrayHttpEntity)
    {
        if (responseStreaming == ALWAYS || (responseStreaming == AUTO && existingContentLength == null && CHUNKED.equals(existingTransferEncoding)))
        {
            if (supportsTranferEncoding)
            {
                setupChunkedEncoding(httpResponseHeaderBuilder);
            }
        }
        else
        {
            setupContentLengthEncoding(httpResponseHeaderBuilder, byteArrayHttpEntity.getContent().length);
        }
    }

    private void setupContentLengthEncoding(HttpResponseHeaderBuilder httpResponseHeaderBuilder, int contentLength)
    {
        if (httpResponseHeaderBuilder.getTransferEncoding() != null)
        {
            logger.debug("Content-Length encoding is being used so the 'Transfer-Encoding' header has been removed");
            httpResponseHeaderBuilder.removeHeader(TRANSFER_ENCODING);
        }
        httpResponseHeaderBuilder.addContentLenght(String.valueOf(contentLength));
    }

    private void setupChunkedEncoding(HttpResponseHeaderBuilder httpResponseHeaderBuilder)
    {
        if (httpResponseHeaderBuilder.getContentLength() != null)
        {
            logger.debug("Chunked encoding is being used so the 'Content-Length' header has been removed");
            httpResponseHeaderBuilder.removeHeader(CONTENT_LENGTH);
        }
        httpResponseHeaderBuilder.addHeader(TRANSFER_ENCODING, CHUNKED);
    }

    private String createMultipartFormDataContentType()
    {
        return String.format("%s; boundary=%s", HttpHeaders.Values.MULTIPART_FORM_DATA, UUID.getUUID());
    }

    private HttpEntity createUrlEncodedEntity(MuleEvent event, Map payload)
    {
        final Map mapPayload = payload;
        HttpEntity entity = new EmptyHttpEntity();
        if (!mapPayload.isEmpty())
        {
            String encodedBody;
            if (mapPayload instanceof ParameterMap)
            {
                encodedBody = HttpParser.encodeString(event.getEncoding(), ((ParameterMap) mapPayload).toListValuesMap());
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
            logger.warn(String.format("Payload is a Map which will be used to generate an url encoded http body but Contenty-Type specified is %s and not %s.", contentType, HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED));
            mapPayloadButNoUrlEncodedContentyTypeWarned = true;
        }
    }

    private void warnNoMultipartContentTypeButMultipartEntity(String contentType)
    {
        if (!multipartEntityWithNoMultipartContentyTypeWarned)
        {
            logger.warn(String.format("Sending http response with Content-Type %s but the message has attachment and a multipart entity is generated.", contentType));
            multipartEntityWithNoMultipartContentyTypeWarned = true;
        }
    }

    private HttpEntity createMultipartEntity(MuleMessage muleMessage, String contentType) throws MessagingException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Message contains attachments. Ignoring payload and trying to generate multipart response.");
        }

        final MultipartHttpEntity multipartEntity;
        try
        {
            multipartEntity = new MultipartHttpEntity(HttpPartDataSource.createFrom(getParts()));
            return new ByteArrayHttpEntity(HttpMultipartEncoder.createMultipartContent(multipartEntity, contentType));
        }
        catch (Exception e)
        {
            throw new MessagingException(MessageFactory.createStaticMessage("Error creating multipart HTTP entity."), muleMessage, e);
        }
    }

    //TODO:  No point for having this.
    public static HttpResponseBuilder emptyInstance() throws InitialisationException
    {
        return new HttpResponseBuilder();
    }

    public void setResponseStreaming(HttpStreamingType responseStreaming)
    {
        this.responseStreaming = responseStreaming;
    }
}