/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request.grizzly;

import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.context.WorkManager;
import org.mule.api.context.WorkManagerSource;
import org.mule.module.http.internal.HttpMessageLogger;
import org.mule.module.http.internal.listener.grizzly.BaseResponseCompletionHandler;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import com.ning.http.client.AsyncCompletionHandlerBase;
import com.ning.http.client.AsyncHandler;
import com.ning.http.client.Response;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.GrizzlyFuture;
import org.glassfish.grizzly.IOEvent;
import org.glassfish.grizzly.Transport;
import org.glassfish.grizzly.attributes.AttributeHolder;
import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.http.HttpCodecFilter;
import org.glassfish.grizzly.impl.FutureImpl;
import org.glassfish.grizzly.impl.SafeFutureImpl;
import org.hamcrest.CoreMatchers;
import org.hamcrest.core.IsNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class FlowWorkManagerIOStrategyTestCase extends AbstractMuleTestCase
{

    FlowWorkManagerIOStrategy ioStrategy = FlowWorkManagerIOStrategy.getInstance();

    @Mock
    Connection connection;
    @Mock
    AsyncHandler asyncHandler;
    @Mock
    ExecutorService grizzlyWorkerThreadPool;
    @Mock
    WorkManager flowWorkManager;

    @Before
    public void setup()
    {
        AttributeHolder attributeHolder = mock(AttributeHolder.class);
        when(connection.getAttributes()).thenReturn(attributeHolder);
        Transport transport = mock(Transport.class);
        when(connection.getTransport()).thenReturn(transport);
        when(transport.getWorkerThreadPool()).thenReturn(grizzlyWorkerThreadPool);
    }


    @Test
    public void flowWorkManagerUsedForReadIOEvent() throws IOException
    {
        new TestFlowWorkManagerIOStrategy(flowWorkManager).executeIoEvent(connection, IOEvent.READ);
        verify(flowWorkManager, times(1)).execute(any(Runnable.class));
    }

    @Test
    public void grizzlyWorkThreadPoolUsedWhenNoWorkManagerForReadIOEvent() throws IOException
    {
        new TestFlowWorkManagerIOStrategy(null).executeIoEvent(connection, IOEvent.READ);
        verify(grizzlyWorkerThreadPool, times(1)).execute(any(Runnable.class));
    }

    @Test
    public void selectorUsedForConnectIOEvent() throws IOException
    {
        new TestFlowWorkManagerIOStrategy(flowWorkManager).executeIoEvent(connection, IOEvent.CLIENT_CONNECTED);
        verify(flowWorkManager, never()).execute(any(Runnable.class));
        verify(grizzlyWorkerThreadPool, never()).execute(any(Runnable.class));
    }

    @Test
    public void selectorUsedWhenNoWorkManagerForReadEvent() throws IOException
    {
        new TestFlowWorkManagerIOStrategy(null).executeIoEvent(connection, IOEvent.CLIENT_CONNECTED);
        verify(flowWorkManager, never()).execute(any(Runnable.class));
        verify(grizzlyWorkerThreadPool, never()).execute(any(Runnable.class));
    }


    class TestFlowWorkManagerIOStrategy extends FlowWorkManagerIOStrategy
    {

        private WorkManager workManager;

        TestFlowWorkManagerIOStrategy(WorkManager workManager)
        {
            this.workManager = workManager;
        }

        @Override
        protected WorkManager getWorkManager(Connection connection) throws MuleException
        {
            return workManager;
        }
    }

}