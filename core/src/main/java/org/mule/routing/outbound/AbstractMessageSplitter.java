/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.outbound;

import org.mule.config.MuleProperties;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.routing.CouldNotRouteOutboundMessageException;
import org.mule.umo.routing.RoutingException;

import java.util.Iterator;
import java.util.List;

/**
 * <code>AbstractMessageSplitter</code> is an outbound Message Splitter used to
 * split the contents of a received message into sup parts that can be processed by
 * other components. Each Part is fired as a separate event to each endpoint on the
 * router. The endpoints can have filters on them to receive only certain message
 * parts
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public abstract class AbstractMessageSplitter extends FilteringOutboundRouter
{
    // Determines if the same endpoint will be matched multiple times until a
    // match is not found. This should be set by overriding classes.
    protected boolean multimatch = true;

    // flag which, if true, makes the splitter honour settings such as remoteSync and
    // synchronous on the endpoint
    protected boolean honorSynchronicity = false;

    public UMOMessage route(UMOMessage message, UMOSession session, boolean synchronous)
        throws RoutingException
    {
        String correlationId = (String)propertyExtractor.getProperty(
            MuleProperties.MULE_CORRELATION_ID_PROPERTY, message);
        initialise(message);

        UMOEndpoint endpoint;
        UMOMessage result = null;
        List list = getEndpoints();
        int correlationSequence = 1;
        for (Iterator iterator = list.iterator(); iterator.hasNext();)
        {
            endpoint = (UMOEndpoint)iterator.next();
            message = getMessagePart(message, endpoint);
            if (message == null)
            {
                // Log a warning if there are no messages for a given endpoint
                logger.warn("Message part is null for endpoint: " + endpoint.getEndpointURI().toString());
            }
            // We'll keep looping to get all messages for the current endpoint
            // before moving to the next endpoint
            // This can be turned off by setting the multimatch flag to false
            while (message != null)
            {
                if (honorSynchronicity)
                {
                    synchronous = endpoint.isSynchronous();
                }
                try
                {
                    if (enableCorrelation != ENABLE_CORRELATION_NEVER)
                    {
                        boolean correlationSet = message.getCorrelationId() != null;
                        if (!correlationSet && (enableCorrelation == ENABLE_CORRELATION_IF_NOT_SET))
                        {
                            message.setCorrelationId(correlationId);
                        }

                        // take correlation group size from the message
                        // properties, set by concrete message splitter
                        // implementations
                        final int groupSize = message.getCorrelationGroupSize();
                        message.setCorrelationGroupSize(groupSize);
                        message.setCorrelationSequence(correlationSequence++);
                    }
                    if (honorSynchronicity)
                    {
                        message.setBooleanProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY,
                            endpoint.isRemoteSync());
                    }
                    if (synchronous)
                    {
                        result = send(session, message, endpoint);
                    }
                    else
                    {
                        dispatch(session, message, endpoint);
                    }
                }
                catch (UMOException e)
                {
                    throw new CouldNotRouteOutboundMessageException(message, endpoint, e);
                }
                if (!multimatch)
                {
                    break;
                }
                message = getMessagePart(message, endpoint);
            }
        }
        return result;
    }

    /**
     * Template method that can be used to split the message up before the
     * getMessagePart method is called .
     * 
     * @param message the message being routed
     */
    protected void initialise(UMOMessage message)
    {
        // nothing to do
    }

    public boolean isHonorSynchronicity()
    {
        return honorSynchronicity;
    }

    /**
     * Sets the flag indicating whether the splitter honurs endpoint settings
     * 
     * @param honorSynchronicity flag setting
     */
    public void setHonorSynchronicity(boolean honorSynchronicity)
    {
        this.honorSynchronicity = honorSynchronicity;
    }

    /**
     * Retrieves a specific message part for the given endpoint. the message will
     * then be routed via the provider. <p/> <strong>NOTE:</strong>Implementations
     * must provide proper synchronization for shared state (payload, properties,
     * etc.)
     * 
     * @param message the current message being processed
     * @param endpoint the endpoint that will be used to route the resulting message
     *            part
     * @return the message part to dispatch
     */
    protected abstract UMOMessage getMessagePart(UMOMessage message, UMOEndpoint endpoint);
}
