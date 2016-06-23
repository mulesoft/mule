/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.listener;

import static org.mule.runtime.module.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.runtime.module.http.internal.HttpParser.decodeUrlEncodedBody;
import static org.mule.runtime.module.http.internal.multipart.HttpPartDataSource.createDataHandlerFrom;
import static org.mule.runtime.module.http.internal.util.HttpToMuleMessage.buildContentTypeDataType;

import org.mule.extension.http.api.HttpRequestAttributes;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.api.message.NullPayload;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeBuilder;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.module.http.api.HttpHeaders;
import org.mule.runtime.module.http.internal.ParameterMap;
import org.mule.runtime.module.http.internal.domain.EmptyHttpEntity;
import org.mule.runtime.module.http.internal.domain.HttpEntity;
import org.mule.runtime.module.http.internal.domain.InputStreamHttpEntity;
import org.mule.runtime.module.http.internal.domain.MultipartHttpEntity;
import org.mule.runtime.module.http.internal.domain.request.HttpRequest;
import org.mule.runtime.module.http.internal.domain.request.HttpRequestContext;
import org.mule.runtime.module.http.internal.listener.HttpRequestParsingException;
import org.mule.runtime.module.http.internal.listener.ListenerPath;

import java.io.InputStream;
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

        final DataType dataType = buildContentTypeDataType(request.getHeaderValueIgnoreCase(CONTENT_TYPE));

        final Map<String, DataHandler> parts = new HashMap<>();
        Object payload = NullPayload.getInstance();
        DataTypeBuilder dataTypeBuilder = DataType.builder(dataType);
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
                    if (dataType.getMimeType() != null)
                    {
                        if (dataType.getMimeType().equals(HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED))
                        {
                            try
                            {
                                payload = decodeUrlEncodedBody(IOUtils.toString(((InputStreamHttpEntity) entity).getInputStream()), dataType.getEncoding());
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

        HttpRequestAttributes attributes = new HttpRequestAttributesBuilder().setRequestContext(requestContext)
                .setListenerPath(listenerPath).setParts(parts).build();
        return new DefaultMuleMessage(payload, dataTypeBuilder.build(), attributes, muleContext);
    }

}
