/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jbi;

import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;

/**
 * <code>TcpMessageDispatcher</code> will send transformed mule events over tcp.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class JbiMessageDispatcher extends AbstractMessageDispatcher
{
    private JbiConnector connector;

    public JbiMessageDispatcher(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
        this.connector = (JbiConnector)endpoint.getConnector();
    }

    protected void doDispose()
    {
        // nothing to do
    }

    protected void doDispatch(UMOEvent event) throws Exception
    {
        InOnly exchange = connector.getExchangeFactory().createInOnlyExchange();
        NormalizedMessage message = exchange.createMessage();
        exchange.setInMessage(message);
        JbiUtils.populateNormalizedMessage(event.getMessage(), message);
        done(exchange);
    }

    protected UMOMessage doSend(UMOEvent event) throws Exception
    {
        InOut exchange = connector.getExchangeFactory().createInOutExchange();
        NormalizedMessage message = exchange.createMessage();
        exchange.setInMessage(message);
        JbiUtils.populateNormalizedMessage(event.getMessage(), message);
        NormalizedMessage nm = exchange.getOutMessage();
        UMOMessage response = null;
        if (nm != null)
        {
            response = new MuleMessage(connector.getMessageAdapter(nm));
        }
        done(exchange);
        return response;
    }

    /**
     * Make a specific request to the underlying transport
     * 
     * @param endpoint the endpoint to use when connecting to the resource
     * @param timeout the maximum time the operation should block before returning.
     *            The call should return immediately if there is data available. If
     *            no data becomes available before the timeout elapses, null will be
     *            returned
     * @return the result of the request wrapped in a UMOMessage object. Null will be
     *         returned if no data was avaialable
     * @throws Exception if the call to the underlying protocal cuases an exception
     */
    protected UMOMessage doReceive(UMOImmutableEndpoint endpoint, long timeout) throws Exception
    {
        throw new UnsupportedOperationException("doReceive");
    }

    public Object getDelegateSession() throws UMOException
    {
        return null;
    }

    protected void error(MessageExchange me, Fault fault) throws MessagingException
    {
        me.setFault(fault);
        me.setStatus(ExchangeStatus.ERROR);
        connector.getDeliveryChannel().send(me);
    }

    protected void done(MessageExchange me) throws MessagingException
    {
        me.setStatus(ExchangeStatus.DONE);
        connector.getDeliveryChannel().send(me);
    }

    protected void doConnect(UMOImmutableEndpoint endpoint) throws Exception
    {
        // no op
    }

    protected void doDisconnect() throws Exception
    {
        // no op
    }

}
