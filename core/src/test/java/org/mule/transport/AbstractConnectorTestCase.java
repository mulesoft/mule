/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport;

import org.mule.api.DefaultMuleException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.api.transport.MessageAdapter;
import org.mule.api.transport.MessageDispatcherFactory;
import org.mule.api.transport.MessageRequesterFactory;
import org.mule.config.i18n.MessageFactory;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.transport.AbstractConnector;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

import java.beans.ExceptionListener;

/**
 * <code>AbstractConnectorTestCase</code> tests common behaviour of all endpoints and
 * provides 'reminder' methods for implementation specific interface methods
 */
public abstract class AbstractConnectorTestCase extends AbstractMuleTestCase
{
    protected String connectorName;

    public AbstractConnectorTestCase()
    {
        setStartContext(true);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    protected void doSetUp() throws Exception
    {
        Connector connector = createConnector();
        connectorName = connector.getName();
        if (connectorName == null)
        {
            fail("You need to set the connector name on the connector before returning it");
        }
        connector.setMuleContext(muleContext);
        muleContext.getRegistry().registerConnector(connector);
    }

    protected void doTearDown() throws Exception
    {
        Connector connector = getConnector();
        if (connector.isDisposed())
        {
            fail("Connector has been disposed prematurely - lifecycle problem? Instance: " + connector);
        }

        connector.dispose();
    }

    /** Look up the connector from the Registry */
    protected Connector getConnector()
    {
        return muleContext.getRegistry().lookupConnector(connectorName);
    }
    
    public void testConnectorExceptionHandling() throws Exception
    {
        Connector connector = getConnector();
        assertNotNull(connector);

        // Text exception handler
        Mock ehandlerMock = new Mock(ExceptionListener.class, "exceptionHandler");

        ehandlerMock.expect("exceptionThrown", C.isA(Exception.class));

        assertNotNull(connector.getExceptionListener());
        connector.setExceptionListener((ExceptionListener) ehandlerMock.proxy());
        connector.handleException(new DefaultMuleException(MessageFactory.createStaticMessage("Dummy")));

        if (connector instanceof AbstractConnector)
        {
            ehandlerMock.expect("exceptionThrown", C.isA(Exception.class));
            ((AbstractConnector) connector).exceptionThrown(
                    new DefaultMuleException(MessageFactory.createStaticMessage("Dummy")));
        }

        ehandlerMock.verify();

        connector.setExceptionListener(null);
        try
        {
            connector.handleException(new DefaultMuleException(MessageFactory.createStaticMessage("Dummy")));
            fail("Should have thrown exception as no strategy is set");
        }
        catch (RuntimeException e)
        {
            // expected
        }
    }

    public void testConnectorLifecycle() throws Exception
    {
        // this test used to use the connector created for this test, but since we need to
        // simulate disposal as well we have to create an extra instance here.

        Connector localConnector = this.createConnector();
        localConnector.setMuleContext(muleContext);
        localConnector.setName(connectorName+"-temp");
        // the connector did not come from the registry, so we need to initialise manually
        localConnector.initialise();
        localConnector.start();

        assertNotNull(localConnector);
        assertTrue(localConnector.isStarted());
        assertTrue(!localConnector.isDisposed());
        localConnector.stop();
        assertTrue(!localConnector.isStarted());
        assertTrue(!localConnector.isDisposed());
        localConnector.dispose();
        assertTrue(!localConnector.isStarted());
        assertTrue(localConnector.isDisposed());

        try
        {
            localConnector.start();
            fail("Connector cannot be restarted after being disposing");
        }
        catch (Exception e)
        {
            // expected
        }
    }

    public void testConnectorListenerSupport() throws Exception
    {
        Connector connector = getConnector();
        assertNotNull(connector);

        Service service = getTestService("anApple", Apple.class);

        ImmutableEndpoint endpoint = 
            muleContext.getRegistry().lookupEndpointFactory().getOutboundEndpoint(getTestEndpointURI());

        try
        {
            connector.registerListener(null, null);
            fail("cannot register null");
        }
        catch (Exception e)
        {
            // expected
        }

        try
        {
            connector.registerListener(null, endpoint);
            fail("cannot register null");
        }
        catch (Exception e)
        {
            // expected
        }

        try
        {
            connector.registerListener(service, null);
            fail("cannot register null");
        }
        catch (Exception e)
        {
            // expected
        }

        connector.registerListener(service, endpoint);

        // this should work
        connector.unregisterListener(service, endpoint);
        // so should this
        try
        {
            connector.unregisterListener(null, null);
            fail("cannot unregister null");
        }
        catch (Exception e)
        {
            // expected
        }
        try
        {
            connector.unregisterListener(service, null);
            fail("cannot unregister null");
        }
        catch (Exception e)
        {
            // expected
        }

        try
        {
            connector.unregisterListener(null, endpoint);
            fail("cannot unregister null");
        }
        catch (Exception e)
        {
            // expected
        }
        connector.unregisterListener(service, endpoint);
        muleContext.getRegistry().unregisterComponent(service.getName());
    }

    public void testConnectorBeanProps() throws Exception
    {
        Connector connector = getConnector();
        assertNotNull(connector);

        try
        {
            connector.setName(null);
            fail("Should throw IllegalArgumentException if name set to null");
        }
        catch (IllegalArgumentException e)
        {
            // expected
        }

        connector.setName("Test");
        assertEquals("Test", connector.getName());

        assertNotNull("Protocol must be set as a constant", connector.getProtocol());

    }

    public void testConnectorMessageAdapter() throws Exception
    {
        Connector connector = getConnector();
        assertNotNull(connector);
        MessageAdapter adapter = connector.getMessageAdapter(getValidMessage());
        assertNotNull(adapter);
    }

    public void testConnectorMessageDispatcherFactory() throws Exception
    {
        Connector connector = getConnector();
        assertNotNull(connector);

        MessageDispatcherFactory factory = connector.getDispatcherFactory();
        assertNotNull(factory);
    }

    public void testConnectorMessageRequesterFactory() throws Exception
    {
        Connector connector = getConnector();
        assertNotNull(connector);

        MessageRequesterFactory factory = connector.getRequesterFactory();
        assertNotNull(factory);
    }

    public void testConnectorInitialise() throws Exception
    {
        Connector connector = getConnector();
        try
        {
            connector.initialise();
            fail("A connector cannot be initialised more than once");
        }
        catch (Exception e)
        {
            // expected
        }
    }

    public abstract Connector createConnector() throws Exception;

    public abstract Object getValidMessage() throws Exception;

    public abstract String getTestEndpointURI();

}
