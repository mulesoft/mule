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

import org.mule.providers.AbstractServiceEnabledConnector;
import org.mule.umo.UMOException;

import javax.jbi.JBIException;
import javax.jbi.component.ComponentContext;
import javax.jbi.component.ComponentLifeCycle;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.management.ObjectName;

/**
 * <code>JbiConnector</code> can bind to a Jbi container allowing components to send events via
 * Mule
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class JbiConnector extends AbstractServiceEnabledConnector implements ComponentLifeCycle {

    private ObjectName extensionMBeanName;
    private ComponentContext context;
    private DeliveryChannel deliveryChannel;
    private MessageExchangeFactory exchangeFactory;

    public String getProtocol() {
        return "jbi";
    }

    public ObjectName getExtensionMBeanName() {
        return extensionMBeanName;
    }

    public void setExtensionMBeanName(ObjectName extensionMBeanName) {
        this.extensionMBeanName = extensionMBeanName;
    }

    public ComponentContext getComponentContext() {
        return context;
    }

    public DeliveryChannel getDeliveryChannel() {
        return deliveryChannel;
    }

    public MessageExchangeFactory getExchangeFactory() {
        return exchangeFactory;
    }

    // TODO the start/stop/shutdown JBI lifecycle methods are rather picky,
    // we should probably review the spec here again

    public void init(ComponentContext componentContext) throws JBIException {
        this.context = componentContext;
        this.deliveryChannel = context.getDeliveryChannel();
        this.exchangeFactory = deliveryChannel.createExchangeFactory();
    }

    public void start() {
        try {
            startConnector();
        } catch (UMOException e) {
            handleException(e);
        }
    }

    public void stop() {
        try {
            stopConnector();
        } catch (UMOException e) {
            handleException(e);
        }
    }

    public void shutDown() throws JBIException {
        // nothing to do (for now?)
    }
}
