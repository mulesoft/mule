/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.api.context.WorkManager;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.size.SmallTest;

import java.util.Collections;
import java.util.List;

import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@SmallTest
public class TrackingWorkManagerTestCase extends AbstractMuleTestCase
{

    @Rule
    public SystemProperty waitMillis = new SystemProperty(TrackingWorkManager.MULE_WAIT_MILLIS, "0");

    private final WorkManager delegateWorkManager = mock(WorkManager.class);
    private final WorkTracker workTracker = mock(WorkTracker.class);
    private final WorkManagerHolder workManagerHolder = mock(WorkManagerHolder.class);
    private TrackingWorkManager trackingWorkManager;

    @Before
    public void setUp() throws Exception
    {
        trackingWorkManager = new TrackingWorkManager(workManagerHolder, 5000);
        trackingWorkManager.setWorkTracker(workTracker);
        when(workManagerHolder.getWorkManager()).thenReturn(delegateWorkManager);
    }

    @Test
    public void delegatesDoWork() throws WorkException
    {
        final Work work = mock(Work.class);
        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                work.run();
                return null;
            }
        }).when(delegateWorkManager).doWork(work);

        trackingWorkManager.doWork(work);

        verify(work).run();
    }

    @Test
    public void tracksWorkOnDoWorkDelegation() throws Exception
    {
        Work work = mock(Work.class);

        trackingWorkManager.doWork(work);

        assertWorkIsTracked(work);
    }

    @Test
    public void untracksWorkOnDoWorkException() throws Exception
    {
        Work work = mock(Work.class);
        doThrow(new WorkException()).when(delegateWorkManager).doWork(work);

        try
        {
            trackingWorkManager.doWork(work);
            expectedExceptionFail();
        }
        catch (Exception e)
        {
            // Expected
        }

        assertWorkIsTracked(work);
    }

    @Test
    public void delegatesParameterizedDoWork() throws WorkException
    {
        Work work = mock(Work.class);
        int startTimeout = 10;
        ExecutionContext execContext = mock(ExecutionContext.class);
        WorkListener workListener = mock(WorkListener.class);

        trackingWorkManager.doWork(work, startTimeout, execContext, workListener);

        verify(delegateWorkManager).doWork(work, startTimeout, execContext, workListener);
    }

    @Test
    public void tracksWorkOnDoParameterizedWorkDelegation() throws Exception
    {
        Work work = mock(Work.class);
        int startTimeout = 10;
        ExecutionContext execContext = mock(ExecutionContext.class);
        WorkListener workListener = mock(WorkListener.class);

        trackingWorkManager.doWork(work, startTimeout, execContext, workListener);

        assertParameterizedWorkWasTracked(work, startTimeout, execContext, workListener);
    }

    @Test
    public void untracksWorkOnDoParameterizedWorkException() throws Exception
    {
        Work work = mock(Work.class);
        int startTimeout = 10;
        ExecutionContext execContext = mock(ExecutionContext.class);
        WorkListener workListener = mock(WorkListener.class);

        doThrow(new WorkException()).when(delegateWorkManager).doWork(work, startTimeout, execContext, workListener);

        try
        {
            trackingWorkManager.doWork(work, startTimeout, execContext, workListener);
            expectedExceptionFail();
        }
        catch (Exception e)
        {
            // Expected
        }

        assertParameterizedWorkWasTracked(work, startTimeout, execContext, workListener);
    }

    @Test
    public void startsWork() throws WorkException
    {
        final Work work = mock(Work.class);
        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                work.run();
                return null;
            }
        }).when(delegateWorkManager).startWork(Matchers.<Work>any());

        trackingWorkManager.startWork(work);

        verify(work).run();
    }

    @Test
    public void tracksWorkStart() throws Exception
    {
        final Work work = mock(Work.class);

        final ArgumentCaptor<Work> argument = ArgumentCaptor.forClass(Work.class);
        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                argument.getValue().run();
                return null;
            }
        }).when(delegateWorkManager).startWork(argument.capture());

        trackingWorkManager.startWork(work);

        InOrder inOrder = inOrder(workTracker, delegateWorkManager);
        inOrder.verify(workTracker).addWork(work);
        inOrder.verify(delegateWorkManager).startWork(Matchers.<Work>any());
        inOrder.verify(workTracker).removeWork(work);
    }

    @Test
    public void untracksWorkOnStartWorkException() throws Exception
    {
        final Work work = mock(Work.class);

        doThrow(new WorkException()).when(delegateWorkManager).startWork(Matchers.<Work>any());

        try
        {
            trackingWorkManager.startWork(work);
            expectedExceptionFail();
        }
        catch (Exception e)
        {
            // Expected
        }

        InOrder inOrder = inOrder(workTracker, delegateWorkManager);
        inOrder.verify(workTracker).addWork(work);
        inOrder.verify(delegateWorkManager).startWork(Matchers.<Work>any());
        inOrder.verify(workTracker).removeWork(work);
    }

    @Test
    public void untracksWorkOnStartRuntimeException() throws Exception
    {
        final Work work = mock(Work.class);

        doThrow(new RuntimeException()).when(delegateWorkManager).startWork(Matchers.<Work>any());

        try
        {
            trackingWorkManager.startWork(work);
            expectedExceptionFail();
        }
        catch (Exception e)
        {
            // Expected
        }

        InOrder inOrder = inOrder(workTracker, delegateWorkManager);
        inOrder.verify(workTracker).addWork(work);
        inOrder.verify(delegateWorkManager).startWork(Matchers.<Work>any());
        inOrder.verify(workTracker).removeWork(work);
    }

    @Test
    public void untracksWorkOnStartExecutionException() throws Exception
    {
        final Work work = mock(Work.class);
        doThrow(new RuntimeException()).when(work).run();

        final ArgumentCaptor<Work> argument = ArgumentCaptor.forClass(Work.class);
        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                // Fakes delegation to avoid work exception to leak through the test method
                Thread schedulerThread = new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        argument.getValue().run();
                    }
                });

                schedulerThread.start();
                return null;
            }
        }).when(delegateWorkManager).startWork(argument.capture());

        trackingWorkManager.startWork(work);

        verify(work, timeout(5000)).run();

        InOrder inOrder = inOrder(workTracker, delegateWorkManager);
        inOrder.verify(workTracker).addWork(work);
        inOrder.verify(delegateWorkManager).startWork(Matchers.<Work>any());
        inOrder.verify(workTracker).removeWork(work);
    }

    @Test
    public void startsParameterizedWork() throws WorkException
    {
        final Work work = mock(Work.class);
        long startTimeout = 0;
        ExecutionContext execContext = mock(ExecutionContext.class);
        WorkListener workListener = mock(WorkListener.class);

        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                work.run();
                return null;
            }
        }).when(delegateWorkManager).startWork(Matchers.<Work>any(), eq(startTimeout), eq(execContext), Matchers.<WorkListener>any());

        trackingWorkManager.startWork(work, startTimeout, execContext, workListener);

        verify(work).run();
    }

    @Test
    public void wrapsWorkListenerOnParameterizedStartWork() throws WorkException
    {
        final Work work = mock(Work.class);
        long startTimeout = 0;
        ExecutionContext execContext = mock(ExecutionContext.class);
        WorkListener workListener = mock(WorkListener.class);
        WorkListener wrappedWorkListener = mock(WorkListener.class);

        WorkListenerWrapperFactory workListenerWrapperFactory = mock(WorkListenerWrapperFactory.class);
        when(workListenerWrapperFactory.create(work, workListener)).thenReturn(wrappedWorkListener);
        trackingWorkManager.setWorkListenerWrapperFactory(workListenerWrapperFactory);

        trackingWorkManager.startWork(work, startTimeout, execContext, workListener);

        verify(workListenerWrapperFactory).create(work, workListener);
        verify(delegateWorkManager).startWork(any(Work.class), eq(startTimeout), eq(execContext), eq(wrappedWorkListener));
    }

    @Test
    public void tracksParameterizedWorkStart() throws Exception
    {
        final Work work = mock(Work.class);
        long startTimeout = 0;
        ExecutionContext execContext = mock(ExecutionContext.class);
        WorkListener workListener = mock(WorkListener.class);

        final ArgumentCaptor<Work> argument = ArgumentCaptor.forClass(Work.class);
        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                argument.getValue().run();
                return null;
            }
        }).when(delegateWorkManager).startWork(argument.capture(), eq(startTimeout), eq(execContext), Matchers.<WorkListener>any());

        trackingWorkManager.startWork(work, startTimeout, execContext, workListener);

        InOrder inOrder = inOrder(workTracker, delegateWorkManager);
        inOrder.verify(workTracker).addWork(work);
        inOrder.verify(delegateWorkManager).startWork(argument.capture(), eq(startTimeout), eq(execContext), Matchers.<WorkListener>any());
        inOrder.verify(workTracker).removeWork(work);
    }

    @Test
    public void untracksWorkOnParameterizedStartWorkException() throws Exception
    {
        final Work work = mock(Work.class);
        long startTimeout = 0;
        ExecutionContext execContext = mock(ExecutionContext.class);
        WorkListener workListener = mock(WorkListener.class);

        doThrow(new WorkException()).when(delegateWorkManager).startWork(Matchers.<Work>any(), eq(startTimeout), eq(execContext), Matchers.<WorkListener>any());

        try
        {
            trackingWorkManager.startWork(work, startTimeout, execContext, workListener);
            expectedExceptionFail();
        }
        catch (Exception e)
        {
            // Expected
        }

        InOrder inOrder = inOrder(workTracker, delegateWorkManager);
        inOrder.verify(workTracker).addWork(work);
        inOrder.verify(delegateWorkManager).startWork(Matchers.<Work>any(), eq(startTimeout), eq(execContext), Matchers.<WorkListener>any());
        inOrder.verify(workTracker).removeWork(work);
    }

    @Test
    public void untracksWorkOnParameterizedStartRuntimeException() throws Exception
    {
        final Work work = mock(Work.class);
        long startTimeout = 0;
        ExecutionContext execContext = mock(ExecutionContext.class);
        WorkListener workListener = mock(WorkListener.class);

        doThrow(new RuntimeException()).when(delegateWorkManager).startWork(Matchers.<Work>any(), eq(startTimeout), eq(execContext), Matchers.<WorkListener>any());

        try
        {
            trackingWorkManager.startWork(work, startTimeout, execContext, workListener);
            expectedExceptionFail();
        }
        catch (Exception e)
        {
            // Expected
        }

        InOrder inOrder = inOrder(workTracker, delegateWorkManager);
        inOrder.verify(workTracker).addWork(work);
        inOrder.verify(delegateWorkManager).startWork(Matchers.<Work>any(), eq(startTimeout), eq(execContext), Matchers.<WorkListener>any());
        inOrder.verify(workTracker).removeWork(work);
    }

    @Test
    public void untracksWorkOnParameterizedStartExecutionException() throws Exception
    {
        final Work work = mock(Work.class);
        long startTimeout = 0;
        ExecutionContext execContext = mock(ExecutionContext.class);
        WorkListener workListener = mock(WorkListener.class);

        doThrow(new RuntimeException()).when(work).run();

        final ArgumentCaptor<Work> argument = ArgumentCaptor.forClass(Work.class);
        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                // Fakes delegation to avoid work exception to leak through the test method
                Thread schedulerThread = new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        argument.getValue().run();
                    }
                });

                schedulerThread.start();
                return null;
            }
        }).when(delegateWorkManager).startWork(argument.capture(), eq(startTimeout), eq(execContext), Matchers.<WorkListener>any());

        trackingWorkManager.startWork(work, startTimeout, execContext, workListener);

        verify(work, timeout(5000)).run();

        InOrder inOrder = inOrder(workTracker, delegateWorkManager);
        inOrder.verify(workTracker).addWork(work);
        inOrder.verify(delegateWorkManager).startWork(argument.capture(), eq(startTimeout), eq(execContext), Matchers.<WorkListener>any());
        inOrder.verify(workTracker).removeWork(work);
    }

    @Test
    public void schedulesWork() throws WorkException
    {
        final Work work = mock(Work.class);

        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                work.run();
                return null;
            }
        }).when(delegateWorkManager).scheduleWork(Matchers.<Work>any());

        trackingWorkManager.scheduleWork(work);

        verify(work).run();
    }

    @Test
    public void tracksWorkScheduling() throws Exception
    {
        final Work work = mock(Work.class);

        final ArgumentCaptor<Work> argument = ArgumentCaptor.forClass(Work.class);
        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                argument.getValue().run();
                return null;
            }
        }).when(delegateWorkManager).scheduleWork(argument.capture());

        trackingWorkManager.scheduleWork(work);

        InOrder inOrder = inOrder(workTracker, delegateWorkManager);
        inOrder.verify(workTracker).addWork(work);
        inOrder.verify(delegateWorkManager).scheduleWork(Matchers.<Work>any());
        inOrder.verify(workTracker).removeWork(work);
    }

    @Test
    public void untracksWorkOnSchedulingWorkException() throws Exception
    {
        final Work work = mock(Work.class);

        doThrow(new WorkException()).when(delegateWorkManager).scheduleWork(Matchers.<Work>any());

        try
        {
            trackingWorkManager.scheduleWork(work);
            expectedExceptionFail();
        }
        catch (Exception e)
        {
            // Expected
        }

        InOrder inOrder = inOrder(workTracker, delegateWorkManager);
        inOrder.verify(workTracker).addWork(work);
        inOrder.verify(delegateWorkManager).scheduleWork(Matchers.<Work>any());
        inOrder.verify(workTracker).removeWork(work);
    }

    @Test
    public void untracksWorkOnSchedulingRuntimeException() throws Exception
    {
        final Work work = mock(Work.class);

        doThrow(new RuntimeException()).when(delegateWorkManager).scheduleWork(Matchers.<Work>any());

        try
        {
            trackingWorkManager.scheduleWork(work);
            expectedExceptionFail();
        }
        catch (Exception e)
        {
            // Expected
        }

        InOrder inOrder = inOrder(workTracker, delegateWorkManager);
        inOrder.verify(workTracker).addWork(work);
        inOrder.verify(delegateWorkManager).scheduleWork(Matchers.<Work>any());
        inOrder.verify(workTracker).removeWork(work);
    }

    @Test
    public void untracksWorkOnSchedulingExecutionException() throws Exception
    {
        final Work work = mock(Work.class);
        doThrow(new RuntimeException()).when(work).run();

        final ArgumentCaptor<Work> argument = ArgumentCaptor.forClass(Work.class);
        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                // Fakes delegation to avoid work exception to leak through the test method
                Thread schedulerThread = new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        argument.getValue().run();
                    }
                });

                schedulerThread.start();
                return null;
            }
        }).when(delegateWorkManager).scheduleWork(argument.capture());

        trackingWorkManager.scheduleWork(work);

        verify(work, timeout(5000)).run();

        InOrder inOrder = inOrder(workTracker, delegateWorkManager);
        inOrder.verify(workTracker).addWork(work);
        inOrder.verify(delegateWorkManager).scheduleWork(Matchers.<Work>any());
        inOrder.verify(workTracker).removeWork(work);
    }

    @Test
    public void schedulesParameterizedWork() throws WorkException
    {
        final Work work = mock(Work.class);
        long startTimeout = 0;
        ExecutionContext execContext = mock(ExecutionContext.class);
        WorkListener workListener = mock(WorkListener.class);

        final ArgumentCaptor<Work> argument = ArgumentCaptor.forClass(Work.class);

        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                argument.getValue().run();
                return null;
            }
        }).when(delegateWorkManager).scheduleWork(argument.capture(), eq(startTimeout), eq(execContext), Matchers.<WorkListener>any());

        trackingWorkManager.scheduleWork(work, startTimeout, execContext, workListener);

        verify(work).run();
    }

    @Test
    public void wrapsWorkListenerOnParameterizedScheduleWork() throws WorkException
    {
        final Work work = mock(Work.class);
        long startTimeout = 0;
        ExecutionContext execContext = mock(ExecutionContext.class);
        WorkListener workListener = mock(WorkListener.class);
        WorkListener wrappedWorkListener = mock(WorkListener.class);

        WorkListenerWrapperFactory workListenerWrapperFactory = mock(WorkListenerWrapperFactory.class);
        when(workListenerWrapperFactory.create(work, workListener)).thenReturn(wrappedWorkListener);
        trackingWorkManager.setWorkListenerWrapperFactory(workListenerWrapperFactory);

        trackingWorkManager.scheduleWork(work, startTimeout, execContext, workListener);

        verify(workListenerWrapperFactory).create(work, workListener);
        verify(delegateWorkManager).scheduleWork(any(Work.class), eq(startTimeout), eq(execContext), eq(wrappedWorkListener));
    }

    @Test
    public void tracksPrameterizedWorkScheduling() throws Exception
    {
        final Work work = mock(Work.class);
        long startTimeout = 0;
        ExecutionContext execContext = mock(ExecutionContext.class);
        WorkListener workListener = mock(WorkListener.class);

        final ArgumentCaptor<Work> argument = ArgumentCaptor.forClass(Work.class);

        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                argument.getValue().run();
                return null;
            }
        }).when(delegateWorkManager).scheduleWork(argument.capture(), eq(startTimeout), eq(execContext), Matchers.<WorkListener>any());

        trackingWorkManager.scheduleWork(work, startTimeout, execContext, workListener);

        InOrder inOrder = inOrder(workTracker, delegateWorkManager);
        inOrder.verify(workTracker).addWork(work);
        inOrder.verify(delegateWorkManager).scheduleWork(argument.capture(), eq(startTimeout), eq(execContext), Matchers.<WorkListener>any());
        inOrder.verify(workTracker).removeWork(work);
    }

    @Test
    public void untracksParameterizedWorkOnSchedulingWorkException() throws Exception
    {
        final Work work = mock(Work.class);
        long startTimeout = 0;
        ExecutionContext execContext = mock(ExecutionContext.class);
        WorkListener workListener = mock(WorkListener.class);

        doThrow(new WorkException()).when(delegateWorkManager).scheduleWork(Matchers.<Work>any(), eq(startTimeout), eq(execContext), Matchers.<WorkListener>any());

        try
        {
            trackingWorkManager.scheduleWork(work, startTimeout, execContext, workListener);
            expectedExceptionFail();
        }
        catch (Exception e)
        {
            // Expected
        }

        InOrder inOrder = inOrder(workTracker, delegateWorkManager);
        inOrder.verify(workTracker).addWork(work);
        inOrder.verify(delegateWorkManager).scheduleWork(Matchers.<Work>any(), eq(startTimeout), eq(execContext), Matchers.<WorkListener>any());
        inOrder.verify(workTracker).removeWork(work);
    }

    @Test
    public void untracksParameterizedWorkOnSchedulingRuntimeException() throws Exception
    {
        final Work work = mock(Work.class);
        long startTimeout = 0;
        ExecutionContext execContext = mock(ExecutionContext.class);
        WorkListener workListener = mock(WorkListener.class);

        doThrow(new RuntimeException()).when(delegateWorkManager).scheduleWork(Matchers.<Work>any(), eq(startTimeout), eq(execContext), Matchers.<WorkListener>any());

        try
        {
            trackingWorkManager.scheduleWork(work, startTimeout, execContext, workListener);
            expectedExceptionFail();
        }
        catch (Exception e)
        {
            // Expected
        }

        InOrder inOrder = inOrder(workTracker, delegateWorkManager);
        inOrder.verify(workTracker).addWork(work);
        inOrder.verify(delegateWorkManager).scheduleWork(Matchers.<Work>any(), eq(startTimeout), eq(execContext), Matchers.<WorkListener>any());
        inOrder.verify(workTracker).removeWork(work);
    }

    @Test
    public void untracksParameterizedWorkOnSchedulingExecutionException() throws Exception
    {
        final Work work = mock(Work.class);
        doThrow(new RuntimeException()).when(work).run();
        long startTimeout = 0;
        ExecutionContext execContext = mock(ExecutionContext.class);
        WorkListener workListener = mock(WorkListener.class);

        final ArgumentCaptor<Work> argument = ArgumentCaptor.forClass(Work.class);

        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                // Fakes delegation to avoid work exception to leak through the test method
                Thread schedulerThread = new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        argument.getValue().run();
                    }
                });

                schedulerThread.start();
                return null;
            }
        }).when(delegateWorkManager).scheduleWork(argument.capture(), eq(startTimeout), eq(execContext), Matchers.<WorkListener>any());

        trackingWorkManager.scheduleWork(work, startTimeout, execContext, workListener);

        verify(work, timeout(5000)).run();

        InOrder inOrder = inOrder(workTracker, delegateWorkManager);
        inOrder.verify(workTracker).addWork(work);
        inOrder.verify(delegateWorkManager).scheduleWork(argument.capture(), eq(startTimeout), eq(execContext), Matchers.<WorkListener>any());
        inOrder.verify(workTracker).removeWork(work);
    }

    @Test
    public void executesWork() throws Exception
    {
        final Work work = mock(Work.class);

        final ArgumentCaptor<Runnable> argument = ArgumentCaptor.forClass(Runnable.class);
        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                argument.getValue().run();
                return null;
            }
        }).when(delegateWorkManager).execute(argument.capture());

        trackingWorkManager.execute(work);

        verify(work).run();
    }

    @Test
    public void tracksWorkExecution() throws Exception
    {
        final Work work = mock(Work.class);

        final ArgumentCaptor<Runnable> argument = ArgumentCaptor.forClass(Runnable.class);
        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                argument.getValue().run();
                return null;
            }
        }).when(delegateWorkManager).execute(argument.capture());

        trackingWorkManager.execute(work);

        InOrder inOrder = inOrder(workTracker, delegateWorkManager);
        inOrder.verify(workTracker).addWork(work);
        inOrder.verify(delegateWorkManager).execute(Matchers.<Runnable>any());
        inOrder.verify(workTracker).removeWork(work);
    }

    @Test
    public void untracksWorkOnExecutionException() throws Exception
    {
        final Runnable work = mock(Runnable.class);

        doThrow(new RuntimeException()).when(delegateWorkManager).execute(Matchers.<Runnable>any());

        try
        {
            trackingWorkManager.execute(work);
            expectedExceptionFail();
        }
        catch (Exception e)
        {
            // Expected
        }

        InOrder inOrder = inOrder(workTracker, delegateWorkManager);
        inOrder.verify(workTracker).addWork(work);
        inOrder.verify(delegateWorkManager).execute(Matchers.<Work>any());
        inOrder.verify(workTracker).removeWork(work);
    }

    @Test
    public void untracksWorkOnExecuteExecutionException() throws Exception
    {
        final Runnable work = mock(Runnable.class);
        doThrow(new RuntimeException()).when(work).run();

        final ArgumentCaptor<Runnable> argument = ArgumentCaptor.forClass(Runnable.class);
        doAnswer(new Answer()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                // Fakes delegation to avoid work exception to leak through the test method
                Thread schedulerThread = new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        argument.getValue().run();
                    }
                });

                schedulerThread.start();
                return null;
            }
        }).when(delegateWorkManager).execute(argument.capture());

        trackingWorkManager.execute(work);

        verify(work, timeout(5000)).run();

        InOrder inOrder = inOrder(workTracker, delegateWorkManager);
        inOrder.verify(workTracker).addWork(work);
        inOrder.verify(delegateWorkManager).execute(Matchers.<Runnable>any());
        inOrder.verify(workTracker).removeWork(work);
    }

    @Test
    public void waitsForWorkCompletion() throws Exception
    {
        Work work = mock(Work.class);
        trackingWorkManager.scheduleWork(work);
        List<Runnable> pendingWorks = Collections.<Runnable>singletonList(work);
        when(workTracker.pendingWorks()).thenReturn(pendingWorks).thenReturn(pendingWorks).thenReturn(Collections.<Runnable>emptyList());

        trackingWorkManager.dispose();

        verify(workTracker, times(3)).pendingWorks();
    }

    @Test
    public void disposesWorkTracker() throws Exception
    {
        trackingWorkManager.dispose();
        verify(workTracker).dispose();
    }

    private void expectedExceptionFail()
    {
        fail("Should throw an exception");
    }

    private void assertWorkIsTracked(Work work) throws WorkException
    {
        InOrder inOrder = inOrder(workTracker, delegateWorkManager);
        inOrder.verify(workTracker).addWork(work);
        inOrder.verify(delegateWorkManager).doWork(work);
        inOrder.verify(workTracker).removeWork(work);
    }

    private void assertParameterizedWorkWasTracked(Work work, int startTimeout, ExecutionContext execContext, WorkListener workListener) throws WorkException
    {
        InOrder inOrder = inOrder(workTracker, delegateWorkManager);
        inOrder.verify(workTracker).addWork(work);
        inOrder.verify(delegateWorkManager).doWork(work, startTimeout, execContext, workListener);
        inOrder.verify(workTracker).removeWork(work);
    }
}
