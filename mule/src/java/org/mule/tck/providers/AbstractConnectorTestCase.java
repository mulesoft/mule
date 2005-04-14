/*
 * $Header:
 * /cvsroot/mule/mule/src/test/org/mule/test/tck/providers/AbstractConnectorTestCase.java,v
 * 1.2 2003/11/25 21:45:05 rossmason Exp $ $Revision$ $Date: 2003/11/25
 * 21:45:05 $
 * $Header$ $Revision$ $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved. http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *  
 */

package org.mule.tck.providers;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import org.mule.MuleException;
import org.mule.MuleManager;
import org.mule.config.i18n.Message;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.AbstractConnector;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.umo.provider.UMOMessageDispatcher;

import java.beans.ExceptionListener;
import java.util.HashMap;


/**
 * <code>AbstractConnectorTestCase</code> tests common behaviour of all
 * endpoints and provides 'reminder' methods for implementation specific
 * interface methods
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractConnectorTestCase extends AbstractMuleTestCase
{
    protected MuleDescriptor descriptor;

    protected UMOConnector connector;

    /*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
    protected void setUp() throws Exception
    {
        super.setUp();
        descriptor = getTestDescriptor("orange", Orange.class.getName());
        connector = getConnector();
    }

    protected void tearDown() throws Exception
    {
        if(!connector.isDisposed())
            connector.dispose();
    }

    public void testConnectorExceptionHandling() throws Exception
    {
        assertNotNull(connector);

        //Text exception handler
        Mock ehandlerMock = new Mock(ExceptionListener.class, "exceptionHandler");

        ehandlerMock.expect("exceptionThrown", C.isA(Exception.class));

        assertNotNull(connector.getExceptionListener());
        connector.setExceptionListener((ExceptionListener) ehandlerMock.proxy());
        connector.handleException(new MuleException(Message.createStaticMessage("Dummy")));

        if (connector instanceof AbstractConnector)
        {
            ehandlerMock.expect("exceptionThrown", C.isA(Exception.class));
            ((AbstractConnector) connector).exceptionThrown(new MuleException(Message.createStaticMessage("Dummy")));
        }

        ehandlerMock.verify();

        connector.setExceptionListener(null);
        try
        {
            connector.handleException(new MuleException(Message.createStaticMessage("Dummy")));
            fail("Should have thrown exception as no strategy is set");
        }
        catch (RuntimeException e)
        {
            // expected
        }
    }

    public void testConnectorLifecycle() throws Exception
    {
        assertNotNull(connector);

        assertTrue(!connector.isStarted());
        assertTrue(!connector.isDisposed());
        connector.start();
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

//        try
//        {
//            connector.stop();
//            fail("Connector cannot be stopped after being disposing");
//        }
//        catch (Exception e)
//        {
//            // expected
//        }
    }

    public void testConnectorListenerSupport() throws Exception
    {
        assertNotNull(connector);


        MuleDescriptor d = getTestDescriptor("anOrange", Orange.class.getName());

        UMOComponent component = MuleManager.getInstance().getModel().registerComponent(d);
        UMOEndpoint endpoint = new MuleEndpoint("test", new MuleEndpointURI(getTestEndpointURI()),
            connector, null, UMOEndpoint.ENDPOINT_TYPE_SENDER, 0, null);

        try
        {
            connector.registerListener(null, null);
            fail("cannot register null");
        }
        catch (Exception e)
        { /* expected */
        }

        try
        {
            connector.registerListener(null, endpoint);
            fail("cannot register null");
        }
        catch (Exception e)
        { /* expected */
        }

        try
        {
            connector.registerListener(component, null);
            fail("cannot register null");
        }
        catch (Exception e)
        { /* expected */
        }

        connector.registerListener(component, endpoint);

        //this should work
        connector.unregisterListener(component, endpoint);
        //so should this
        try
        {
            connector.unregisterListener(null, null);
            fail("cannot unregister null");
        }
        catch (Exception e)
        {
            //expected
        }
        try
        {
            connector.unregisterListener(component, null);
            fail("cannot unregister null");
        }
        catch (Exception e)
        {
            //expected
        }

        try
        {
            connector.unregisterListener(null, endpoint);
            fail("cannot unregister null");
        }
        catch (Exception e)
        {
            //expected
        }
        connector.unregisterListener(component, endpoint);
        MuleManager.getInstance().getModel().unregisterComponent(d);
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
        { /* expected */
        }

        connector.setName("Test");
        assertEquals("Test", connector.getName());

        assertNotNull("Protocol must be set as a constant", connector.getProtocol());

        //test do threading prop
        AbstractConnector tempCnn = null;

        HashMap props = new HashMap();

        if (connector instanceof AbstractConnector)
        {
            tempCnn = (AbstractConnector) connector;

            tempCnn.setRetryFrequency(-1);
            assertEquals(AbstractConnector.DEFAULT_RETRY_FREQUENCY, tempCnn.getRetryFrequency());

            tempCnn.setRetryCount(-1);
            assertEquals(AbstractConnector.DEFAULT_RETRY_COUNT, tempCnn.getRetryCount());
        }
    }

    public void testConnectorMessageAdapter() throws Exception
    {
        UMOConnector connector = getConnector();
        assertNotNull(connector);
        UMOMessageAdapter adapter = connector.getMessageAdapter(getValidMessage());
        assertNotNull(adapter);
    }

    public void testConnectorMessageDispatcher() throws Exception
    {
        UMOConnector connector = getConnector();
        assertNotNull(connector);
        assertNotNull(connector.getDispatcherFactory());
        UMOMessageDispatcher dispatcher = connector.getDispatcher("dummy");
        assertNotNull(dispatcher);
        assertEquals(connector, dispatcher.getConnector());
    }

    public void testConnectorInitialise() throws Exception
    {
        UMOConnector connector = getConnector();

        try
        {
            connector.initialise();
            fail("A connector cannot be initialised more than once");
        } catch (Exception e)
        {
            //expected
        }
    }

    public abstract UMOConnector getConnector() throws Exception;

    public abstract Object getValidMessage() throws Exception;

    public abstract String getTestEndpointURI();
}
