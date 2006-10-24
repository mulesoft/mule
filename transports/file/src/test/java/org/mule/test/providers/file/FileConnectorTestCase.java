/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.providers.file;

import com.mockobjects.dynamic.Mock;

import org.mule.MuleManager;
import org.mule.impl.ImmutableMuleEndpoint;
import org.mule.providers.file.FileConnector;
import org.mule.providers.file.FileMessageReceiver;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.providers.AbstractConnectorTestCase;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOEvent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageReceiver;

import java.io.File;
import java.util.Properties;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @author <a href="mailto:aperepel@gmail.com">Andrew Perepelytsya</a>
 * @version $Revision$
 */
public class FileConnectorTestCase extends AbstractConnectorTestCase
{
    static final long POLLING_FREQUENCY = 1234;
    static final long POLLING_FREQUENCY_OVERRIDE = 4321;

    private File validMessage;

    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        // The working directory is deleted on tearDown
        File dir = new File(MuleManager.getConfiguration().getWorkingDirectory(), "tmp");
        if (!dir.exists())
        {
            dir.mkdirs();
        }
        validMessage = File.createTempFile("simple", ".mule", dir);
        assertNotNull(validMessage);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractConnectorTestCase#createConnector()
     */
    public UMOConnector getConnector() throws Exception
    {
        UMOConnector connector = new FileConnector();
        connector.setName("testFile");
        connector.initialise();
        return connector;
    }

    public String getTestEndpointURI()
    {
        return "file://" + MuleManager.getConfiguration().getWorkingDirectory();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractConnectorTestCase#testDispatch()
     */
    public void testDispatch() throws Exception
    {
        UMOConnector connector = getConnector();

        Mock session = MuleTestUtils.getMockSession();
        UMOEndpoint endpoint = getTestEndpoint("simple", UMOImmutableEndpoint.ENDPOINT_TYPE_SENDER);
        UMOComponent component = getTestComponent(descriptor);
        UMOEvent event = getTestEvent("TestData");

        connector.registerListener(component, endpoint);
        connector.startConnector();
        connector.getDispatcher(new ImmutableMuleEndpoint("file:/foo", false)).dispatch(event);

        session.verify();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.providers.AbstractConnectorTestCase#testSend()
     */
    public void testSend() throws Exception
    {
        UMOConnector connector = getConnector();

        UMOEndpoint endpoint = getTestEndpoint("simple", UMOImmutableEndpoint.ENDPOINT_TYPE_SENDER);
        UMOEvent event = getTestEvent("TestData");
        UMOComponent component = getTestComponent(descriptor);

        connector.registerListener(component, endpoint);
        connector.startConnector();
        connector.getDispatcher(new ImmutableMuleEndpoint("file:/foo", false)).send(event);

    }

    public Object getValidMessage() throws Exception
    {
        return validMessage;
    }

    /**
     * Test polling frequency set on a connector.
     */
    public void testConnectorPollingFrequency() throws Exception
    {
        FileConnector connector = (FileConnector)getConnector();
        connector.setPollingFrequency(POLLING_FREQUENCY);

        UMOEndpoint endpoint = getTestEndpoint("simple", UMOImmutableEndpoint.ENDPOINT_TYPE_RECEIVER);
        UMOComponent component = getTestComponent(descriptor);
        UMOMessageReceiver receiver = connector.createReceiver(component, endpoint);
        assertEquals("Connector's polling frequency must not be ignored.", POLLING_FREQUENCY,
            ((FileMessageReceiver)receiver).getFrequency());
    }

    /**
     * Test polling frequency overridden at an endpoint level.
     */
    public void testPollingFrequencyEndpointOverride() throws Exception
    {
        FileConnector connector = (FileConnector)getConnector();
        // set some connector-level value which we are about to override
        connector.setPollingFrequency(-1);

        UMOEndpoint endpoint = getTestEndpoint("simple", UMOImmutableEndpoint.ENDPOINT_TYPE_RECEIVER);

        Properties props = new Properties();
        // Endpoint wants String-typed properties
        props.put(FileConnector.PROPERTY_POLLING_FREQUENCY, "" + POLLING_FREQUENCY_OVERRIDE);
        endpoint.setProperties(props);

        UMOComponent component = getTestComponent(descriptor);
        UMOMessageReceiver receiver = connector.createReceiver(component, endpoint);
        assertEquals("Polling frequency endpoint override must not be ignored.", POLLING_FREQUENCY_OVERRIDE,
            ((FileMessageReceiver)receiver).getFrequency());
    }
}
