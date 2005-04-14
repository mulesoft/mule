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
package org.mule.util.queue;

import org.mule.MuleManager;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleEvent;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;

import java.io.Serializable;

/**
 * <code>EventHolder</code> used to contain an event for serialisation.  The UMOEvent can be reconstructed from this
 * holder object.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
    public class EventHolder implements Serializable
    {
        private String eventId;
        private UMOMessage message;
        private String componentName;
        private String endpointName;
        private boolean synchronous;
        private boolean stopFurtherProcessing;
        private int timeout;

        public EventHolder(UMOEvent event)
        {
            this.eventId = event.getId();
            this.synchronous = event.isSynchronous();
            this.stopFurtherProcessing = event.isStopFurtherProcessing();
            this.message = event.getMessage();
            this.componentName = event.getComponent().getDescriptor().getName();
            this.endpointName = event.getEndpoint().getName();
            this.timeout = event.getTimeout();
        }

        public UMOEvent getEvent() throws InitialisationException {
            UMOSession session = MuleManager.getInstance().getModel().getComponentSession(componentName);
            if(session==null) {
                throw new InitialisationException(new Message(Messages.NO_SESSION_FOR_COMPONENT_X, componentName), this);
            }
            UMOEndpoint endpoint = session.getComponent().getDescriptor().getInboundRouter().getEndpoint(endpointName);
            if(endpoint == null) {
                endpoint = MuleManager.getInstance().lookupEndpoint(endpointName);
                if(endpoint == null) {
                    throw new InitialisationException(new Message(Messages.NO_ENDPOINT_X_FOR_COMPONENT_X, componentName), this);
                }
            }
            UMOEvent event = new MuleEvent(message, endpoint, session, eventId, synchronous);
            event.setStopFurtherProcessing(stopFurtherProcessing);
            event.setTimeout(timeout);
            return event;
        }
    }
