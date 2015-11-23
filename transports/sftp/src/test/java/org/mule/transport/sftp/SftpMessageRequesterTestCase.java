/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@SmallTest
public class SftpMessageRequesterTestCase extends AbstractMuleTestCase
{

    private static final String CONNECTOR_NAME = "connector-name";
    private static final String ENDPOINT_URI_PATH = "endpoint-uri-path";
    private static final String[] FILE_NAMES = new String[] {"some-file-1", "some-file-2"};

    private MuleContext muleContext;
    private SftpConnector sftpConnector;
    private SftpMessageRequester requester;

    @Before
    public void createMocks() throws Exception
    {
        FileUtils.deleteDirectory(new File("archive"));

        muleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS);
        sftpConnector = mock(SftpConnector.class, RETURNS_DEEP_STUBS);
        InboundEndpoint endpoint = mock(InboundEndpoint.class);
        EndpointURI endpointURI = mock(EndpointURI.class);

        when(sftpConnector.getName()).thenReturn(CONNECTOR_NAME);
        when(sftpConnector.createSftpClient(endpoint).listFiles()).thenReturn(FILE_NAMES);
        when(sftpConnector.getArchiveDir()).thenReturn("archive");
        when(sftpConnector.getArchiveTempReceivingDir()).thenReturn("receiving");
        when(sftpConnector.getArchiveTempSendingDir()).thenReturn("sending");
        when(endpoint.getMuleContext()).thenReturn(muleContext);
        when(endpoint.getEndpointURI()).thenReturn(endpointURI);
        when(endpoint.getConnector()).thenReturn(sftpConnector);
        when(endpointURI.getPath()).thenReturn(ENDPOINT_URI_PATH);

        requester = new SftpMessageRequester(endpoint);
        requester.initialise();
    }

    @Test
    public void postProcessInvoked() throws Exception
    {
        when(sftpConnector.getMuleMessageFactory().create(anyObject(), anyString(), eq(muleContext))).thenAnswer(new Answer<MuleMessage>()
        {
            @Override
            public MuleMessage answer(InvocationOnMock invocation) throws Throwable
            {
                SftpFileArchiveInputStream inputStream = null;
                if (invocation.getArguments()[0] != null)
                {
                    inputStream = (SftpFileArchiveInputStream) spy(invocation.getArguments()[0]);
                }
                return new DefaultMuleMessage(inputStream, muleContext);
            }
        });

        MuleMessage message = requester.request(30);

        SftpFileArchiveInputStream inputStream = (SftpFileArchiveInputStream) message.getPayload();
        verify(inputStream, never()).postProcess();
        inputStream.close();
        verify(inputStream).postProcess();
    }
}
