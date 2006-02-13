/*
 * $Header$
 * $Revision$
 * $Date$
 * -----------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 *
 */
package org.mule.providers.jms;

import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageReceiver;
import org.mule.providers.ConnectException;
import org.mule.providers.jms.filters.JmsSelectorFilter;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.LifecycleException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.util.PropertiesHelper;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.Topic;
import javax.resource.spi.work.Work;

/**
 *
 * Registers a single JmsMessage listene but uses a thread pool to process incoming messages
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class JmsMessageReceiver extends SingleJmsMessageReceiver
 {
    public JmsMessageReceiver(UMOConnector connector, UMOComponent component, UMOEndpoint endpoint)
            throws InitialisationException {
        super(connector, component, endpoint);
    }

    public void onMessage(Message message) {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Message received it is of type: " + message.getClass().getName());
                if (message.getJMSDestination() != null) {
                    logger.debug("Message received on " + message.getJMSDestination() + " ("
                            + message.getJMSDestination().getClass().getName() + ")");
                } else {
                    logger.debug("Message received on unknown destination");
                }
                logger.debug("Message CorrelationId is: " + message.getJMSCorrelationID());
                logger.debug("Jms Message Id is: " + message.getJMSMessageID());
            }

            if (message.getJMSRedelivered()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Message with correlationId: " + message.getJMSCorrelationID()
                            + " is redelivered. handing off to Exception Handler");
                }
                redeliveryHandler.handleRedelivery(message);
            }
            getWorkManager().scheduleWork(new Worker(message));
        } catch (Exception e) {
            handleException(e);
        }
    }

    private class Worker implements Work {
        private Message message;

        public Worker(Message message) {
            this.message = message;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Runnable#run()
         */
        public void run() {
            try {
                UMOMessageAdapter adapter = connector.getMessageAdapter(message);
                routeMessage(new MuleMessage(adapter));
            } catch (Exception e) {
                getConnector().handleException(e);
            }
        }

        public void release() {
        }
    }
}
