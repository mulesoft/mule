/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.transformers;

import static org.mule.transport.http.HttpConstants.FORM_URLENCODED_CONTENT_TYPE;
import static org.mule.transport.http.HttpConstants.HEADER_CONTENT_TYPE;
import static org.mule.transport.http.HttpConstants.METHOD_GET;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.StringUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.net.URLCodec;

public class HttpRequestBodyToParamMap extends AbstractMessageTransformer
{
    public HttpRequestBodyToParamMap()
    {
        registerSourceType(DataTypeFactory.OBJECT);
        setReturnDataType(DataTypeFactory.OBJECT);
    }

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
    {
        Map<String, Object> paramMap = new HashMap<String, Object>();

        try
        {
            String httpMethod = message.getInboundProperty("http.method");
            String contentType = message.getInboundProperty(HEADER_CONTENT_TYPE);

            boolean isGet = METHOD_GET.equalsIgnoreCase(httpMethod);
            boolean isFormUrlEncoded = false;
            if (contentType != null)
            {
                isFormUrlEncoded = contentType.startsWith(FORM_URLENCODED_CONTENT_TYPE);
            }

            if (!(isGet || isFormUrlEncoded))
            {
                throw new Exception("The HTTP method or content type is unsupported!");
            }

            String queryString;
            if (isGet)
            {
                URI uri = new URI(message.getPayloadAsString(outputEncoding));
                queryString = uri.getRawQuery();
            }
            else
            {
                queryString = new String(message.getPayloadAsBytes());
            }

            if (StringUtils.isNotBlank(queryString))
            {
                addQueryStringToParameterMap(queryString, paramMap, outputEncoding);
            }
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }

        return paramMap;
    }

    protected void addQueryStringToParameterMap(String queryString, Map<String, Object> paramMap,
        String outputEncoding) throws Exception
    {
        String[] pairs = queryString.split("&");
        for (String pair : pairs)
        {
            String[] nameValue = pair.split("=");
            if (nameValue.length == 2)
            {
                URLCodec codec = new URLCodec(outputEncoding);
                String key = codec.decode(nameValue[0]);
                String value = codec.decode(nameValue[1]);
                addToParameterMap(paramMap, key, value);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void addToParameterMap(Map<String, Object> paramMap, String key, String value)
    {
        Object existingValue = paramMap.get(key);
        if (existingValue != null)
        {
            List<Object> values;
            if (existingValue instanceof List<?>)
            {
                values = (List<Object>) existingValue;
            }
            else
            {
                values = new ArrayList<Object>();
                values.add(existingValue);
            }

            values.add(value);
            paramMap.put(key, values);
        }
        else
        {
            paramMap.put(key, value);
        }
    }

    @Override
    public boolean isAcceptNull()
    {
        return false;
    }
}
