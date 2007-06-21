/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jbi.components;

import org.mule.impl.MuleEvent;
import org.mule.impl.MuleSession;
import org.mule.providers.AbstractConnector;
import org.mule.providers.jbi.JbiUtils;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;

/**
 * A JBI component that can dispatch Normalised Messages over a given transport
 * specified by the muleEndpoint property. This component can deliver events over any
 * Mule transport such as jms, ftp, htp, jdbc, ejb, etc.
 */
public class MuleDispatcher extends AbstractEndpointComponent implements MessageExchangeListener
{

    public void onExchange(MessageExchange messageExchange) throws MessagingException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("In Mule Dispatcher");
        }

        try
        {
            NormalizedMessage out = messageExchange.getMessage(IN);
            UMOMessage message = JbiUtils.createMessage(out);

            if (logger.isDebugEnabled())
            {
                logger.debug("Dispatching Message via Mule: " + message);
            }

            MuleSession session = new MuleSession(message,
                ((AbstractConnector)muleEndpoint.getConnector()).getSessionHandler());

            UMOEvent event = new MuleEvent(message, muleEndpoint, session, muleEndpoint.isSynchronous());

            if (muleEndpoint.isSynchronous())
            {
                logger.debug("Dispatching to: " + muleEndpoint.getEndpointURI());
                logger.debug("Payload is: " + event.getMessageAsString());

                UMOMessage result = muleEndpoint.send(event);
                // TODO send result back
            }
            else
            {
                muleEndpoint.dispatch(event);
            }
        }
        catch (Exception e)
        {
            handleException(e);
            error(messageExchange, e);
        }
    }
}
