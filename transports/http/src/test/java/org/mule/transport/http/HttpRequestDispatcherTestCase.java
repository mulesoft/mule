/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.api.context.WorkManager;
import org.mule.api.exception.SystemExceptionHandler;
import org.mule.api.retry.RetryCallback;
import org.mule.api.retry.RetryContext;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.tck.size.SmallTest;
import org.mule.transport.AbstractConnector;
import org.mule.transport.ConnectorLifecycleManager;
import org.mule.util.concurrent.Latch;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class HttpRequestDispatcherTestCase extends AbstractMuleTestCase
{

    public static final int WAIT_TIME = 5000;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private HttpConnector mockHttpConnector;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private WorkManager mockWorkManager;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ServerSocket mockServerSocket;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RetryPolicyTemplate mockRetryTemplate;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ConnectorLifecycleManager mockConnectorLifecycleManager;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RetryContext mockRetryContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SystemExceptionHandler mockExceptionListener;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ExecutorService mockExecutor;


    @Test(expected = IllegalArgumentException.class)
    public void createHttpSocketDispatcherWithNullConnector()
    {
        new HttpRequestDispatcher(null, mockRetryTemplate, mockServerSocket, mockWorkManager);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createHttpSocketDispatcherWithNullRetryPolicyTemplate()
    {
        new HttpRequestDispatcher(mockHttpConnector, null, mockServerSocket, mockWorkManager);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createHttpSocketDispatcherWithNullServerSocket()
    {
        new HttpRequestDispatcher(mockHttpConnector, mockRetryTemplate, null, mockWorkManager);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createHttpSocketDispatcherWithNullWorkManager()
    {
        new HttpRequestDispatcher(mockHttpConnector, mockRetryTemplate, mockServerSocket, null);
    }

    @Test
    public void closeServerSocketWhenDisconnect() throws IOException
    {
        HttpRequestDispatcher httpRequestDispatcher = new HttpRequestDispatcher(mockHttpConnector, mockRetryTemplate, mockServerSocket, mockWorkManager);
        httpRequestDispatcher.disconnect();
        verify(mockServerSocket, times(1)).close();
    }

    @Test
    public void whenFailureCallSystemExceptionHandler() throws Exception
    {
        final HttpRequestDispatcher httpRequestDispatcher = new HttpRequestDispatcher(mockHttpConnector, mockRetryTemplate, mockServerSocket, mockWorkManager);
        final Latch acceptCalledLath = new Latch();
        sustituteLifecycleManager();
        when(mockConnectorLifecycleManager.getState().isStarted()).thenReturn(true);
        when(mockRetryTemplate.execute(any(RetryCallback.class), any(WorkManager.class))).thenAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                acceptCalledLath.release();
                throw new Exception();
            }
        });
        when(mockHttpConnector.getMuleContext().getExceptionListener()).thenReturn(mockExceptionListener);
        Thread dispatcherThread = createDispatcherThread(httpRequestDispatcher);
        try
        {
            dispatcherThread.start();
            if (!acceptCalledLath.await(WAIT_TIME, TimeUnit.MILLISECONDS))
            {
                fail("retry template should be executed");
            }

            Prober prober = new PollingProber(100, 1);
            prober.check(new Probe()
            {
                public boolean isSatisfied()
                {
                    try
                    {
                        verify(mockExceptionListener, Mockito.atLeast(1)).handleException(Mockito.isA(Exception.class));
                        return true;
                    }
                    catch (AssertionError e)
                    {
                        return false;
                    }
                }

                public String describeFailure()
                {
                    return "Exception listener was not invoked";
                }
            });
        }
        finally
        {
            httpRequestDispatcher.disconnect();
        }
    }

    @Test
    public void whenConnectorIsNotStartedDoNotAcceptSockets() throws Exception
    {
        HttpRequestDispatcher httpRequestDispatcher = new HttpRequestDispatcher(mockHttpConnector, mockRetryTemplate, mockServerSocket, mockWorkManager);
        sustituteLifecycleManager();
        when(mockConnectorLifecycleManager.getState().isStarted()).thenReturn(false);
        when(mockHttpConnector.isStarted()).thenReturn(false);
        Thread dispatcherThread = createDispatcherThread(httpRequestDispatcher);
        try
        {
            dispatcherThread.start();
            verify(mockRetryTemplate, times(0)).execute(any(RetryCallback.class),any(WorkManager.class));
        }
        finally
        {
            httpRequestDispatcher.disconnect();
        }
    }


    @Test
    public void whenSocketAcceptedExecuteWork() throws Exception
    {
        final HttpRequestDispatcher httpRequestDispatcher = new HttpRequestDispatcher(mockHttpConnector, mockRetryTemplate, mockServerSocket, mockWorkManager);
        httpRequestDispatcher.requestHandOffExecutor = mockExecutor;
        final Latch acceptCalledLath = new Latch();
        sustituteLifecycleManager();
        when(mockConnectorLifecycleManager.getState().isStarted()).thenReturn(true);
        when(mockRetryTemplate.execute(any(RetryCallback.class), any(WorkManager.class))).thenAnswer(new Answer<RetryContext>()
        {
            @Override
            public RetryContext answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                ((RetryCallback) invocationOnMock.getArguments()[0]).doWork(mockRetryContext);
                return null;
            }
        });
        Mockito.doAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                acceptCalledLath.release();
                return null;
            }
        }).when(mockExecutor).execute(any(HttpRequestDispatcherWork.class));
        Thread dispatcherThread = createDispatcherThread(httpRequestDispatcher);
        dispatcherThread.start();
        try
        {
            if (!acceptCalledLath.await(500, TimeUnit.MILLISECONDS))
            {
                fail("Work should have been scheduled");
            }
        }
        finally
        {
            httpRequestDispatcher.disconnect();
        }

    }

    @Test
    public void shutsDownRequestHandOffExecutorWhenDisconnected()
    {
        HttpRequestDispatcher httpRequestDispatcher = new HttpRequestDispatcher(mockHttpConnector, mockRetryTemplate, mockServerSocket, mockWorkManager);
        httpRequestDispatcher.requestHandOffExecutor = mockExecutor;

        httpRequestDispatcher.disconnect();
        verify(mockExecutor).shutdown();
    }

    private void sustituteLifecycleManager() throws NoSuchFieldException, IllegalAccessException
    {
        Field filed = AbstractConnector.class.getDeclaredField("lifecycleManager");
        filed.setAccessible(true);
        filed.set(mockHttpConnector, mockConnectorLifecycleManager);
    }

    private Thread createDispatcherThread(final HttpRequestDispatcher httpRequestDispatcher)
    {
        Thread requestDispatcherThread = new Thread()
        {
            @Override
            public void run()
            {
                httpRequestDispatcher.run();
            }
        };
        requestDispatcherThread.setDaemon(true);
        return requestDispatcherThread;
    }
}
