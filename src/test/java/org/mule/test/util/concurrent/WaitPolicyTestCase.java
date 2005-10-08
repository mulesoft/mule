/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */
package org.mule.test.util.concurrent;

import edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.RejectedExecutionException;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

import org.mule.util.concurrent.WaitPolicy;

import junit.framework.TestCase;

/**
 * @author Holger Hoffstaette
 */

public class WaitPolicyTestCase extends TestCase
{
	private static int _activeThreads;
	private ThreadPoolExecutor _executor;

	public WaitPolicyTestCase(String name)
	{
		super(name);
	}

	protected void setUp() throws Exception
	{
		super.setUp();
		// allow 1 active & 1 queued Thread
		_executor = new ThreadPoolExecutor(1, 1, 10000L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue(1));
		_activeThreads = 0;
	}

	protected void tearDown() throws Exception
	{
		_executor.shutdown();
		super.tearDown();
	}

	public void testWaitPolicyWithShutdownExecutor() throws Exception
	{
		assertEquals(0, _activeThreads);

		// should wait forever, but will fail immediately
		_executor.setRejectedExecutionHandler(new WaitPolicyTestWrapper());
		_executor.shutdown();

		try
		{
			// should fail immediately
			_executor.execute(new SleepyTask(1000));
			fail();
		}
		catch (RejectedExecutionException rex)
		{
			// expected
		}

		assertEquals(0, _activeThreads);
	}

	public void testWaitPolicyForever() throws Exception
	{
		assertEquals(0, _activeThreads);

		// wait forever
		WaitPolicyTestWrapper policy = new WaitPolicyTestWrapper();
		_executor.setRejectedExecutionHandler(policy);

		// 1 runs immediately
		_executor.execute(new SleepyTask(1000));
		// 2 is queued
		_executor.execute(new SleepyTask(1000));
		// 3 is initially rejected but waits forever
		Runnable s3 = new SleepyTask(1000);
		_executor.execute(s3);

		_executor.awaitTermination(4000, TimeUnit.MILLISECONDS);

		assertSame(s3, policy._lastRejected);
		assertEquals(0, _activeThreads);
	}

	public void testWaitPolicyWithTimeout() throws Exception
	{
		assertEquals(0, _activeThreads);

		// set a reasonable retry interval
		WaitPolicyTestWrapper policy = new WaitPolicyTestWrapper(2500, TimeUnit.MILLISECONDS);
		_executor.setRejectedExecutionHandler(policy);

		// 1 runs immediately
		_executor.execute(new SleepyTask(1000));
		// 2 is queued
		_executor.execute(new SleepyTask(1000));
		// 3 is initially rejected but will eventually succeed
		Runnable s3 = new SleepyTask(1000);
		_executor.execute(s3);

		_executor.awaitTermination(4000, TimeUnit.MILLISECONDS);

		assertSame(s3, policy._lastRejected);
		assertEquals(0, _activeThreads);
	}

	public void testWaitPolicyWithTimeoutFailure() throws Exception
	{
		assertEquals(0, _activeThreads);

		// set a really short wait interval
		WaitPolicyTestWrapper policy = new WaitPolicyTestWrapper(100, TimeUnit.MILLISECONDS);
		_executor.setRejectedExecutionHandler(policy);

		// 1 runs immediately
		_executor.execute(new SleepyTask(1000));
		// 2 is queued
		_executor.execute(new SleepyTask(1000));

		Runnable s3 = new SleepyTask(1000);

		try
		{
			// 3 is initially rejected & will retry but should fail quickly
			_executor.execute(s3);
			fail();
		}
		catch (RejectedExecutionException rex)
		{
			// expected
		}

		_executor.awaitTermination(3000, TimeUnit.MILLISECONDS);

		assertSame(s3, policy._lastRejected);
		assertEquals(0, _activeThreads);
	}

	static class WaitPolicyTestWrapper extends WaitPolicy
	{
		// needed to hand the rejected Runnable back to the TestCase
		Runnable _lastRejected;

		public WaitPolicyTestWrapper()
		{
			super();
		}

		public WaitPolicyTestWrapper(long time, TimeUnit timeUnit)
		{
			super(time, timeUnit);
		}

		public void rejectedExecution(Runnable r, ThreadPoolExecutor e)
		{
			_lastRejected = r;
			super.rejectedExecution(r, e);
		}
	}

	// task to execute - just sleeps for the given interval
	static class SleepyTask extends Object implements Runnable
	{
		private long _sleepTime;

		public SleepyTask(long sleepTime)
		{
			_sleepTime = sleepTime;
		}

		public void run()
		{
			_activeThreads++;

			try
			{
				synchronized (this)
				{
					this.wait(_sleepTime);
				}
			}
			catch (InterruptedException iex)
			{
				// ignore
			}

			_activeThreads--;
		}

	}

}
