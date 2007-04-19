/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.bpm;

import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageReceiver;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;

import java.util.Map;

import javax.resource.spi.work.Work;

/**
 * Generates an incoming Mule event from an executing workflow process.
 */
public class ProcessMessageReceiver extends AbstractMessageReceiver {

    private ProcessConnector connector = null;

    public ProcessMessageReceiver(UMOConnector connector, UMOComponent component, UMOEndpoint endpoint)
            throws InitialisationException {
        super(connector, component, endpoint);
        this.connector = (ProcessConnector) connector;
    }

    public UMOMessage generateSynchronousEvent(String endpoint, Object payload, Map messageProperties) throws UMOException {
        logger.debug("Executing process is sending an event (synchronously) to Mule endpoint = " + endpoint);
        return generateEvent(endpoint, payload, messageProperties, true);
    }

    public void generateAsynchronousEvent(String endpoint, Object payload, Map messageProperties) throws UMOException {
        logger.debug("Executing process is dispatching an event (asynchronously) to Mule endpoint = " + endpoint);
        try {
            getWorkManager().scheduleWork(new Worker(endpoint, payload, messageProperties));
        } catch (Exception e) {
            handleException(e);
        }
    }

    protected UMOMessage generateEvent(String endpoint, Object payload, Map messageProperties, boolean synchronous) throws UMOException {
        UMOMessage message;
        if (payload instanceof UMOMessage) {
            message = (UMOMessage) payload;
        } else {
            message = new MuleMessage(connector.getMessageAdapter(payload));
        }
        message.addProperties(messageProperties);

        if (connector.isAllowGlobalDispatcher()) {
            // TODO MULE-1221 This should use the "dynamic://" endpoint and not depend on the MuleClient.
            if (synchronous) {
                return connector.getMuleClient().send(endpoint, message);
            } else {
                connector.getMuleClient().dispatch(endpoint, message);
                return null;
            }
        }
        else {
            message.setStringProperty(ProcessConnector.PROPERTY_ENDPOINT, endpoint);
            return routeMessage(message, synchronous);
        }
    }

    private class Worker implements Work {
        private String endpoint;
        private Object payload;
        private Map messageProperties;

        public Worker(String endpoint, Object payload, Map messageProperties) {
            this.endpoint = endpoint;
            this.payload = payload;
            this.messageProperties = messageProperties;
        }

        public void run() {
            try {
                generateEvent(endpoint, payload, messageProperties, false);
            } catch (Exception e) {
                getConnector().handleException(e);
            }
        }

        public void release() { /*nop*/ }
    }

    protected void doConnect() throws Exception
    {
        // nothing to do
    }

    protected void doDisconnect() throws Exception
    {
        // nothing to do
    }

    protected void doStart() throws UMOException
    {
        // nothing to do
    }

    protected void doStop() throws UMOException
    {
        // nothing to do
    }

    protected void doDispose()
    {
        // nothing to do               
    }

}
