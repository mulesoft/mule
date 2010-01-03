/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.transformers;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageAwareTransformer;
import org.mule.transport.http.HttpConstants;

import java.net.URI;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class HttpRequestBodyToParamMap extends AbstractMessageAwareTransformer
{
    
    public HttpRequestBodyToParamMap()
    {
        registerSourceType(Object.class);
        setReturnClass(Object.class);
    }

    @Override
    public Object transform(MuleMessage message, String encoding) throws TransformerException
    {
        Map<String, Object> paramMap = new HashMap<String, Object>();

        try
        {
            String httpMethod = (String) message.getProperty("http.method");
            String contentType = (String) message.getProperty("Content-Type");
            
            boolean isGet = HttpConstants.METHOD_GET.equalsIgnoreCase(httpMethod);
            boolean isPost = HttpConstants.METHOD_POST.equalsIgnoreCase(httpMethod);
            boolean isUrlEncoded = contentType.startsWith("application/x-www-form-urlencoded");

            if (!(isGet || (isPost && isUrlEncoded)))
            {
                throw new Exception("The HTTP method or content type is unsupported!");
            }

            String queryString = null;
            if (isGet)
            {
                URI uri = new URI(message.getPayloadAsString(encoding));
                queryString = uri.getQuery();
            }
            else if (isPost)
            {
                queryString = new String(message.getPayloadAsBytes());
            }

            if (queryString != null && queryString.length() > 0)
            {
                String[] pairs = queryString.split("&");
                for (int x = 0; x < pairs.length; x++)
                {
                    String[] nameValue = pairs[x].split("=");
                    if (nameValue.length == 2)
                    {
                        paramMap.put(URLDecoder.decode(nameValue[0], encoding), URLDecoder.decode(
                            nameValue[1], encoding));
                    }
                }
            }
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }

        return paramMap;

    }

    @Override
    public boolean isAcceptNull()
    {
        return false;
    }

}
