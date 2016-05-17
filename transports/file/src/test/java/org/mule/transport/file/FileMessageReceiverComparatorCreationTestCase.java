/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.file;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.construct.Flow;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Comparator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@SmallTest
public class FileMessageReceiverComparatorCreationTestCase extends AbstractMuleContextTestCase
{

    @Rule
    public TemporaryFolder readFolder = new TemporaryFolder();

    @Test
    public void usesExecutionClassLoader() throws Exception
    {
        ClassLoader classLoader = spy(new URLClassLoader(new URL[0], muleContext.getExecutionClassLoader()));
        muleContext.setExecutionClassLoader(classLoader);

        InboundEndpoint endpoint = createEndpoint();

        FileMessageReceiver receiver = new FileMessageReceiver(endpoint.getConnector(), mock(Flow.class), endpoint,
                                                               readFolder.getRoot().getAbsolutePath(), null, null, RECEIVE_TIMEOUT);

        receiver.connect();

        receiver.poll();

        verify(classLoader, timeout(RECEIVE_TIMEOUT)).loadClass(TestFileComparator.class.getName());
    }

    private InboundEndpoint createEndpoint() throws Exception
    {
        InboundEndpoint inboundEndpoint = muleContext.getEndpointFactory().getInboundEndpoint("file://./simple");
        inboundEndpoint.getProperties().put(FileMessageReceiver.COMPARATOR_CLASS_NAME_PROPERTY, TestFileComparator.class.getName());

        return inboundEndpoint;
    }

    public static class TestFileComparator implements Comparator
    {

        @Override
        public int compare(Object file1, Object file2)
        {
            return 0;
        }
    }

}
