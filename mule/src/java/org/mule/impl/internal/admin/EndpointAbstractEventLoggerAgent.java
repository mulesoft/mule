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
package org.mule.impl.internal.admin;

import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.MuleSession;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.UMOServerEvent;
import org.mule.umo.provider.UMOMessageDispatcher;

/**
 * <code>EndpointAbstractEventLoggerAgent</code> will forward server events to a configurered
 * endpoint uri.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class EndpointAbstractEventLoggerAgent extends AbstractEventLoggerAgent
{
    private String endpointAddress;
    private UMOEndpoint logEndpoint = null;
    private UMOSession session = null;

    protected void doInitialise() throws InitialisationException
    {
        //first see if we're logging events to an endpoint
        try
        {
            if(endpointAddress!=null) {
                logEndpoint = MuleEndpoint.getOrCreateEndpointForUri(endpointAddress, UMOEndpoint.ENDPOINT_TYPE_SENDER);
                //Create a dummy session for sending events
                session = new MuleSession();
            }
        } catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }
    }

    protected void logEvent(UMOServerEvent e)
    {
        if(logEndpoint!=null) {
            try
            {
                UMOMessageDispatcher  dispatcher = logEndpoint.getConnector().getDispatcher("ANY");
                UMOEvent event = new MuleEvent(new MuleMessage(e.toString(), null), logEndpoint, session, false);
                dispatcher.dispatch(event);
            } catch (Exception e1)
            {
                logger.error("Failed to dispatch event: " + e.toString() + " over endpoint: " +
                        logEndpoint + ". Error is: " + e1.getMessage(),e1);
            }
        }
    }

    /**
     * Should be a 1 line description of the agent
     *
     * @return
     */
    public String getDescription()
    {
        StringBuffer buf = new StringBuffer();
        buf.append(getName()).append(": ");
        if(endpointAddress!=null) {
            buf.append("Forwarding events to: " + endpointAddress);
        } else {
            buf.append("No log forwarding endpoint is configured!");
        }
        return buf.toString();
    }
}
