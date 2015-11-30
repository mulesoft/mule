/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.context.WorkManager;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.retry.RetryCallback;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.transport.MuleAbstractTransportMessageHandlerTestCase.MethodInvocation.MethodPart;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

import edu.umd.cs.mtc.MultithreadedTestCase;
import edu.umd.cs.mtc.TestFramework;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This test tests class {@link AbstractTransportMessageHandler} but its name starts with "Mule"
 * because there is an exclusion rule in parent pom for test classes that have their
 * names starting with "Abstract".
 */
public class MuleAbstractTransportMessageHandlerTestCase
{
    /**
     * The logger used for this class
     */
    static Log log = LogFactory.getLog(MuleAbstractTransportMessageHandlerTestCase.class);

    @Test
    public void testStartRethrowsMuleExceptionCorrectly() throws Exception
    {
        final MuleException someMuleException = mock(MuleException.class);
        AbstractTransportMessageHandler connectable = new AbstractTransportMessageHandler(createDummyEndpoint())
        {
            @Override
            protected void doStart() throws MuleException
            {
                throw someMuleException;
            }

            @Override
            protected WorkManager getWorkManager()
            {
                return null;
            }

            @Override
            protected ConnectableLifecycleManager createLifecycleManager()
            {
                return new ConnectableLifecycleManager("test", this);
            }
        };
        connectable.initialise();
        connectable.connect();
        try
        {
            connectable.start();
            fail("Should have thrown a " + MuleException.class.getSimpleName());
        }
        catch (MuleException caughtException)
        {
            assertExceptionIsInCaughtException(someMuleException, caughtException);
        }
    }

    /**
     * This test tests that start method is thread safe and that
     * {@link AbstractTransportMessageHandler#doStart()} is always called making sure that the
     * connectable is {@link AbstractTransportMessageHandler#isConnected() connected}.
     * <p>
     * To make multithreaded test easier it uses a library called <a
     * href="http://www.cs.umd.edu/projects/PL/multithreadedtc/overview.html"
     * >MultithreadedTC</a>. You will perhaps go and read that link if you want to
     * understand how this test works.
     * 
     * @throws Throwable
     */
    @Ignore
    @Test
    public void testStartIsThreadSafe() throws Throwable
    {
        TestFramework.runOnce(new AbstractConnectableMultithreaded());
    }

    /**
     * This inner class will do the work of
     * {@link MuleAbstractTransportMessageHandlerTestCase#testStartIsThreadSafe()
     * testStartIsThreadSafe()}.
     */
    @Ignore
    class AbstractConnectableMultithreaded extends MultithreadedTestCase
    {
        private volatile AbstractConnectableForTest connectable;

        /**
         * This is called before any of the thread* methods are called. Pretty much
         * like a {@link Before} method in JUnit 4.
         */
        @Override
        public void initialize()
        {
            try
            {
                ImmutableEndpoint endpoint = createDummyEndpoint();

                connectable = new AbstractConnectableForTest(endpoint);
                connectable.initialise();
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        /**
         * This will be called by the <a
         * href="http://www.cs.umd.edu/projects/PL/multithreadedtc/overview.html"
         * >MultithreadedTC</a> framework with an independent thread.
         * 
         * @throws Exception on unexpected error.
         */
        public void thread1() throws Exception
        {
            connectable.start();
        }

        /**
         * @see #thread1()
         * @throws Exception on unexpected error
         */
        public void thread2() throws Exception
        {
            // waiting for first tick means that thread1 will work on tick 0 and
            // after it blocks then this one will start.
            waitForTick(1);
            connectable.start();
        }

        /**
         * This is called after all the thread* methods finish. Pretty much like an
         * {@link After} method in JUnit 4.
         */
        @Override
        public void finish()
        {
            for (MethodInvocation methodInvocation : connectable.methodInvocations)
            {
                log.debug(methodInvocation);
            }

            int i = 0;
            assertEquals("doConnect", connectable.methodInvocations.get(i++).getMethodName());
            assertEquals("doConnect", connectable.methodInvocations.get(i++).getMethodName());
            assertEquals("doStart", connectable.methodInvocations.get(i++).getMethodName());
            assertEquals("doStart", connectable.methodInvocations.get(i++).getMethodName());
        }

        /**
         * This is a specific implementation of {@link AbstractTransportMessageHandler} for this
         * test that will only implement {@link #doConnect()} and {@link #doStart()}
         * methods.
         * <p>
         * The implementation of those methods simulate a long {@link #doConnect()}
         * execution that makes the other thread attempt to execute
         * {@link #doStart()}. They have also code that validates that
         * {@link #doStart()} is never called before {@link #doConnect()} has
         * finished.
         */
        @Ignore
        class AbstractConnectableForTest extends AbstractTransportMessageHandler
        {
            private final AtomicBoolean doConnectCalled = new AtomicBoolean();
            private final AtomicBoolean doStartCalled = new AtomicBoolean();

            /**
             * This list will hold reference to each of the calls to the methods
             * {@link #doConnect()} and {@link #doStart()}. We use a {@link Vector}
             * because it will be accessed by different threads.
             */
            List<MethodInvocation> methodInvocations = new Vector<MethodInvocation>();

            public AbstractConnectableForTest(ImmutableEndpoint endpoint)
            {
                super(endpoint);
            }

            @Override
            protected WorkManager getWorkManager()
            {
                return null;
            }

            @Override
            protected ConnectableLifecycleManager createLifecycleManager()
            {
                return new ConnectableLifecycleManager("test", this);
            }

            @Override
            protected void doConnect() throws Exception
            {
                methodInvocations.add(new MethodInvocation(Thread.currentThread(), "doConnect",
                    MethodPart.BEGINNING));
                assertTrue(doConnectCalled.compareAndSet(false, true));
                assertFalse(doStartCalled.get());

                // This method is called by thread1 in the test and waiting
                // for tick 2 will make thread 2 execute during tick 1, attempting
                // to call doStart(). Thread 2 should be then blocked waiting on a
                // monitor until AbstractConnectable.connected is true, which
                // triggers tick 2 and thread 1 will resume its execution,
                // finishing the exection of this method.
                waitForTick(2);

                assertFalse(doStartCalled.get());

                methodInvocations.add(new MethodInvocation(Thread.currentThread(), "doConnect",
                    MethodPart.END));
            }

            @Override
            protected void doStart() throws MuleException
            {
                methodInvocations.add(new MethodInvocation(Thread.currentThread(), "doStart",
                    MethodPart.BEGINNING));
                assertTrue(doStartCalled.compareAndSet(false, true));
                assertTrue(doConnectCalled.get());
                methodInvocations.add(new MethodInvocation(Thread.currentThread(), "doStart", MethodPart.END));
            }

        }
    }

    /**
     * This class just represent a method invocation that allow keeping track of the
     * order in which calls are made by different threads.
     */
    @org.junit.Ignore
    static class MethodInvocation
    {
        @Ignore
        static enum MethodPart
        {
            BEGINNING, END
        }

        private final Thread thread;
        private final String methodName;
        private final MethodPart methodPart;

        public MethodInvocation(Thread thread, String methodName, MethodPart methodPart)
        {
            this.thread = thread;
            this.methodName = methodName;
            this.methodPart = methodPart;
        }

        public Thread getThread()
        {
            return thread;
        }

        public String getMethodName()
        {
            return methodName;
        }

        public MethodPart getMethodPart()
        {
            return methodPart;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
            {
                return true;
            }
            else if (obj == null || obj.getClass() != this.getClass())
            {
                return false;
            }
            else
            {
                MethodInvocation other = (MethodInvocation) obj;
                return new EqualsBuilder().append(this.thread, other.thread).append(this.methodName,
                    other.methodName).append(this.methodPart, other.methodPart).isEquals();
            }
        }

        @Override
        public int hashCode()
        {
            return new HashCodeBuilder().append(this.thread)
                .append(this.methodName)
                .append(this.methodPart)
                .toHashCode();
        }

        @Override
        public String toString()
        {
            return "Thread " + this.thread + " passing through " + this.methodName + "() at the "
                   + this.methodPart;
        }
    }

    private void assertExceptionIsInCaughtException(MuleException someMuleException, MuleException caughtException)
    {
        boolean found = false;
        Throwable candidate = caughtException;
        while (candidate != null)
        {
            if (someMuleException.equals(candidate))
            {
                found = true;
                break;
            }
            
            candidate = candidate.getCause();
        }
        
        if (found == false)
        {
            fail();
        }
    }
    
    /**
     * @return an dummy implementation of {@link ImmutableEndpoint} suitable for this
     *         test.
     * @throws Exception
     */
    ImmutableEndpoint createDummyEndpoint() throws Exception
    {
        ImmutableEndpoint endpoint = mock(ImmutableEndpoint.class);
        MuleContext muleContext = mock(MuleContext.class);
        when(endpoint.getEndpointURI()).thenReturn(new MuleEndpointURI("http://dummy.endpoint/", muleContext));
        AbstractConnector connector = mock(AbstractConnector.class);
        when(endpoint.getConnector()).thenReturn(connector);

        RetryPolicyTemplate retryPolicyTemplate = mock(RetryPolicyTemplate.class);
        when(endpoint.getRetryPolicyTemplate()).thenReturn(retryPolicyTemplate);
        when(retryPolicyTemplate.execute(any(RetryCallback.class), any(WorkManager.class))).thenAnswer(
            new Answer<Object>()
            {
                public Object answer(InvocationOnMock invocation) throws Throwable
                {
                    RetryCallback retryCallback = (RetryCallback) invocation.getArguments()[0];
                    retryCallback.doWork(null);
                    return null;
                }
            });

        return endpoint;
    }
}
