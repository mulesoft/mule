/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jms;

import org.mule.impl.MuleMessage;
import org.mule.transaction.TransactionCallback;
import org.mule.transaction.TransactionTemplate;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.util.ClassUtils;

import javax.jms.Message;
import javax.resource.spi.work.Work;

/**
 * Registers a single JmsMessage listener but uses a thread pool to process incoming
 * messages
 */
public class JmsMessageReceiver extends SingleJmsMessageReceiver
{

    public JmsMessageReceiver(UMOConnector connector, UMOComponent component, UMOEndpoint endpoint)
        throws InitialisationException
    {
        super(connector, component, endpoint);
    }

    public void onMessage(Message message)
    {
        try
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Message received of type: " + ClassUtils.getSimpleName(message.getClass()));
                if (message.getJMSDestination() != null)
                {
                    logger.debug("Message received on " + message.getJMSDestination() + " ("
                                 + message.getJMSDestination().getClass().getName() + ")");
                }
                else
                {
                    logger.debug("Message received on unknown destination");
                }
                logger.debug("Message CorrelationId is: " + message.getJMSCorrelationID());
                logger.debug("Jms Message Id is: " + message.getJMSMessageID());
            }

            if (message.getJMSRedelivered())
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Message with correlationId: " + message.getJMSCorrelationID()
                                 + " has redelivered flag set, handing off to Exception Handler");
                }
                redeliveryHandler.handleRedelivery(message);
            }
            getWorkManager().scheduleWork(new Worker(message));
        }
        catch (Exception e)
        {
            handleException(e);
        }
    }

    private class Worker implements Work
    {
        private Message message;

        public Worker(Message message)
        {
            this.message = message;
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Runnable#run()
         */
        public void run()
        {
            try
            {
                if (message.getJMSRedelivered())
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Message with correlationId: " + message.getJMSCorrelationID()
                                     + " has redelivered flag set, handing off to Exception Handler");
                    }
                    redeliveryHandler.handleRedelivery(message);
                }
            }
            catch (Exception e)
            {
                getConnector().handleException(e);
            }


            TransactionTemplate tt = new TransactionTemplate(endpoint.getTransactionConfig(),
            connector.getExceptionListener(), connector.getManagementContext());

            // Receive messages and process them in a single transaction
            // Do not enable threading here, but serveral workers
            // may have been started
            TransactionCallback cb = new TransactionCallback()
            {
                public Object doInTransaction() throws Exception
                {
                    UMOMessageAdapter adapter = connector.getMessageAdapter(message);
                    return routeMessage(new MuleMessage(adapter));
                }
            };

            try
            {
                tt.execute(cb);
//                UMOMessageAdapter adapter = connector.getMessageAdapter(message);
//                routeMessage(new MuleMessage(adapter));
            }
            catch (Exception e)
            {
                getConnector().handleException(e);
            }
        }

        public void release()
        {
            // no op
        }
    }
}
