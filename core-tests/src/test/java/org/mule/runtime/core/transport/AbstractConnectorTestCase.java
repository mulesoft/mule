/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.exception.SystemExceptionHandler;
import org.mule.runtime.core.api.transport.Connector;
import org.mule.runtime.core.api.transport.MessageDispatcherFactory;
import org.mule.runtime.core.api.transport.MessageRequesterFactory;
import org.mule.runtime.core.api.transport.MuleMessageFactory;
import org.mule.runtime.core.config.i18n.MessageFactory;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.transport.AbstractConnector;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import org.junit.Test;

/**
 * <code>AbstractConnectorTestCase</code> tests common behaviour of all endpoints and
 * provides 'reminder' methods for implementation specific interface methods
 */
public abstract class AbstractConnectorTestCase extends AbstractMuleContextTestCase
{
    protected String connectorName;
    protected String encoding;

    @Override
    protected void doSetUp() throws Exception
    {
        Connector connector = createConnector();
        if (connector.getName() == null)
        {
            connector.setName("test");
        }
        connectorName = connector.getName();
        muleContext.getRegistry().registerConnector(connector);
        encoding = muleContext.getConfiguration().getDefaultEncoding();
    }

    @Override
    protected void doTearDown() throws Exception
    {
        Connector connector = getConnector();
        if (connector != null && connector.isDisposed())
        {
            fail("Connector has been disposed prematurely - lifecycle problem? Instance: " + connector);
        }
    }

    /** Look up the connector from the Registry */
    protected Connector getConnector()
    {
        return (muleContext == null) ?  null : muleContext.getRegistry().lookupConnector(connectorName);
    }

    protected Connector getConnectorAndAssert()
    {
        Connector connector = getConnector();
        assertNotNull(connector);
        return connector;
    }

    @Test
    public void testConnectorExceptionHandling() throws Exception
    {
        Connector connector = getConnectorAndAssert();

        // Text exception handler
        SystemExceptionHandler ehandlerMock = mock(SystemExceptionHandler.class);

        assertNotNull(muleContext.getExceptionListener());
        muleContext.setExceptionListener(ehandlerMock);
        muleContext.getExceptionListener().handleException(new DefaultMuleException(MessageFactory.createStaticMessage("Dummy")));

        if (connector instanceof AbstractConnector)
        {
            muleContext.getExceptionListener().handleException(
                    new DefaultMuleException(MessageFactory.createStaticMessage("Dummy")));
        }


        muleContext.setExceptionListener(null);
        try
        {
            muleContext.getExceptionListener().handleException(new DefaultMuleException(MessageFactory.createStaticMessage("Dummy")));
            fail("Should have thrown exception as no strategy is set");
        }
        catch (RuntimeException e)
        {
            // expected
        }
    }

    @Test
    public void testConnectorLifecycle() throws Exception
    {
        // this test used to use the connector created for this test, but since we need to
        // simulate disposal as well we have to create an extra instance here.

        Connector localConnector = createConnector();
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

    @Test
    public void testConnectorListenerSupport() throws Exception
    {
        Connector connector = getConnectorAndAssert();

        Flow flow = getTestFlow("anApple", Apple.class);

        InboundEndpoint endpoint =
            muleContext.getEndpointFactory().getInboundEndpoint(getTestEndpointURI());

        try
        {
            connector.registerListener(null, null, flow);
            fail("cannot register null");
        }
        catch (Exception e)
        {
            // expected
        }

        try
        {
            connector.registerListener(endpoint, null, flow);
            fail("cannot register null");
        }
        catch (Exception e)
        {
            // expected
        }

        try
        {
            connector.registerListener(null, getSensingNullMessageProcessor(), flow);
            fail("cannot register null");
        }
        catch (Exception e)
        {
            // expected
        }

        connector.registerListener(endpoint, getSensingNullMessageProcessor(), flow);

        // this should work
        connector.unregisterListener(endpoint, flow);
        // so should this
        try
        {
            connector.unregisterListener(null, flow);
            fail("cannot unregister null");
        }
        catch (Exception e)
        {
            // expected
        }
        try
        {
            connector.unregisterListener(null, flow);
            fail("cannot unregister null");
        }
        catch (Exception e)
        {
            // expected
        }

        try
        {
            connector.unregisterListener(null, flow);
            fail("cannot unregister null");
        }
        catch (Exception e)
        {
            // expected
        }
        connector.unregisterListener(endpoint, flow);
    }

    @Test
    public void testConnectorBeanProps() throws Exception
    {
        Connector connector = getConnectorAndAssert();

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

    /**
     * This test only asserts that the transport descriptor mechanism works for creating the
     * MuleMessageFactory. For exhaustive tests of MuleMessageFactory implementations see
     * {@link AbstractMuleMessageFactoryTestCase} and subclasses.
     */
    @Test
    public void testConnectorMuleMessageFactory() throws Exception
    {
        Connector connector = getConnectorAndAssert();

        MuleMessageFactory factory = connector.createMuleMessageFactory();
        assertNotNull(factory);
    }

    @Test
    public void testConnectorMessageDispatcherFactory() throws Exception
    {
        Connector connector = getConnectorAndAssert();

        MessageDispatcherFactory factory = connector.getDispatcherFactory();
        assertNotNull(factory);
    }

    @Test
    public void testConnectorMessageRequesterFactory() throws Exception
    {
        Connector connector = getConnectorAndAssert();

        MessageRequesterFactory factory = connector.getRequesterFactory();
        assertNotNull(factory);
    }

    @Test
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
