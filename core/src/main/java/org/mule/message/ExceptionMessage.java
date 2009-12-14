/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.message;

import org.mule.RequestContext;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.EndpointURI;

import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;

/**
 * <code>ExceptionMessage</code> is used by the DefaultServiceExceptionStrategy
 * for wrapping an exception with a message to send via an endpointUri.
 */
public class ExceptionMessage extends BaseMessageDTO
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -538516243574950621L;

    private Throwable exception;
    private String componentName;
    private String endpointUri;
    private Date timeStamp;

    public ExceptionMessage(Serializable message,
                            Throwable exception,
                            String componentName,
                            EndpointURI endpointUri)
    {
        super(message);
        this.exception = exception;
        timeStamp = new Date();
        this.componentName = componentName;
        this.endpointUri = endpointUri.toString();

        MuleEventContext ctx = RequestContext.getEventContext();
        if (ctx != null)
        {
            MuleMessage msg = ctx.getMessage();
            for (Iterator iterator = msg.getPropertyNames().iterator(); iterator.hasNext();)
            {
                String propertyKey = (String) iterator.next();
                setProperty(propertyKey, msg.getProperty(propertyKey));
            }
        }
    }
    
    /**
     * @deprecated Use ExceptionMessage(Serializable message...) instead
     */
    public ExceptionMessage(Object message,
                            Throwable exception,
                            String componentName,
                            EndpointURI endpointUri)
    {
        super(getAsSerializable(message));
        this.exception = exception;
        timeStamp = new Date();
        this.componentName = componentName;
        this.endpointUri = endpointUri.toString();

        MuleEventContext ctx = RequestContext.getEventContext();
        if (ctx != null)
        {
            MuleMessage msg = ctx.getMessage();
            for (Iterator iterator = msg.getPropertyNames().iterator(); iterator.hasNext();)
            {
                String propertyKey = (String) iterator.next();
                setProperty(propertyKey, msg.getProperty(propertyKey));
            }
        }
    }

    protected static Serializable getAsSerializable(Object message)
    {
        if(message instanceof Serializable)
        {
            return (Serializable)message;
        }
        else
        {
            return message.toString();
        }
    }

    public String getComponentName()
    {
        return componentName;
    }

    public String getEndpoint()
    {
        return endpointUri;
    }

    public Date getTimeStamp()
    {
        return timeStamp;
    }

    public Throwable getException()
    {
        return exception;
    }

    public String toString()
    {
        return "ExceptionMessage{" + "payload=" + payload + ", context=" + properties + "exception=" + exception
               + ", componentName='" + componentName + "'" + ", endpointUri=" + endpointUri + ", timeStamp="
               + timeStamp + "}";
    }
}
