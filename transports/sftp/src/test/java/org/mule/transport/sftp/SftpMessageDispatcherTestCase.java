/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.transport.file.FilenameParser;
import org.mule.transport.sftp.notification.SftpNotifier;
import org.mule.util.IOUtils;

import java.io.InputStream;
import java.io.StringWriter;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class SftpMessageDispatcherTestCase extends AbstractMuleTestCase
{

    private final String PROPERTY_OUTPUT_PATTERN = "outPattern";
    private final String PROPERTY_FILENAME = "outFilename";
    private String payload = "HelloWorld!";
    private EndpointURI endpointUri = mock(EndpointURI.class);
    private OutboundEndpoint outboundEndpoint = mock(OutboundEndpoint.class);
    private MuleEvent muleEvent = mock(MuleEvent.class);
    private MuleMessage muleMessage = mock(MuleMessage.class);
    private SftpConnector sftpConnector = mock(SftpConnector.class);
    private SftpClient sftpClient = mock(SftpClient.class);
    private ArgumentCaptor<String> transferFilenameCaptor = ArgumentCaptor.forClass(String.class);
    private ArgumentCaptor<InputStream> inputStreamCaptor = ArgumentCaptor.forClass(InputStream.class);
    private ArgumentCaptor<SftpClient.WriteMode> writeModeCaptor = ArgumentCaptor.forClass(SftpClient.WriteMode.class);
    private FilenameParser filenameParser = mock(FilenameParser.class);

    @Before
    public void initializeMocks() throws Exception
    {
        when(sftpConnector.createSftpClient(eq(outboundEndpoint), any(SftpNotifier.class))).thenReturn(sftpClient);
        when(sftpConnector.getFilenameParser()).thenReturn(filenameParser);
        when(filenameParser.getFilename(any(MuleMessage.class), anyString())).thenAnswer(
                new Answer<String>()
                {
                    @Override
                    public String answer(InvocationOnMock invocation) throws Throwable
                    {
                        Object[] args = invocation.getArguments();
                        return (String) args[1];    // outPattern
                    }
                }
        );
        when(outboundEndpoint.getConnector()).thenReturn(sftpConnector);
        when(outboundEndpoint.getEndpointURI()).thenReturn(endpointUri);
        when(muleEvent.getMessage()).thenReturn(muleMessage);
        when(muleMessage.getPayload()).thenReturn(payload);
        when(muleMessage.findPropertyInAnyScope(SftpConnector.PROPERTY_FILENAME, null)).thenReturn(PROPERTY_FILENAME);
        when(sftpClient.duplicateHandling(anyString(), anyString(), anyString())).thenAnswer(
                new Answer<String>()
                {
                    @Override
                    public String answer(InvocationOnMock invocation) throws Throwable
                    {
                        Object[] args = invocation.getArguments();
                        return (String) args[1];    // filename
                    }
                }
        );
    }

    @Test
    public void outputFilenameAndPayloadWhenOutPatternSet() throws Exception
    {
        when(outboundEndpoint.getProperty(SftpConnector.PROPERTY_OUTPUT_PATTERN)).thenReturn(PROPERTY_OUTPUT_PATTERN);
        SftpMessageDispatcher sftpMessageDispatcher = new SftpMessageDispatcher(outboundEndpoint);

        sftpMessageDispatcher.doDispatch(muleEvent);
        verify(sftpClient).storeFile(transferFilenameCaptor.capture(), inputStreamCaptor.capture());
        assertEquals("Output filename was not set correctly.", PROPERTY_OUTPUT_PATTERN,
                     transferFilenameCaptor.getValue());

        InputStream inputStream = inputStreamCaptor.getValue();
        StringWriter stringWriter = new StringWriter();
        IOUtils.copy(inputStream, stringWriter, "UTF-8");
        assertEquals(payload, stringWriter.toString());
    }

    @Test
    public void outputFilenameAndPayloadWhenOutPatternNotSet() throws Exception
    {
        when(outboundEndpoint.getProperty(SftpConnector.PROPERTY_OUTPUT_PATTERN)).thenReturn(null);
        SftpMessageDispatcher sftpMessageDispatcher = new SftpMessageDispatcher(outboundEndpoint);

        sftpMessageDispatcher.doDispatch(muleEvent);
        verify(sftpClient).storeFile(transferFilenameCaptor.capture(), inputStreamCaptor.capture());
        assertEquals("Output filename was not set correctly.", PROPERTY_FILENAME,
                     transferFilenameCaptor.getValue());

        InputStream inputStream = inputStreamCaptor.getValue();
        StringWriter stringWriter = new StringWriter();
        IOUtils.copy(inputStream, stringWriter, "UTF-8");
        assertEquals(payload, stringWriter.toString());
    }
    
    @Test
    public void appendFile() throws Exception
    {
        when(sftpConnector.getDuplicateHandling()).thenReturn("append");
        SftpMessageDispatcher sftpMessageDispatcher = new SftpMessageDispatcher(outboundEndpoint);
        sftpMessageDispatcher.doDispatch(muleEvent);
        verify(sftpClient).storeFile(transferFilenameCaptor.capture(), inputStreamCaptor.capture(), writeModeCaptor.capture());
        InputStream inputStream = inputStreamCaptor.getValue();
        StringWriter stringWriter = new StringWriter();
        IOUtils.copy(inputStream, stringWriter, "UTF-8");
        assertEquals(payload, stringWriter.toString());
    }

    @Test
    public void overwriteFile() throws Exception
    {
        SftpMessageDispatcher sftpMessageDispatcher = new SftpMessageDispatcher(outboundEndpoint);
        sftpMessageDispatcher.doDispatch(muleEvent);
        verify(sftpClient).storeFile(transferFilenameCaptor.capture(), inputStreamCaptor.capture());
        InputStream inputStream = inputStreamCaptor.getValue();
        StringWriter stringWriter = new StringWriter();
        IOUtils.copy(inputStream, stringWriter, "UTF-8");
        assertEquals(payload, stringWriter.toString());        
    }
    
}
