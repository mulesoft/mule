package org.mule.transport.sftp;/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transport.sftp.notification.SftpNotifier;

import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SftpReceiverRequesterUtilTestCase extends AbstractMuleContextTestCase
{

    private static final String FILE_TXT = "file.txt";

    @Rule
    public ExpectedException expectedException = none();

    @Mock
    private ImmutableEndpoint mockEndpoint;
    @Mock
    private SftpConnector mockConnector;
    @Mock
    private SftpNotifier mockNotifier;
    @Mock
    private SftpClient mockSftpClient;

    @Test
    public void clientIsReleasedWhenRetrievingFails() throws Exception
    {
        when(mockEndpoint.getConnector()).thenReturn(mockConnector);
        when(mockConnector.createSftpClient(mockEndpoint, mockNotifier)).thenReturn(mockSftpClient);
        when(mockSftpClient.retrieveFile(FILE_TXT)).thenThrow(IOException.class);
        SftpReceiverRequesterUtil requesterUtil = new SftpReceiverRequesterUtil(mockEndpoint);

        expectedException.expect(IOException.class);
        try
        {
            requesterUtil.retrieveFile(FILE_TXT, mockNotifier);
        }
        finally
        {
            verify(mockConnector).releaseClient(mockEndpoint, mockSftpClient);
        }
    }
}
