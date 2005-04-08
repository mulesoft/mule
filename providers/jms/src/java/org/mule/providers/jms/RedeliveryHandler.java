/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.jms;

import javax.jms.Message;
import javax.jms.JMSException;

/**
 * <code>RedeliveryHandler</code> is used to control how redelivered
 * messages are processed by a connector.  Typically, a messsage will
 * be re-tried once or twice before throwing an exception.  Then the
 * Exception Strategy on the connector can be used to forward the message
 * to a DLQ or log the failure.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public interface RedeliveryHandler {

    /**
     * The connector associated with this handler is set before <code>handleRedelivery()</code>
     * is called
     * @param connector the connector associated with this handler
     */
    public void setConnector(JmsConnector connector);

    /**
     * process the redelivered message.  If the Jms receiver should
     * process the message, it should be returned.  Otherwise the connector
     * should throw a <code>MessageRedeliveredException</code> to indicate
     * that the message should be handled by the connector Exception Handler.
     * @param message
     * @throws JMSException if there is a problem reading message properties
     * @throws MessageRedeliveredException should be thrown if the message should
     * be handled by the connection exception handler
     */
    public void handleRedelivery(Message message) throws JMSException, MessageRedeliveredException;
}
