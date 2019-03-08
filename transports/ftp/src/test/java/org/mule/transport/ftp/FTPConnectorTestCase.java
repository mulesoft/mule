/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ftp;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.api.transport.MessageReceiver;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.transport.nameable.AbstractInboundEndpointNameableConnectorTestCase;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPListParseEngine;
import org.apache.commons.pool.ObjectPool;
import org.junit.Test;

/**
 * Test configuration of FTP connector. It's all done in code, no configuration files
 * are used and Mule is not started.
 */
public class FTPConnectorTestCase extends AbstractInboundEndpointNameableConnectorTestCase
{
    static final long POLLING_FREQUENCY = 1234;
    static final long POLLING_FREQUENCY_OVERRIDE = 4321;
    static final String TEST_ENDPOINT_URI = "ftp://foo:bar@example.com";
    static final String VALID_MESSAGE = "This is a valid FTP message";
    static final String RESOURCE_PATH = "somePath";
    
    protected static FTPClient client = mock(FTPClient.class);

    @Override
    public Connector createConnector() throws Exception
    {
        return internalGetConnector(false);
    }

    @Override
    public Object getValidMessage() throws Exception
    {
        return VALID_MESSAGE.getBytes();
    }

    @Override
    public String getTestEndpointURI()
    {
        return TEST_ENDPOINT_URI;
    }

    /**
     * Test polling frequency set on a connector.
     */
    @Test
    public void testConnectorPollingFrequency() throws Exception
    {
        InboundEndpoint endpoint = getTestInboundEndpoint("mock");
        Service service = getTestService("apple", Apple.class);
        FtpConnector connector = (FtpConnector)getConnector();
        MessageReceiver receiver = connector.createReceiver(service, endpoint);
        assertEquals("Connector's polling frequency must not be ignored.", POLLING_FREQUENCY,
            ((FtpMessageReceiver)receiver).getFrequency());
    }

    /**
     * Test polling frequency overridden at an endpoint level.
     */
    @Test
    public void testPollingFrequencyEndpointOverride() throws Exception
    {
        Map<Object, Object> props = new HashMap<Object, Object>();
        // Endpoint wants String-typed properties
        props.put(FtpConnector.PROPERTY_POLLING_FREQUENCY, String.valueOf(POLLING_FREQUENCY_OVERRIDE));

        InboundEndpoint endpoint = getTestInboundEndpoint("mock", null, null, null, props, null);

        Service service = getTestService("apple", Apple.class);
        FtpConnector connector = (FtpConnector)getConnector();
        MessageReceiver receiver = connector.createReceiver(service, endpoint);
        assertEquals("Polling frequency endpoint override must not be ignored.", POLLING_FREQUENCY_OVERRIDE,
            ((FtpMessageReceiver)receiver).getFrequency());
    }

    /**
     * Test setting a connection factory on a ftp endpoint.
     * @throws Exception
     */
    @Test
    public void testCustomFtpConnectionFactory() throws Exception
    {
        final ImmutableEndpoint endpoint = muleContext.getEndpointFactory()
            .getOutboundEndpoint("ftp://test:test@example.com");
        FtpConnector connector = createFtpConnector(endpoint);

        // no validate call for simplicity
        connector.setValidateConnections(false);

        ObjectPool pool = connector.getFtpPool(endpoint);
        Object obj = pool.borrowObject();
        assertEquals("Custom FTP connection factory has been ignored.", client, obj);
    }

    @Test
    public void isFile() throws Exception
    {
        FTPFile file = mock(FTPFile.class);
        FTPFile[] files = {file};
        when(file.isFile()).thenReturn(true);
        when(file.getName()).thenReturn(RESOURCE_PATH);
        FtpConnector connector = (FtpConnector) getConnector();

        assertThat(connector.isFile(mockEndpoint(), mockClient(files)), is(true));
    }

    @Test
    public void isDirectory() throws Exception
    {
        FTPFile file = mock(FTPFile.class);
        FTPFile secondFile = mock(FTPFile.class);
        FTPFile[] files = {file, secondFile};
        when(file.isFile()).thenReturn(true);
        FtpConnector connector = (FtpConnector) getConnector();

        assertThat(connector.isFile(mockEndpoint(), mockClient(files)), is(false));
    }

    @Test
    public void isDirectoryWithOnlyAFile() throws Exception
    {
        FTPFile file = mock(FTPFile.class);
        FTPFile[] files = {file};
        when(file.isFile()).thenReturn(true);
        when(file.getName()).thenReturn("someFile");
        FtpConnector connector = (FtpConnector) getConnector();

        assertThat(connector.isFile(mockEndpoint(), mockClient(files)), is(false));
    }

    private ImmutableEndpoint mockEndpoint ()
    {
        ImmutableEndpoint endpoint = mock(ImmutableEndpoint.class, RETURNS_DEEP_STUBS);
        when(endpoint.getEndpointURI().getPath()).thenReturn(RESOURCE_PATH);
        return endpoint;
    }

    private FTPClient mockClient (FTPFile[] filesToReturn) throws Exception
    {
        FTPListParseEngine engine = mock(FTPListParseEngine.class);
        when(engine.getNext(2)).thenReturn(filesToReturn);

        FTPClient client = mock(FTPClient.class);
        when(client.initiateListParsing(RESOURCE_PATH)).thenReturn(engine);
        return client;
    }

    /**
     * Workaround. The super getConnector() call will init the connector,
     * but for some tests we want more config steps before the initialisation.
     */
    protected FtpConnector internalGetConnector(boolean initialiseConnector) throws Exception
    {
        FtpConnector connector = new FtpConnector(muleContext);
        connector.setName("testFTP");
        connector.setPollingFrequency(POLLING_FREQUENCY);

        if (initialiseConnector)
        {
            connector.initialise();
        }

        return connector;
    }
    
    @Test
    public void testClientReleaseOnExceptionDuringCreation() throws Exception
    {
        final ImmutableEndpoint endpoint = muleContext.getEndpointFactory()
                                                      .getOutboundEndpoint("ftp://test:test@example.com");
        FtpConnector connector = createFtpConnector(endpoint);


        // This is done so that an exception is raised after client creation
        doThrow(Exception.class).when(client).setDataTimeout(anyInt());
        
        try
        {
            connector.createFtpClient(endpoint);
        }
        catch (Exception e)
        {

        }
        
        verify(client).disconnect();

    }

    private FtpConnector createFtpConnector(final ImmutableEndpoint endpoint)
    {
        final EndpointURI endpointURI = endpoint.getEndpointURI();

        FtpConnectionFactory testFactory = new TestFtpConnectionFactory(endpointURI);
        FtpConnector connector = (FtpConnector) getConnector();

        connector.setConnectionFactoryClass(testFactory.getClass().getName());
        // no validate call for simplicity
        connector.setValidateConnections(false);

        return connector;
    }
    
    public static final class TestFtpConnectionFactory extends FtpConnectionFactory {

        public TestFtpConnectionFactory(EndpointURI uri)
        {
            super(uri);
        }

        @Override
        public Object makeObject() throws Exception
        {
            return client;
        }

        @Override
        public void activateObject(final Object obj) throws Exception
        {
            // empty no-op, do not call super
        }
        
        @Override
        public void passivateObject(Object obj) throws Exception
        {
            // For testing purposes, on passivate implies a disconnection
            ((FTPClient) obj).disconnect();
        }

    }

}
