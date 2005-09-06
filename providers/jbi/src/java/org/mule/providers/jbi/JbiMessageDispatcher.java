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
package org.mule.providers.jbi;

import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageDispatcher;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpointURI;

import javax.jbi.messaging.*;

/**
 * <code>TcpMessageDispatcher</code> will send transformed mule events over
 * tcp.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class JbiMessageDispatcher extends AbstractMessageDispatcher {
    private JbiConnector connector;

    public JbiMessageDispatcher(JbiConnector connector) {
        super(connector);
        this.connector = connector;
    }

    public void doDispose() {

    }

    public void doDispatch(UMOEvent event) throws Exception {
        InOnly exchange = connector.getExchangeFactory().createInOnlyExchange();
        NormalizedMessage message = exchange.createMessage();
        exchange.setInMessage(message);
        JbiUtils.populateNormalizedMessage(event.getMessage(), message);
        done(exchange);
    }

    public UMOMessage doSend(UMOEvent event) throws Exception {
        InOut exchange = connector.getExchangeFactory().createInOutExchange();
        NormalizedMessage message = exchange.createMessage();
        exchange.setInMessage(message);
        JbiUtils.populateNormalizedMessage(event.getMessage(), message);
        NormalizedMessage nm = exchange.getOutMessage();
        UMOMessage response = null;
        if(nm!=null) {
            response = new MuleMessage(connector.getMessageAdapter(nm));
        }
        done(exchange);
        return response;
    }

    public UMOMessage receive(UMOEndpointURI endpointUri, long timeout) throws Exception {
        throw new UnsupportedOperationException("JbiMessageDispatcher:receiver");
    }

    public Object getDelegateSession() throws UMOException {
        return null;
    }

    protected void error(MessageExchange me, Fault fault) throws MessagingException {
        me.setFault(fault);
        me.setStatus(ExchangeStatus.ERROR);
        connector.getDeliveryChannel().send(me);
    }

    protected void done(MessageExchange me) throws MessagingException {
        me.setStatus(ExchangeStatus.DONE);
        connector.getDeliveryChannel().send(me);
    }

}
