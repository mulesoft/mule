/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.ftp;

import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.tck.providers.AbstractConnectorTestCase;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageReceiver;

import java.util.Properties;

import org.apache.commons.pool.ObjectPool;

public class FTPConnectorTestCase extends AbstractConnectorTestCase
{
    static final long POLLING_FREQUENCY = 1234;
    static final long POLLING_FREQUENCY_OVERRIDE = 4321;
    static final String TEST_ENDPOINT_URI = "ftp://foo:bar@example.com";
    static final String VALID_MESSAGE = "This is a valid FTP message";

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractConnectorTestCase#createConnector()
     */
    public UMOConnector getConnector() throws Exception
    {
        return internalGetConnector(false);
    }

    public Object getValidMessage() throws Exception
    {
        return VALID_MESSAGE.getBytes();
    }

    public String getTestEndpointURI()
    {
        return TEST_ENDPOINT_URI;
    }

    /**
     * Test polling frequency set on a connector.
     */
    public void testConnectorPollingFrequency() throws Exception
    {
        UMOEndpoint endpoint = getTestEndpoint("mock", UMOImmutableEndpoint.ENDPOINT_TYPE_RECEIVER);
        UMOComponent component = getTestComponent(descriptor);
        UMOMessageReceiver receiver = ((FtpConnector) connector).createReceiver(component, endpoint);
        assertEquals("Connector's polling frequency must not be ignored.", POLLING_FREQUENCY,
            ((FtpMessageReceiver)receiver).getFrequency());
    }

    /**
     * Test polling frequency overridden at an endpoint level.
     */
    public void testPollingFrequencyEndpointOverride() throws Exception
    {
        UMOEndpoint endpoint = getTestEndpoint("mock", UMOImmutableEndpoint.ENDPOINT_TYPE_RECEIVER);

        Properties props = new Properties();
        // Endpoint wants String-typed properties
        props.put(FtpConnector.PROPERTY_POLLING_FREQUENCY, String.valueOf(POLLING_FREQUENCY_OVERRIDE));
        endpoint.setProperties(props);

        UMOComponent component = getTestComponent(descriptor);
        UMOMessageReceiver receiver = ((FtpConnector) connector).createReceiver(component, endpoint);
        assertEquals("Polling frequency endpoint override must not be ignored.", POLLING_FREQUENCY_OVERRIDE,
            ((FtpMessageReceiver)receiver).getFrequency());
    }

    public void testCustomFtpConnectionFactory() throws Exception
    {
        final String testObject = "custom object";

        final UMOEndpoint endpoint = new MuleEndpoint("ftp://test:test@example.com", false);
        final UMOEndpointURI endpointURI = endpoint.getEndpointURI();

        FtpConnectionFactory testFactory = new TestFtpConnectionFactory(endpointURI);

        ((FtpConnector) connector).setConnectionFactoryClass(testFactory.getClass().getName());
        // no validate call for simplicity
        ((FtpConnector) connector).setValidateConnections(false);

        ObjectPool pool = ((FtpConnector) connector).getFtpPool(endpointURI);
        Object obj = pool.borrowObject();
        assertEquals("Custom FTP connection factory has been ignored.", testObject, obj);
    }


    /**
     * Workaround. The super getConnector() call will init the connector,
     * but for some tests we want more config steps before the initialisation.
     */
    protected FtpConnector internalGetConnector(boolean initialiseConnector) throws Exception
    {
        FtpConnector connector = new FtpConnector();
        connector.setName("testFTP");
        connector.setPollingFrequency(POLLING_FREQUENCY);

        if (initialiseConnector)
        {
            connector.initialise();
        }

        return connector;
    }

    public static final class TestFtpConnectionFactory extends FtpConnectionFactory {

        public TestFtpConnectionFactory(UMOEndpointURI uri)
        {
            super(uri);
        }

        public Object makeObject() throws Exception
        {
            return "custom object";
        }

        public void activateObject(final Object obj) throws Exception
        {
            // empty no-op, do not call super
        }

    }
    
}
