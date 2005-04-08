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

import org.mule.umo.provider.UMOConnector;
import org.mule.config.i18n.Messages;
import org.apache.commons.collections.LRUMap;

import javax.jms.Message;
import javax.jms.JMSException;
import java.util.Map;
import java.util.Collections;

/**
 * <code>DefaultRedeliveryHandler</code> TODO
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class DefaultRedeliveryHandler implements RedeliveryHandler
{
    private Map messages = null;

    protected JmsConnector connector;

    public DefaultRedeliveryHandler() {
        messages = Collections.synchronizedMap(new LRUMap(256));
    }

    /**
     * The connector associated with this handler is set before <code>handleRedelivery()</code>
     * is called
     * @param connector the connector associated with this handler
     */
    public void setConnector(JmsConnector connector)
    {
        this.connector = connector;
    }

    /**
     * process the redelivered message.  If the Jms receiver should
     * process the message, it should be returned.  Otherwise the connector
     * should throw a <code>MessageRedeliveredException</code> to indicate
     * that the message should be handled by the connector Exception Handler.
     *
     * @param message
     */
    public void handleRedelivery(Message message) throws JMSException, MessageRedeliveredException {
        if(connector.getMaxRedelivey() <= 0) return;

        String id = message.getJMSMessageID();
        Integer i = (Integer)messages.remove(id);
        if(i==null) {
            messages.put(id, new Integer(1));
            return;
        } else if(i.intValue() == connector.getMaxRedelivey()) {
                throw new MessageRedeliveredException(message);
        } else {
            messages.put(id, new Integer(i.intValue()+1));
        }
    }
}
