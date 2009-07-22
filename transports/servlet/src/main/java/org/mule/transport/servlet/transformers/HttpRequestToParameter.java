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
import org.mule.transport.servlet.HttpRequestMessageAdapter;
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

    public Object transform(MuleMessage message, String outputEncoding) throws TransformerException
    {
        HttpRequestMessageAdapter messageAdapter = (HttpRequestMessageAdapter) message.getAdapter();

        String payloadParam = messageAdapter.getStringProperty(
            AbstractReceiverServlet.PAYLOAD_PARAMETER_NAME, 
            AbstractReceiverServlet.DEFAULT_PAYLOAD_PARAMETER_NAME);
        
        String payload = messageAdapter.getStringProperty(payloadParam, null);
        if (payload == null)
        {
            // Plain text
            if (messageAdapter.getContentType() == null || messageAdapter.getContentType().startsWith("text/"))
            {
                try
                {
                    InputStream is = (InputStream) message.getPayload();
                    BufferedReader reader;
                    if (messageAdapter.getCharacterEncoding() != null)
                    {
                        reader = new BufferedReader(new InputStreamReader(is, messageAdapter.getCharacterEncoding()));
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
            else if (messageAdapter.getContentType().equals("application/x-www-form-urlencoded"))
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
