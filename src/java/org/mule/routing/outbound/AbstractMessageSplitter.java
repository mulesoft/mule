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
 * other components.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public abstract class AbstractMessageSplitter extends FilteringOutboundRouter
{
    //determines if the same endpoint will be matched multimple
    //times until a match is not found
    //This should be set by overriding classes
    protected boolean multimatch = true;

    public UMOMessage route(UMOMessage message, UMOSession session, boolean synchronous) throws RoutingException
    {
        String correlationId = (String)propertyExtractor.getPropertry(MuleProperties.MULE_CORRELATION_ID_PROPERTY, message);
        initialise(message);

        UMOEndpoint endpoint;
        //UMOMessage message;
        UMOMessage result = null;
        List list = getEndpoints();
        int i = 1;
        for (Iterator iterator = list.iterator(); iterator.hasNext();)
        {
            endpoint = (UMOEndpoint) iterator.next();
            message = getMessagePart(message, endpoint);
            if(message==null) {
                //Log a warning if there are no messages for a given endpont
                logger.warn("Message part is null for endpoint: " + endpoint.getEndpointURI().toString());
            }
            //We'll keep looping to get all messages for the current endpoint
            //before moving to the next endpoint
            //This can be turned off by setting the multimatch flag to false
            while (message != null)
            {
                try
                {
                    if(enableCorrelation != ENABLE_CORREATION_NEVER) {
                        boolean correlationSet = message.getCorrelationId()!=null;
                        if(correlationSet && (enableCorrelation == ENABLE_CORREATION_IF_NOT_SET)) {
                            logger.debug("CorrelationId is already set, not setting Correlation group size");
                        } else {
                            //the correlationId will be set by the AbstractOutboundRouter
                            //todo maybe the correlationId doesn't have to be set here
                            message.setCorrelationId(correlationId);
                            message.setCorrelationGroupSize(list.size());
                            message.setCorrelationSequence(i++);
                        }
                    }
//                    message.setCorrelationId(correlationId);
//                    message.setCorrelationSequence(i++);
//                    message.setCorrelationGroupSize(list.size());
                    if (synchronous)
                    {
                        result = send(session, message, endpoint);
                    } else
                    {
                        dispatch(session, message, endpoint);
                    }
                } catch (UMOException e)
                {
                    throw new CouldNotRouteOutboundMessageException(message, endpoint, e);
                }
                if(!multimatch) break;
                message = getMessagePart(message, endpoint);
            }
        }
        return result;
    }

    /**
     * Template method can be used to split the message up before the getMessagePart
     * method is called .
     *
     * @param message the message being routed
     */
    protected void initialise(UMOMessage message)
    {

    }

    /**
     * Retrieves a specific message part for the given endpoint. the message will then
     * be routed via the parovider.
     *
     * @param message  the current message being processed
     * @param endpoint the endpoint that will be used to route the resulting message
     *                 part
     * @return the message part to dispatch
     */
    protected abstract UMOMessage getMessagePart(UMOMessage message, UMOEndpoint endpoint);
}
