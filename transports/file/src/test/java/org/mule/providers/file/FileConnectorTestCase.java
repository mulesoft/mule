/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.file;

import org.mule.RegistryContext;
import org.mule.tck.providers.AbstractConnectorTestCase;
import org.mule.umo.provider.UMOConnector;
import org.mule.util.FileUtils;

import java.io.File;

public class FileConnectorTestCase extends AbstractConnectorTestCase
{
    static final long POLLING_FREQUENCY = 1234;
    static final long POLLING_FREQUENCY_OVERRIDE = 4321;

    private File validMessage;

    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        // The working directory is deleted on tearDown
        File tempDir = FileUtils.newFile(RegistryContext.getConfiguration().getWorkingDirectory(), "tmp");
        if (!tempDir.exists())
        {
            tempDir.mkdirs();
        }
        validMessage = File.createTempFile("simple", ".mule", tempDir);
        assertNotNull(validMessage);
    }

    protected void doTearDown() throws Exception
    {
        // TestConnector dispatches events via the test: protocol to test://test
        // endpoints, which seems to end up in a directory called "test" :(
        FileUtils.deleteTree(FileUtils.newFile(getTestConnector().getProtocol()));
        super.doTearDown();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.tck.providers.AbstractConnectorTestCase#createConnector()
     */
    // @Override
    public UMOConnector createConnector() throws Exception
    {
        UMOConnector connector = new FileConnector();
        connector.setName("testFile");
        return connector;
    }

    public String getTestEndpointURI()
    {
        return "file://" + RegistryContext.getConfiguration().getWorkingDirectory();
    }

//    /*
//     * (non-Javadoc)
//     *
//     * @see org.mule.tck.providers.AbstractConnectorTestCase#testDispatch()
//     */
//    public void testDispatch() throws Exception
//    {
//        UMOConnector connector = getConnector();
//
//        Mock session = MuleTestUtils.getMockSession();
//        UMOEndpoint endpoint = getTestEndpoint("simple", UMOImmutableEndpoint.ENDPOINT_TYPE_SENDER);
//        UMOComponent component = getTestComponent(descriptor);
//        UMOEvent event = getTestEvent("TestData");
//
//        connector.registerListener(component, endpoint);
//        connector.start();
//        connector.dispatch(new ImmutableMuleEndpoint("file:/foo", false), event);
//
//        session.verify();
//    }
//
//    /*
//     * (non-Javadoc)
//     *
//     * @see org.mule.tck.providers.AbstractConnectorTestCase#testSend()
//     */
//    public void testSend() throws Exception
//    {
//        UMOConnector connector = getConnector();
//
//        UMOEndpoint endpoint = getTestEndpoint("simple", UMOImmutableEndpoint.ENDPOINT_TYPE_SENDER);
//        UMOEvent event = getTestEvent("TestData");
//        UMOComponent component = getTestComponent(descriptor);
//
//        connector.registerListener(component, endpoint);
//        connector.start();
//        connector.send(new ImmutableMuleEndpoint("file:/foo", false), event);
//
//    }

    public Object getValidMessage() throws Exception
    {
        return validMessage;
    }

//    /**
//     * Test polling frequency set on a connector.
//     */
//    public void testConnectorPollingFrequency() throws Exception
//    {
//        FileConnector connector = (FileConnector)getConnector();
//        connector.setPollingFrequency(POLLING_FREQUENCY);
//
//        UMOEndpoint endpoint = getTestEndpoint("simple", UMOImmutableEndpoint.ENDPOINT_TYPE_RECEIVER);
//        UMOComponent component = getTestComponent(descriptor);
//        UMOMessageReceiver receiver = connector.createReceiver(component, endpoint);
//        assertEquals("Connector's polling frequency must not be ignored.", POLLING_FREQUENCY,
//            ((FileMessageReceiver)receiver).getFrequency());
//    }
//
//    /**
//     * Test polling frequency overridden at an endpoint level.
//     */
//    public void testPollingFrequencyEndpointOverride() throws Exception
//    {
//        FileConnector connector = (FileConnector)getConnector();
//        // set some connector-level value which we are about to override
//        connector.setPollingFrequency(-1);
//
//        UMOEndpoint endpoint = getTestEndpoint("simple", UMOImmutableEndpoint.ENDPOINT_TYPE_RECEIVER);
//
//        Properties props = new Properties();
//        // Endpoint wants String-typed properties
//        props.put(FileConnector.PROPERTY_POLLING_FREQUENCY, String.valueOf(POLLING_FREQUENCY_OVERRIDE));
//        endpoint.setProperties(props);
//
//        UMOComponent component = getTestComponent(descriptor);
//        UMOMessageReceiver receiver = connector.createReceiver(component, endpoint);
//        assertEquals("Polling frequency endpoint override must not be ignored.", POLLING_FREQUENCY_OVERRIDE,
//            ((FileMessageReceiver)receiver).getFrequency());
//    }
}
