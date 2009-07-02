/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.bpm;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.context.WorkManager;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.api.transport.ConnectorException;
import org.mule.config.i18n.MessageFactory;
import org.mule.transport.AbstractMessageReceiver;

import java.util.Map;

import javax.resource.spi.work.Work;

/** Generates an incoming Mule event from an executing workflow process. */
public class ProcessMessageReceiver extends AbstractMessageReceiver
{

    private ProcessConnector connector = null;

    public ProcessMessageReceiver(Connector connector, Service service, InboundEndpoint endpoint)
            throws CreateException
    {
        super(connector, service, endpoint);
        this.connector = (ProcessConnector) connector;
    }

    public MuleMessage generateSynchronousEvent(String endpoint, Object payload, Map messageProperties) throws MuleException
    {
        logger.debug("Executing process is sending an event (synchronously) to Mule endpoint = " + endpoint);
        MuleMessage response = generateEvent(endpoint, payload, messageProperties, true);
        if (logger.isDebugEnabled())
        {
            logger.debug("Synchronous response is " + (response != null ? response.getPayload() : null));
        }
        return response;
    }

    public void generateAsynchronousEvent(String endpoint, Object payload, Map messageProperties) throws MuleException
    {
        logger.debug("Executing process is dispatching an event (asynchronously) to Mule endpoint = " + endpoint);
        try
        {
            WorkManager workManager = getWorkManager();
            if (workManager != null)
            {
                workManager.scheduleWork(new Worker(endpoint, payload, messageProperties));
            }
            else
            {
                throw new ConnectorException(MessageFactory.createStaticMessage("WorkManager not available"), getConnector());
            }
        }
        catch (Exception e)
        {
            handleException(e);
        }
    }

    protected MuleMessage generateEvent(String endpoint, Object payload, Map messageProperties, boolean synchronous) throws MuleException
    {
        MuleMessage message;
        if (payload instanceof MuleMessage)
        {
            message = (MuleMessage) payload;
        }
        else
        {
            message = new DefaultMuleMessage(connector.getMessageAdapter(payload), connector.getMuleContext());
        }
        message.addProperties(messageProperties);

        if (connector.isAllowGlobalDispatcher())
        {
            // TODO MULE-1221 This should use the "dynamic://" endpoint and not depend on the MuleClient.
            if (synchronous)
            {
                return connector.getMuleClient().send(endpoint, message);
            }
            else
            {
                connector.getMuleClient().dispatch(endpoint, message);
                return null;
            }
        }
        else
        {
            message.setStringProperty(ProcessConnector.PROPERTY_ENDPOINT, endpoint);
            return routeMessage(message, synchronous);
        }
    }

    private class Worker implements Work
    {
        private String endpoint;
        private Object payload;
        private Map messageProperties;

        public Worker(String endpoint, Object payload, Map messageProperties)
        {
            this.endpoint = endpoint;
            this.payload = payload;
            this.messageProperties = messageProperties;
        }

        public void run()
        {
            try
            {
                generateEvent(endpoint, payload, messageProperties, false);
            }
            catch (Exception e)
            {
                getConnector().handleException(e);
            }
        }

        public void release()
        { /*nop*/ }
    }
}
