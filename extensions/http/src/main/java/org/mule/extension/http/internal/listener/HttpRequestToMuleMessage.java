/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.listener;

import static org.mule.runtime.core.util.SystemUtils.getDefaultEncoding;
import static org.mule.runtime.module.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.runtime.module.http.internal.HttpParser.decodeUrlEncodedBody;
import static org.mule.runtime.module.http.internal.multipart.HttpPartDataSource.createDataHandlerFrom;
import static org.mule.runtime.module.http.internal.util.HttpToMuleMessage.getMediaType;
import org.mule.extension.http.api.HttpRequestAttributes;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.api.message.NullPayload;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.module.http.api.HttpHeaders;
import org.mule.runtime.module.http.internal.domain.EmptyHttpEntity;
import org.mule.runtime.module.http.internal.domain.HttpEntity;
import org.mule.runtime.module.http.internal.domain.InputStreamHttpEntity;
import org.mule.runtime.module.http.internal.domain.MultipartHttpEntity;
import org.mule.runtime.module.http.internal.domain.request.HttpRequest;
import org.mule.runtime.module.http.internal.domain.request.HttpRequestContext;
import org.mule.runtime.module.http.internal.listener.HttpRequestParsingException;
import org.mule.runtime.module.http.internal.listener.ListenerPath;

import java.util.HashMap;
import java.util.Map;

import javax.activation.DataHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component that transforms an HTTP request to a proper {@link MuleMessage}.
 *
 * @since 4.0
 */
public class HttpRequestToMuleMessage
{
    private static Logger logger = LoggerFactory.getLogger(HttpRequestToMuleMessage.class);

    public static MuleMessage transform(final HttpRequestContext requestContext, final MuleContext muleContext, Boolean parseRequest, ListenerPath listenerPath) throws HttpRequestParsingException
    {
        final HttpRequest request = requestContext.getRequest();

        final MediaType mediaType = getMediaType(request.getHeaderValueIgnoreCase(CONTENT_TYPE), getDefaultEncoding(muleContext));

        final Map<String, DataHandler> parts = new HashMap<>();
        Object payload = NullPayload.getInstance();
        if (parseRequest)
        {
            final HttpEntity entity = request.getEntity();
            if (entity != null && !(entity instanceof EmptyHttpEntity))
            {
                if (entity instanceof MultipartHttpEntity)
                {
                    parts.putAll(createDataHandlerFrom(((MultipartHttpEntity) entity).getParts()));
                }
                else
                {
                    if (mediaType != null)
                    {
                        if (mediaType.matches(HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED))
                        {
                            try
                            {
                                payload = decodeUrlEncodedBody(IOUtils.toString(((InputStreamHttpEntity) entity).getInputStream()), mediaType.getCharset().get());
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

        HttpRequestAttributes attributes = new HttpRequestAttributesBuilder().setRequestContext(requestContext)
                .setListenerPath(listenerPath).setParts(parts).build();
        return MuleMessage.builder().payload(payload).mediaType(mediaType).attributes(attributes).build();
    }

}
