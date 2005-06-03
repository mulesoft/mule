/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.impl.message;

import java.util.Date;

import org.mule.umo.UMOEventContext;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;

/**
 * <code>ExceptionMessage</code> is used by the
 * DefaultComponentExceptionStrategy for wrapping an exception with a message to
 * send via an endpointUri.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ExceptionMessage extends BaseMessage
{
    private Throwable exception;
    private String componentName;
    private UMOEndpointURI endpointUri;
    private Date timeStamp;

    public ExceptionMessage(Object message, UMOEndpoint endpoint, Throwable exception, UMOEventContext ctx)
    {
        super(message);
        this.exception = exception;
        timeStamp = new Date();
        componentName = ctx.getComponentDescriptor().getName();
        if (endpoint != null) {
            endpointUri = endpoint.getEndpointURI();
        } else {
            endpointUri = ctx.getEndpointURI();
        }
        addProperties(ctx.getProperties());
    }

    public ExceptionMessage(Object message, Throwable exception, UMOEventContext ctx)
    {
        super(message);
        this.exception = exception;
        timeStamp = new Date();
        componentName = ctx.getComponentDescriptor().getName();
        endpointUri = ctx.getEndpointURI();
        addProperties(ctx.getProperties());
    }

    public String getComponentName()
    {
        return componentName;
    }

    public UMOEndpointURI getEndpoint()
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
}
