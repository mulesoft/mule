/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.file;

import org.mule.api.MuleMessage;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.api.transport.MessageReceiver;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.AbstractConnectorTestCase;
import org.mule.util.FileUtils;

import java.io.File;
import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FileConnectorTestCase extends AbstractConnectorTestCase
{
    private static final long POLLING_FREQUENCY = 1234;
    private static final long POLLING_FREQUENCY_OVERRIDE = 4321;
    private static final String VALID_MESSAGE = "validMessage";
    
    private File validMessage;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        // The working directory is deleted on tearDown
        File tempDir = FileUtils.newFile(muleContext.getConfiguration().getWorkingDirectory(), "tmp");
        if (!tempDir.exists())
        {
            tempDir.mkdirs();
        }

        validMessage = File.createTempFile("simple", ".mule", tempDir);
        assertNotNull(validMessage);
        FileUtils.writeStringToFile(validMessage, VALID_MESSAGE);
    }

    @Override
    protected void doTearDown() throws Exception
    {
        // TestConnector dispatches events via the test: protocol to test://test
        // endpoints, which seems to end up in a directory called "test" :(
        FileUtils.deleteTree(FileUtils.newFile(getTestConnector().getProtocol()));
        super.doTearDown();
    }

    @Override
    public Connector createConnector() throws Exception
    {
        FileConnector connector = new FileConnector(muleContext);
        connector.setName("testFile");
        connector.setOutputAppend(true);
        return connector;
    }

    @Override
    public String getTestEndpointURI()
    {
        return "file://" + muleContext.getConfiguration().getWorkingDirectory();
    }

    @Override
    public Object getValidMessage() throws Exception
    {
        return validMessage;
    }

    /**
     * Test polling frequency set on a connector.
     */
    @Test
    public void testConnectorPollingFrequency() throws Exception
    {
        FileConnector connector = (FileConnector) getConnector();
        connector.setPollingFrequency(POLLING_FREQUENCY);

        InboundEndpoint endpoint = getTestInboundEndpoint("simple");
        Service service = getTestService();
        MessageReceiver receiver = connector.createReceiver(service, endpoint);
        assertEquals("Connector's polling frequency must not be ignored.", POLLING_FREQUENCY,
                ((FileMessageReceiver) receiver).getFrequency());
    }

    /**
     * Test polling frequency overridden at an endpoint level.
     */
    @Test
    public void testPollingFrequencyEndpointOverride() throws Exception
    {
        FileConnector connector = (FileConnector) getConnector();
        // set some connector-level value which we are about to override
        connector.setPollingFrequency(-1);

        InboundEndpoint endpoint = getTestInboundEndpoint("simple");

        // Endpoint wants String-typed properties
        endpoint.getProperties().put(FileConnector.PROPERTY_POLLING_FREQUENCY, String.valueOf(POLLING_FREQUENCY_OVERRIDE));

        Service service = getTestService();
        MessageReceiver receiver = connector.createReceiver(service, endpoint);
        assertEquals("Polling frequency endpoint override must not be ignored.", POLLING_FREQUENCY_OVERRIDE,
                ((FileMessageReceiver) receiver).getFrequency());
    }

    @Test
    public void testOutputAppendEndpointOverride() throws Exception
    {
        FileConnector connector = (FileConnector) getConnector();

        EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(new URIBuilder("file://foo", muleContext));
        OutboundEndpoint endpoint = endpointBuilder.buildOutboundEndpoint();

        // Endpoint wants String-typed properties
        endpoint.getProperties().put("outputAppend", "true");

        try
        {
            connector.getDispatcherFactory().create(endpoint);
            fail("outputAppend cannot be configured on File endpoints");
        }
        catch (IllegalArgumentException e)
        {
            //expected
        }
    }

    @Test
    public void testOnlySingleDispatcherPerEndpoint() throws InitialisationException
    {
        // MULE-1773 implies that we must only have one dispatcher per endpoint
        FileConnector connector = (FileConnector) getConnector();

        assertEquals(1, connector.getMaxDispatchersActive());

        connector.setMaxDispatchersActive(2);

        // value must be unchanged
        assertEquals(1, connector.getMaxDispatchersActive());
    }
    
    /**
     * If the connector is configured not to do streaming it converts to byte[] so the original
     * input payload is not the same as the payload in the MuleMessage
     */
    @Test
    public void testConnectorMessageFactoryNonStreaming() throws Exception
    {
        Connector connector = getConnectorAndAssert();
        ((FileConnector) connector).setStreaming(false);

        Object payload = getValidMessage();
        MuleMessage message = connector.createMuleMessageFactory().create(payload, encoding);
        assertNotNull(message);
        
        byte[] messagePayload = (byte[]) message.getPayload();
        assertTrue(Arrays.equals(VALID_MESSAGE.getBytes(), messagePayload));
    }
}
