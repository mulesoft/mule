/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo;

import edu.emory.mathcs.backport.java.util.concurrent.Callable;
import edu.emory.mathcs.backport.java.util.concurrent.ExecutionException;
import edu.emory.mathcs.backport.java.util.concurrent.Executor;
import edu.emory.mathcs.backport.java.util.concurrent.Executors;
import edu.emory.mathcs.backport.java.util.concurrent.FutureTask;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.TimeoutException;

import org.mule.impl.MuleMessage;
import org.mule.umo.transformer.TransformerException;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.concurrent.DaemonThreadFactory;

/**
 * <code>FutureMessageResult</code> is an UMOMessage result of a remote invocation
 * on a Mule Server. This object makes the result available to the client code once
 * the request has been processed. This execution happens asynchronously.
 */
// @ThreadSafe
public class FutureMessageResult extends FutureTask
{
    /**
     * This is a simple default Executor for FutureMessageResults. Instead of
     * spawning a Thread for each invocation it uses a single daemon Thread with an
     * unbounded queue, so "truly" concurrent operation of multiple Futures or
     * otherwise customized execution behaviour requires calling the
     * {@link #setExecutor(Executor)} method and passing in a custom {@link Executor}.
     * This is strongly recommended in order to provide better control over
     * concurrency, resource consumption and possible overruns.
     * <p>
     * Reasons for these defaults:
     * <ul>
     * <li> a daemon thread does not block the VM on shutdown; lifecycle control
     * should be done elsewhere (e.g. the provider of the custom ExecutorService),
     * otherwise this class would become too overloaded
     * <li> a single thread provides for conservative & predictable yet async
     * behaviour from a client's point of view
     * <li> the unbounded queue is not optimal but probably harmless since e.g. a
     * MuleClient would have to create a LOT of Futures for an OOM. Cancelled/timed
     * out invocations are GC'ed so the problem is rather unlikely to occur.
     * </ul>
     */
    private static final Executor DefaultExecutor =
        Executors.newSingleThreadExecutor(new DaemonThreadFactory("MuleDefaultFutureMessageExecutor"));

    // @GuardedBy(this)
    private Executor executor;
    // @GuardedBy(this)
    private UMOTransformer transformer;

    public FutureMessageResult(Callable callable)
    {
        super(callable);
        this.executor = DefaultExecutor;
    }

    /**
     * @deprecated Please use {@link #FutureMessageResult(Callable)} and configure
     *             e.g with {@link #setExecutor(Executor)} or
     *             {@link #setTransformer(UMOTransformer)}
     */
    public FutureMessageResult(Callable callable, UMOTransformer transformer)
    {
        this(callable);
        this.transformer = transformer;
    }

    /**
     * Set a post-invocation transformer.
     * 
     * @param t UMOTransformer to be applied to the result of this invocation. May be
     *            null.
     */
    public void setTransformer(UMOTransformer t)
    {
        synchronized (this)
        {
            this.transformer = t;
        }
    }

    /**
     * Set an ExecutorService to run this invocation.
     * 
     * @param e the executor to be used.
     * @throws IllegalArgumentException when the executor is null or shutdown.
     */
    public void setExecutor(Executor e)
    {
        if (e == null)
        {
            throw new IllegalArgumentException("Executor must not be null.");
        }

        synchronized (this)
        {
            this.executor = e;
        }
    }

    public UMOMessage getMessage() throws InterruptedException, ExecutionException, TransformerException
    {
        return this.getMessage(this.get());
    }

    public UMOMessage getMessage(long timeout)
        throws InterruptedException, ExecutionException, TimeoutException, TransformerException
    {
        return this.getMessage(this.get(timeout, TimeUnit.MILLISECONDS));
    }

    private UMOMessage getMessage(Object obj) throws TransformerException
    {
        if (obj != null)
        {
            if (obj instanceof UMOMessage)
            {
                return (UMOMessage)obj;
            }

            synchronized (this)
            {
                if (transformer != null)
                {
                    obj = transformer.transform(obj);
                }
            }

            return new MuleMessage(obj);
        }
        else
        {
            return null;
        }
    }

    /**
     * Start asynchronous execution of this task
     */
    public void execute()
    {
        synchronized (this)
        {
            executor.execute(this);
        }
    }

}
