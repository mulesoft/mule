/*
 * $Id$
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

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleMessage;
import org.mule.providers.AbstractMessageReceiver;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.LifecycleException;
import org.mule.umo.provider.UMOConnector;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;

/**
 * Is a Jbi component that can receive events over Mule transports. This is an indeopendent
 * Jbi component implementation that can be used in Any Jbi container, including but not limited
 * to Mule JBI.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class JbiMessageReceiver extends AbstractMessageReceiver implements Work {

    protected ComponentContext context;

    protected JbiConnector connector;

    protected String name;

    private DeliveryChannel deliveryChannel;


    public JbiMessageReceiver(UMOConnector connector, UMOComponent component, UMOEndpoint endpoint) throws InitialisationException {
        super(connector, component, endpoint);
        name = component.getDescriptor().getName() + ".jbiReceiver";
        this.connector = (JbiConnector) connector;
        context = this.connector.getComponentContext();
        deliveryChannel = this.connector.getDeliveryChannel();
    }

    public void doConnect() throws Exception {
        // nothing to do
    }

    public void doDisconnect() throws Exception {
        // nothing to do
    }

    public void doStart() throws UMOException {
        try {
            getWorkManager().scheduleWork(this);
        } catch (WorkException e) {
            throw new LifecycleException(new Message(Messages.FAILED_TO_START_X, name), e, this);
        }
    }

    public void release() {
        // nothing to do
    }


    // TODO This receive code should be separated out to pluggable invocation strategies

    public void run() {
        while (connector.isStarted()) {
            try {
                final MessageExchange me = deliveryChannel.accept();
                if (me != null) {
                    getWorkManager().scheduleWork(new MessageExchangeWorker(me));
                }
            } catch (Exception e) {
                handleException(e);
            }
        }
    }

    private class MessageExchangeWorker implements Work {
        private MessageExchange me;

        public MessageExchangeWorker(MessageExchange me) {
            this.me = me;
        }

        public void release() {
            // nothing to do
        }

        public void run() {
            try {
                try {
                    NormalizedMessage nm = me.getMessage("IN");
                    if (nm != null) {
                        UMOMessage response = routeMessage(new MuleMessage(connector.getMessageAdapter(nm)));
                        if (response != null) {
                            NormalizedMessage nmResposne = me.createMessage();
                            JbiUtils.populateNormalizedMessage(response, nmResposne);
                            me.setMessage(nmResposne, "OUT");
                        }
                    } else {
                        logger.debug("'IN' message on exchange was not set");
                    }

                    done(me);
                } catch (MessagingException e) {
                    error(me, e);
                }
            } catch (Exception e) {
                handleException(e);
            }
        }
    }

    protected void error(MessageExchange me, Exception e) throws MessagingException {
        if (e instanceof Fault) {
            me.setFault((Fault) e);
        } else {
            me.setError(e);
        }
        me.setStatus(ExchangeStatus.ERROR);
        deliveryChannel.send(me);
    }

    protected void done(MessageExchange me) throws MessagingException {
        me.setStatus(ExchangeStatus.DONE);
        deliveryChannel.send(me);
    }
}
