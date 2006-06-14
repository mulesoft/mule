/*
 * $Id$
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

import org.mule.impl.RequestContext;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpointURI;

import java.util.Date;
import java.util.Iterator;

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
    /**
     * Serial version
     */
    private static final long serialVersionUID = -538516243574950621L;

    private Throwable exception;
    private String componentName;
    private UMOEndpointURI endpointUri;
    private Date timeStamp;



    public ExceptionMessage(Object message, Throwable exception, String componentName, UMOEndpointURI endpointUri)
    {
        super(message);
        this.exception = exception;
        timeStamp = new Date();
        this.componentName = componentName;
        this.endpointUri = endpointUri;

        UMOEventContext ctx = RequestContext.getEventContext();
        if(ctx!=null) {
            UMOMessage msg = ctx.getMessage();
            for (Iterator iterator = msg.getPropertyNames().iterator(); iterator.hasNext();) {
                String propertyKey =  (String)iterator.next();
                setProperty(propertyKey, msg.getProperty(propertyKey));
            }
        }
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


    public String toString() {
        return "ExceptionMessage{" +
                 "message=" + message +
                ", context=" + context +
                "exception=" + exception +
                ", componentName='" + componentName + "'" +
                ", endpointUri=" + endpointUri +
                ", timeStamp=" + timeStamp +
                "}";
    }
}
