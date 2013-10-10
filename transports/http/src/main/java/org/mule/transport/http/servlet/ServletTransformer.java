/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http.servlet;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transformer.TransformerMessagingException;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transport.http.HttpConnector;

/**
 * THIS CLASS IS UNSUPPORTED AND THE IMPLEMENTATION DOES NOT CONFORM TO THE SERVLET SPECIFICATION!
 * 
 * With that said, it can be used to make integration with libraries that only support servlets 
 * easier. However, it is not guaranteed to work.
 */
public class ServletTransformer extends AbstractMessageTransformer
{

    @Override
    public Object transform(Object src, MuleEvent event) throws TransformerMessagingException
    {
        MuleMessage message = (MuleMessage) src;
        
        message.setInvocationProperty(HttpConnector.HTTP_SERVLET_REQUEST_PROPERTY, new MuleHttpServletRequest(event));
        message.setInvocationProperty(HttpConnector.HTTP_SERVLET_RESPONSE_PROPERTY, new MuleHttpServletResponse(event));
        
        return message;
    }

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
    {
        throw new IllegalStateException();
    }
}