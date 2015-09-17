/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

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
    private ImmutableEndpoint endpoint;
    @Mock
    private SftpConnector connector;
    @Mock
    private SftpNotifier notifier;
    @Mock
    private SftpClient sftpClient;

    @Test
    public void clientIsReleasedWhenRetrievingFails() throws Exception
    {
        when(endpoint.getConnector()).thenReturn(connector);
        when(connector.createSftpClient(endpoint, notifier)).thenReturn(sftpClient);
        when(sftpClient.retrieveFile(FILE_TXT)).thenThrow(IOException.class);
        SftpReceiverRequesterUtil requesterUtil = new SftpReceiverRequesterUtil(endpoint);

        expectedException.expect(IOException.class);
        try
        {
            requesterUtil.retrieveFile(FILE_TXT, notifier);
        }
        finally
        {
            verify(connector).releaseClient(endpoint, sftpClient);
        }
    }
}
