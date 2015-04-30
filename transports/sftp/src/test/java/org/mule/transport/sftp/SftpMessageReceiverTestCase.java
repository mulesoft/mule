/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import org.junit.Before;
import org.junit.Test;
import org.mule.api.MuleContext;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.util.lock.LockProvider;
import org.mule.util.lock.MuleLockFactory;
import org.mule.util.lock.SingleServerLockProvider;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@SmallTest
public class SftpMessageReceiverTestCase extends AbstractMuleTestCase
{

    private static final String CONNECTOR_NAME = "connector-name";
    private static final String ENDPOINT_URI_PATH = "endpoint-uri-path";
    private static final String[] FILE_NAMES = new String[] {"some-file-1", "some-file-2"};
    private static final FileDescriptor[] FILES = new FileDescriptor[] { new FileDescriptor(FILE_NAMES[0], null), new FileDescriptor(FILE_NAMES[1], null) };

    private MuleLockFactory lockFactory;
    private LockProvider lockProvider;
    private TestSftpMessageReceiver receiver;

    @Before
    public void createMocks() throws Exception
    {
        MuleContext muleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS);
        SftpConnector sftpConnector = mock(SftpConnector.class, RETURNS_DEEP_STUBS);
        FlowConstruct flow = mock(FlowConstruct.class);
        InboundEndpoint endpoint = mock(InboundEndpoint.class);
        EndpointURI endpointURI = mock(EndpointURI.class);

        when(sftpConnector.getName()).thenReturn(CONNECTOR_NAME);
        when(sftpConnector.createSftpClient(endpoint).listFiles()).thenReturn(FILE_NAMES);
        when(sftpConnector.createSftpClient(endpoint).getFileDescriptors()).thenReturn(FILES);
        when(endpoint.getMuleContext()).thenReturn(muleContext);
        when(endpoint.getEndpointURI()).thenReturn(endpointURI);
        when(endpoint.getConnector()).thenReturn(sftpConnector);
        when(endpointURI.getPath()).thenReturn(ENDPOINT_URI_PATH);

        lockProvider = spy(new SingleServerLockProvider());

        lockFactory = new MuleLockFactory();
        lockFactory.setMuleContext(muleContext);
        lockFactory.setLockProvider(lockProvider);
        lockFactory.initialise();

        when(muleContext.getLockFactory()).thenReturn(lockFactory);

        receiver = new TestSftpMessageReceiver(sftpConnector, flow, endpoint);
        receiver.doInitialise();
    }

    @Test
    public void lockIsReleased() throws Exception
    {
        // Simulate two poll cycles
        receiver.poll();
        receiver.poll();

        // Each time poll is called a new lock should be created for each file, and released after the file is processed.
        // If the first created lock is not released the second call to poll won't create a new lock but use the previous one.
        for (String fileName : FILE_NAMES)
        {
            verify(lockProvider, times(2)).createLock(receiver.createLockId(fileName));
        }
    }

    @Test
    public void lockContainsEndpointUri() throws Exception
    {
        for (String fileName : FILE_NAMES)
        {
            assertThat(receiver.createLockId(fileName), is(createTestLockId(fileName)));
        }
    }

    private String createTestLockId(String fileName)
    {
        return String.format("%s-%s-%s", CONNECTOR_NAME, ENDPOINT_URI_PATH, fileName);
    }

    private class TestSftpMessageReceiver extends SftpMessageReceiver
    {

        public TestSftpMessageReceiver(SftpConnector connector, FlowConstruct flow, InboundEndpoint endpoint) throws CreateException
        {
            super(connector, flow, endpoint);
        }

        @Override
        protected void routeFile(final String path) throws Exception
        {
            ensureLockIsLocked(path);
        }

        private void ensureLockIsLocked(final String path) throws InterruptedException
        {
            final AtomicBoolean lockObtained = new AtomicBoolean(true);
            Thread otherThread = new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    lockObtained.set(lockFactory.createLock(receiver.createLockId(path)).tryLock());
                }
            });
            otherThread.start();
            otherThread.join(1000);
            assertThat(lockObtained.get(), is(false));
        }
    }
}
