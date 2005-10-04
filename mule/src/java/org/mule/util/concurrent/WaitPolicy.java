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
package org.mule.util.concurrent;

import edu.emory.mathcs.backport.java.util.concurrent.RejectedExecutionException;
import edu.emory.mathcs.backport.java.util.concurrent.RejectedExecutionHandler;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Holger Hoffstaette
 */

/**
 * A handler for unexecutable tasks that waits until the task can be
 * submitted for execution.
 * 
 * Generously snipped from the jsr166 repository at:
 * http://gee.cs.oswego.edu/cgi-bin/viewcvs.cgi/jsr166/src/main/java/util/concurrent/ThreadPoolExecutor.java
 * 
 */
public class WaitPolicy implements RejectedExecutionHandler
{
	/**
	 * Constructs a <tt>WaitPolicy</tt>.
	 */
	public WaitPolicy()
	{
	}

	public void rejectedExecution(Runnable r, ThreadPoolExecutor e)
	{
		if (!e.isShutdown())
		{
			try
			{
				// TODO add a customizable timeout to avoid deadlocks
				// see http://altair.cs.oswego.edu/pipermail/concurrency-interest/2004-April/000943.html
				e.getQueue().put(r);
			}
			catch (InterruptedException ie)
			{
				Thread.currentThread().interrupt();
				throw new RejectedExecutionException(ie);
			}
		}
	}
}

