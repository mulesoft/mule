/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.providers;

import org.mule.MuleException;
import org.mule.config.i18n.MessageFactory;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.impl.model.seda.SedaModel;
import org.mule.providers.AbstractConnector;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.umo.provider.UMOMessageDispatcherFactory;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

import java.beans.ExceptionListener;

/**
 * <code>AbstractConnectorTestCase</code> tests common behaviour of all endpoints and
 * provides 'reminder' methods for implementation specific interface methods
 */
public abstract class AbstractConnectorTestCase extends AbstractMuleTestCase
{
    //TODO RM*: Remove these instnace variables and obtain everything from the registry
    //Can do this once the code base stabilises a bit
    private MuleDescriptor descriptor;
    private UMOModel model;
    private UMOConnector connector;
    private String connectorName;

    public MuleDescriptor getDescriptor()
    {
        return descriptor;
    }

    public UMOConnector getConnector()
    {
        return connector;
    }

    public UMOModel getModel()
    {
        return model;
    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    protected void doSetUp() throws Exception
    {
        model = new SedaModel();
        model.setName("default");
        managementContext.getRegistry().registerModel(model);
        descriptor = getTestDescriptor("apple", Apple.class.getName());
        connector = createConnector();
        connectorName = connector.getName();
        if (connectorName == null)
        {
            fail("You need to set the connector name on the connector before returning it");
        }
        managementContext.getRegistry().registerConnector(connector);
        // TODO MULE-1988
        managementContext.start();
    }

    protected void doTearDown() throws Exception
    {
        if (!connector.isDisposed())
        {
            connector.dispose();
        }
    }

    public void testConnectorExceptionHandling() throws Exception
    {
        assertNotNull(connector);

        // Text exception handler
        Mock ehandlerMock = new Mock(ExceptionListener.class, "exceptionHandler");

        ehandlerMock.expect("exceptionThrown", C.isA(Exception.class));

        assertNotNull(connector.getExceptionListener());
        connector.setExceptionListener((ExceptionListener) ehandlerMock.proxy());
        connector.handleException(new MuleException(MessageFactory.createStaticMessage("Dummy")));

        if (connector instanceof AbstractConnector)
        {
            ehandlerMock.expect("exceptionThrown", C.isA(Exception.class));
            ((AbstractConnector) connector).exceptionThrown(new MuleException(
                MessageFactory.createStaticMessage("Dummy")));
        }

        ehandlerMock.verify();

        connector.setExceptionListener(null);
        try
        {
            connector.handleException(new MuleException(MessageFactory.createStaticMessage("Dummy")));
            fail("Should have thrown exception as no strategy is set");
        }
        catch (RuntimeException e)
        {
            // expected
        }
    }

    public void testConnectorLifecycle() throws Exception
    {
        UMOConnector connector = managementContext.getRegistry().lookupConnector(connectorName);

        assertNotNull(connector);
        assertTrue(connector.isStarted());
        assertTrue(!connector.isDisposed());
        connector.stop();
        assertTrue(!connector.isStarted());
        assertTrue(!connector.isDisposed());
        connector.dispose();
        assertTrue(!connector.isStarted());
        assertTrue(connector.isDisposed());

        try
        {
            connector.start();
            fail("Connector cannot be restarted after being disposing");
        }
        catch (Exception e)
        {
            // expected
        }
    }

    public void testConnectorListenerSupport() throws Exception
    {
        assertNotNull(connector);

        MuleDescriptor d = getTestDescriptor("anApple", Apple.class.getName());
        d.setModelName(model.getName());
        managementContext.getRegistry().registerService(d);
        UMOComponent component = model.getComponent(d.getName());

//        UMOEndpoint endpoint = new MuleEndpoint("test", new MuleEndpointURI(getTestEndpointURI()), connector,
//                null, UMOEndpoint.ENDPOINT_TYPE_SENDER, 0, null, new HashMap());
        UMOEndpoint endpoint = managementContext.getRegistry().createEndpointFromUri(
                new MuleEndpointURI(getTestEndpointURI()), UMOEndpoint.ENDPOINT_TYPE_SENDER);

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
            connector.registerListener(component, null);
            fail("cannot register null");
        }
        catch (Exception e)
        {
            // expected
        }

        connector.registerListener(component, endpoint);

        // this should work
        connector.unregisterListener(component, endpoint);
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
            connector.unregisterListener(component, null);
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
        connector.unregisterListener(component, endpoint);
        model.unregisterComponent(d);
    }

    public void testConnectorBeanProps() throws Exception
    {
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
        UMOConnector connector = managementContext.getRegistry().lookupConnector(connectorName);
        assertNotNull(connector);
        UMOMessageAdapter adapter = connector.getMessageAdapter(getValidMessage());
        assertNotNull(adapter);
    }

    public void testConnectorMessageDispatcherFactory() throws Exception
    {
        UMOConnector connector = managementContext.getRegistry().lookupConnector(connectorName);
        assertNotNull(connector);

        UMOMessageDispatcherFactory factory = connector.getDispatcherFactory();
        assertNotNull(factory);
    }

    public void testConnectorInitialise() throws Exception
    {
        UMOConnector connector = managementContext.getRegistry().lookupConnector(connectorName);

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

    public abstract UMOConnector createConnector() throws Exception;

    public abstract Object getValidMessage() throws Exception;

    public abstract String getTestEndpointURI();
}
