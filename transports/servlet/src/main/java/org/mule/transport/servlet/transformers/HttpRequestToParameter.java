/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.servlet.transformers;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.servlet.AbstractReceiverServlet;
import org.mule.transport.servlet.ServletConnector;
import org.mule.util.SystemUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

public class HttpRequestToParameter extends AbstractMessageTransformer
{
    public HttpRequestToParameter()
    {
        registerSourceType(DataTypeFactory.OBJECT);
        setReturnDataType(DataTypeFactory.STRING);
    }

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
    {
        String payloadParam = message.getOutboundProperty(AbstractReceiverServlet.PAYLOAD_PARAMETER_NAME,
                                                          AbstractReceiverServlet.DEFAULT_PAYLOAD_PARAMETER_NAME);

        String payload = message.getInboundProperty(payloadParam);
        if (payload == null)
        {
            // Plain text
            String contentType =
                    message.getOutboundProperty(ServletConnector.CONTENT_TYPE_PROPERTY_KEY);
            if ((contentType == null) || contentType.startsWith("text/"))
            {
                try
                {
                    InputStream is = (InputStream) message.getPayload();

                    String characterEncoding =
                            message.getOutboundProperty(ServletConnector.CHARACTER_ENCODING_PROPERTY_KEY);
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
