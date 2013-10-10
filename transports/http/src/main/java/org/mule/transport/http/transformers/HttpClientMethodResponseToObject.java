/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http.transformers;

import org.mule.DefaultMuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.NullPayload;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.ReleasingInputStream;

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
