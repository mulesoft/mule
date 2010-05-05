/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.servlet.transformers;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageAwareTransformer;
import org.mule.transport.servlet.AbstractReceiverServlet;
import org.mule.transport.servlet.ServletConnector;
import org.mule.util.SystemUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

public class HttpRequestToParameter extends AbstractMessageAwareTransformer
{
    public HttpRequestToParameter()
    {
        registerSourceType(Object.class);
        setReturnClass(String.class);
    }

    @Override
    public Object transform(MuleMessage message, String outputEncoding) throws TransformerException
    {
        String payloadParam = message.getStringProperty(
            AbstractReceiverServlet.PAYLOAD_PARAMETER_NAME, 
            AbstractReceiverServlet.DEFAULT_PAYLOAD_PARAMETER_NAME);
        
        String payload = message.getStringProperty(payloadParam, null);
        if (payload == null)
        {
            // Plain text
            String contentType = 
                message.getStringProperty(ServletConnector.CONTENT_TYPE_PROPERTY_KEY, null);
            if ((contentType == null) || contentType.startsWith("text/"))
            {
                try
                {
                    InputStream is = (InputStream) message.getPayload();
                    
                    String characterEncoding = 
                        message.getStringProperty(ServletConnector.CHARACTER_ENCODING_PROPERTY_KEY, null);
                    BufferedReader reader;
                    if (characterEncoding != null)
                    {
                        reader = new BufferedReader(new InputStreamReader(is, characterEncoding));
                    }
                    else
                    {
                        reader = new BufferedReader(new InputStreamReader(is));
                    }
                    
                    StringBuffer buffer = new StringBuffer(8192);
                    String line = reader.readLine();
                    while (line != null)
                    {
                        buffer.append(line);
                        line = reader.readLine();
                        if (line != null) buffer.append(SystemUtils.LINE_SEPARATOR);
                    }
                    payload = buffer.toString();
                }
                catch (IOException e)
                {
                    throw new TransformerException(this, e);
                }
            }
            // HTTP Form
            else if (contentType.equals("application/x-www-form-urlencoded"))
            {
                InputStream is = (InputStream) message.getPayload();
                Properties props = new Properties();
                try
                {
                    props.load(is);
                }
                catch (IOException e)
                {
                    throw new TransformerException(this, e);
                }
                finally
                {
                    try
                    {
                        is.close();
                    }
                    catch (IOException e2)
                    {
                        throw new TransformerException(this, e2);
                    }
                }
                return props.get(payloadParam);
            }
        }

        return payload;
    }
}
