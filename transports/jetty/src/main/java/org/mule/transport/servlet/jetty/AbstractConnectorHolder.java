/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.servlet.jetty;

import org.mule.api.MuleException;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.transport.MessageReceiver;

import javax.servlet.Servlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mortbay.jetty.Connector;

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
