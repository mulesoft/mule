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

import org.mule.providers.AbstractConnector;
import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.InitialisationException;

import javax.jbi.JBIException;
import javax.jbi.component.ComponentContext;
import javax.jbi.component.ComponentLifeCycle;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.management.ObjectName;

/**
 * <code>JbiConnector</code> can bind to a JBI container allowing components to
 * send events via Mule.
 */
public class JbiConnector extends AbstractConnector // TODO need to introduce this interface  runtime due to conflict implements ComponentLifeCycle
{
    private ObjectName extensionMBeanName;
    private ComponentContext context;
    private DeliveryChannel deliveryChannel;
    private MessageExchangeFactory exchangeFactory;


    protected void doInitialise() throws InitialisationException
    {
        // template method, nothing to do
    }

    protected void doDispose()
    {
        // template method
    }

    protected void doConnect() throws Exception
    {
        // template method
    }

    protected void doDisconnect() throws Exception
    {
        // template method
    }

    protected void doStart() throws UMOException
    {
        // template method
    }

    protected void doStop() throws UMOException
    {
        // template method
    }

    public String getProtocol()
    {
        return "jbi";
    }

    public ObjectName getExtensionMBeanName()
    {
        return extensionMBeanName;
    }

    public void setExtensionMBeanName(ObjectName extensionMBeanName)
    {
        this.extensionMBeanName = extensionMBeanName;
    }

    public ComponentContext getComponentContext()
    {
        return context;
    }

    public DeliveryChannel getDeliveryChannel()
    {
        return deliveryChannel;
    }

    public MessageExchangeFactory getExchangeFactory()
    {
        return exchangeFactory;
    }

    /**
     * @see ComponentLifeCycle#init(ComponentContext)
     */
    // TODO the start/stop/shutdown JBI lifecycle methods are rather picky,
    // we should probably review the spec here again
    public void init(ComponentContext componentContext) throws JBIException
    {
        this.context = componentContext;
        this.deliveryChannel = context.getDeliveryChannel();
        this.exchangeFactory = deliveryChannel.createExchangeFactory();
    }

    /**
     * @see ComponentLifeCycle#shutDown()
     */
    public void shutDown() throws JBIException
    {
        // nothing to do (for now?)
    }

}
