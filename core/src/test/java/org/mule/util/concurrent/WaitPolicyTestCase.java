/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.concurrent;

import org.mule.tck.AbstractMuleTestCase;
import org.mule.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.RejectedExecutionException;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;
import edu.emory.mathcs.backport.java.util.concurrent.locks.ReentrantLock;

public class WaitPolicyTestCase extends AbstractMuleTestCase
{
    private ExceptionCollectingThreadGroup threadGroup;
    private ThreadPoolExecutor executor;
    private ReentrantLock executorLock;

    // @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        // allow 1 active & 1 queued Thread
        executor = new ThreadPoolExecutor(1, 1, 10000L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue(1));
        executor.prestartAllCoreThreads();

        // the lock must be fair to guarantee FIFO access to the executor;
        // 'synchronized' on a monitor is not good enough.
        executorLock = new ReentrantLock(true);

        // this is a Threadgroup that collects uncaught exceptions. Necessary for JDK
        // 1.4.x only.
        threadGroup = new ExceptionCollectingThreadGroup();

        // reset counter of active SleepyTasks
        SleepyTask.activeTasks.set(0);
    }

    // @Override
    protected void doTearDown() throws Exception
    {
        executor.shutdown();
        threadGroup.destroy();
        super.doTearDown();
    }

    // Submit the given Runnables to an ExecutorService, but do so in separate
    // threads to avoid blocking the test case when the Executors queue is full.
    // Returns control to the caller when the threads have been started in
    // order to avoid OS-dependent delays in the control flow.
    // A submitting thread waits on a fair Lock to guarantee FIFO ordering.
    // At the time of return the Runnables may or may not be in the queue,
    // rejected, running or already finished. :-)
    protected LinkedList/* <Thread> */execute(final List/* <Runnable> */tasks) throws InterruptedException
    {
        if (tasks == null || tasks.isEmpty())
        {
            throw new IllegalArgumentException("List<Runnable> must not be empty");
        }

        LinkedList submitters = new LinkedList();

        executorLock.lock();

        for (Iterator i = tasks.iterator(); i.hasNext();)
        {
            final Runnable task = (Runnable)i.next();

            Runnable submitterAction = new Runnable()
            {
                public void run()
                {
                    // the lock is important because otherwise two submitters might
                    // stumble over each other, submitting their runnables out-of-order
                    // and causing test failures.
                    try
                    {
                        executorLock.lock();
                        executor.execute(task);
                    }
                    finally
                    {
                        executorLock.unlock();
                    }
                }
            };

            Thread submitter = new Thread(threadGroup, submitterAction);
            submitter.setDaemon(true);
            submitters.add(submitter);
            submitter.start();

            // wait until the thread is actually enqueued on the lock
            while (submitter.isAlive() && !executorLock.hasQueuedThread(submitter))
            {
                Thread.sleep(10);
            }
        }

        executorLock.unlock();
        return submitters;
    }

    public void testWaitPolicyWithShutdownExecutor() throws Exception
    {
        assertEquals(0, SleepyTask.activeTasks.get());

        // wants to wait forever, but will fail immediately
        executor.setRejectedExecutionHandler(new LastRejectedWaitPolicy());
        executor.shutdown();

        // create a task
        List tasks = new ArrayList();
        tasks.add(new SleepyTask("rejected", 1000));

        // should fail and return immediately
        LinkedList submitters = this.execute(tasks);
        assertFalse(submitters.isEmpty());

        // let submitted tasks run
        Thread.sleep(1000);

        LinkedList exceptions = threadGroup.collectedExceptions();
        assertEquals(1, exceptions.size());

        Map.Entry threadFailure = (Map.Entry)((Map)(exceptions.getFirst())).entrySet().iterator().next();
        assertEquals(submitters.getFirst(), threadFailure.getKey());
        assertEquals(RejectedExecutionException.class, threadFailure.getValue().getClass());
        assertEquals(0, SleepyTask.activeTasks.get());
    }

    public void testWaitPolicyForever() throws Exception
    {
        assertEquals(0, SleepyTask.activeTasks.get());

        // tasks wait forever
        LastRejectedWaitPolicy policy = new LastRejectedWaitPolicy(-1, TimeUnit.SECONDS);
        executor.setRejectedExecutionHandler(policy);

        // create tasks
        List tasks = new ArrayList();
        // task 1 runs immediately
        tasks.add(new SleepyTask("hans", 1000));
        // task 2 is queued
        tasks.add(new SleepyTask("franz", 1000));
        // task 3 is initially rejected but waits forever
        Runnable t3 = new SleepyTask("beavis", 1000);
        tasks.add(t3);

        // submit tasks
        LinkedList submitters = this.execute(tasks);
        assertFalse(submitters.isEmpty());

        // the last task should have been queued
        assertFalse(executor.awaitTermination(4000, TimeUnit.MILLISECONDS));
        assertSame(t3, policy.lastRejectedRunnable());
        assertEquals(0, SleepyTask.activeTasks.get());
    }

    public void testWaitPolicyWithTimeout() throws Exception
    {
        assertEquals(0, SleepyTask.activeTasks.get());

        // set a reasonable retry interval
        LastRejectedWaitPolicy policy = new LastRejectedWaitPolicy(2500, TimeUnit.MILLISECONDS);
        executor.setRejectedExecutionHandler(policy);

        // create tasks
        List tasks = new ArrayList();
        // task 1 runs immediately
        tasks.add(new SleepyTask("hans", 1000));
        // task 2 is queued
        tasks.add(new SleepyTask("franz", 1000));
        // task 3 is initially rejected but will eventually succeed
        Runnable t3 = new SleepyTask("tweety", 1000);
        tasks.add(t3);

        // submit tasks
        LinkedList submitters = this.execute(tasks);
        assertFalse(submitters.isEmpty());

        assertFalse(executor.awaitTermination(5000, TimeUnit.MILLISECONDS));
        assertSame(t3, policy.lastRejectedRunnable());
        assertEquals(0, SleepyTask.activeTasks.get());
    }

    public void testWaitPolicyWithTimeoutFailure() throws Exception
    {
        assertEquals(0, SleepyTask.activeTasks.get());

        // set a really short wait interval
        long failureInterval = 100L;
        LastRejectedWaitPolicy policy = new LastRejectedWaitPolicy(failureInterval, TimeUnit.MILLISECONDS);
        executor.setRejectedExecutionHandler(policy);

        // create tasks
        List tasks = new ArrayList();
        // task 1 runs immediately
        tasks.add(new SleepyTask("hans", 1000));
        // task 2 is queued
        tasks.add(new SleepyTask("franz", 1000));
        // task 3 is initially rejected & will retry but should fail quickly
        Runnable failedTask = new SleepyTask("tweety", 1000);
        tasks.add(failedTask);

        // submit tasks
        LinkedList submitters = this.execute(tasks);
        assertFalse(submitters.isEmpty());

        // give failure a chance
        Thread.sleep(failureInterval * 2);

        LinkedList exceptions = threadGroup.collectedExceptions();
        assertEquals(1, exceptions.size());

        Map.Entry threadFailure = (Map.Entry)((Map)(exceptions.getFirst())).entrySet().iterator().next();
        assertEquals(submitters.getLast(), threadFailure.getKey());
        assertEquals(RejectedExecutionException.class, threadFailure.getValue().getClass());

        executor.shutdown();
        assertTrue(executor.awaitTermination(2500, TimeUnit.MILLISECONDS));
        assertSame(failedTask, policy.lastRejectedRunnable());
        assertEquals(0, SleepyTask.activeTasks.get());
    }

}

class LastRejectedWaitPolicy extends WaitPolicy
{
    // needed to hand the last rejected Runnable back to the TestCase
    private volatile Runnable _rejected;

    public LastRejectedWaitPolicy()
    {
        super();
    }

    public LastRejectedWaitPolicy(long time, TimeUnit timeUnit)
    {
        super(time, timeUnit);
    }

    public Runnable lastRejectedRunnable()
    {
        return _rejected;
    }

    // @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor e)
    {
        _rejected = r;
        super.rejectedExecution(r, e);
    }
}

// task to execute - just sleeps for the given interval
class SleepyTask extends Object implements Runnable
{
    public static final AtomicInteger activeTasks = new AtomicInteger(0);

    private final String name;
    private final long sleepTime;

    public SleepyTask(String name, long sleepTime)
    {
        if (StringUtils.isEmpty(name))
        {
            throw new IllegalArgumentException("SleepyTask needs a name!");
        }

        this.name = name;
        this.sleepTime = sleepTime;
    }

    public String toString()
    {
        return this.getClass().getName() + '{' + name + ", " + sleepTime + '}';
    }

    public void run()
    {
        activeTasks.incrementAndGet();

        try
        {
            Thread.sleep(sleepTime);
        }
        catch (InterruptedException iex)
        {
            Thread.currentThread().interrupt();
        }
        finally
        {
            activeTasks.decrementAndGet();
        }
    }

}

// ThreadGroup wrapper that collects uncaught exceptions
class ExceptionCollectingThreadGroup extends ThreadGroup
{
    private final LinkedList/* <Map<Thread, Throwable>> */exceptions = new LinkedList();

    public ExceptionCollectingThreadGroup()
    {
        super("ExceptionCollectingThreadGroup");
    }

    // collected Map(Thread, Throwable) associations
    public LinkedList collectedExceptions()
    {
        return exceptions;
    }

    // all uncaught Thread exceptions end up here
    // @Override
    public void uncaughtException(Thread t, Throwable e)
    {
        synchronized (exceptions)
        {
            exceptions.add(Collections.singletonMap(t, e));
        }
    }
}
