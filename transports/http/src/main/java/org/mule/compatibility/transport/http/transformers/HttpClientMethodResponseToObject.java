/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.transformers;

import org.mule.compatibility.transport.http.HttpConstants;
import org.mule.compatibility.transport.http.ReleasingInputStream;
import org.mule.runtime.api.message.NullPayload;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.transformer.AbstractTransformer;
import org.mule.runtime.core.transformer.types.DataTypeFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;

/**
 * <code>HttpClientMethodResponseToObject</code> transforms a http client response
 * to a DefaultMuleMessage.
 */

public class HttpClientMethodResponseToObject extends AbstractTransformer
{

    public HttpClientMethodResponseToObject()
    {
        registerSourceType(DataTypeFactory.create(HttpMethod.class));
        setReturnDataType(DataTypeFactory.MULE_MESSAGE);
    }

    @Override
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        Object msg;
        HttpMethod httpMethod = (HttpMethod)src;
        
        InputStream is;
        try
        {
            is = httpMethod.getResponseBodyAsStream();
        }
        catch (IOException e)
        {
            throw new TransformerException(this, e);
        }
        
        if (is == null)
        {
            msg = NullPayload.getInstance();
        }
        else
        {
            msg = new ReleasingInputStream(is, httpMethod);
        }
        
        // Standard headers
        Map headerProps = new HashMap();
        Header[] headers = httpMethod.getResponseHeaders();
        String name;
        for (int i = 0; i < headers.length; i++)
        {
            name = headers[i].getName();
            if (name.startsWith(HttpConstants.X_PROPERTY_PREFIX))
            {
                name = name.substring(2);
            }
            headerProps.put(name, headers[i].getValue());
        }
        // Set Mule Properties

        return new DefaultMuleMessage(msg, headerProps, muleContext);
    }
}
