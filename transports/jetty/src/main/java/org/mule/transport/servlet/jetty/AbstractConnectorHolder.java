/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty;

import org.mule.api.MuleException;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.transport.MessageReceiver;

import javax.servlet.Servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.server.Connector;

/**
 * TODO
 */
public abstract class AbstractConnectorHolder<S extends Servlet, R extends MessageReceiver> implements ConnectorHolder<S, R>
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(AbstractConnectorHolder.class);
    protected Connector connector;
    protected S servlet;
    protected boolean started = false;

    public AbstractConnectorHolder(Connector connector, S servlet, R receiver)
    {
        this.connector = connector;
        this.servlet = servlet;
    }

    @Override
    public S getServlet()
    {
        return servlet;
    }

    @Override
    public Connector getConnector()
    {
        return connector;
    }


    @Override
    public void start() throws MuleException
    {
        try
        {
            connector.start();
            started = true;
        }
        catch (Exception e)
        {
            throw new LifecycleException(e, this);
        }
    }

    @Override
    public void stop() throws MuleException
    {
        try
        {
            connector.stop();
            started = false;
        }
        catch (Exception e)
        {
            logger.warn("Jetty connector did not close cleanly: " + e.getMessage());
        }
    }

}
