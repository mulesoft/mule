/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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