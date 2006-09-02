/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the BSD style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo;

import edu.emory.mathcs.backport.java.util.concurrent.Callable;
import edu.emory.mathcs.backport.java.util.concurrent.ExecutionException;
import edu.emory.mathcs.backport.java.util.concurrent.ExecutorService;
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

public class FutureMessageResult extends FutureTask
{
    /**
     * This is a simple default Executor for FutureMessageResults. Instead of
     * spawning a Thread for each invocation it uses a single pooled daemon Thread
     * with an unbounded queue, so "truly" concurrent operation of multiple Futures
     * or otherwise customized execution behaviour requires calling the
     * {@link #FutureMessageResult(Callable, UMOTransformer, ExecutorService)}
     * constructor and passing in a custom {@link ExecutorService}. This is strongly
     * recommended in order to provide better control over concurrency, resource
     * consumption and possible overruns.
     * <p>
     * Reasons for these defaults:
     * <ul>
     * <li> a daemon thread prevents the VM from blocking on shutdown; lifecycle
     * control should be done elsewhere (e.g. the provider of the custom
     * ExecutorService), otherwise this class would become too overloaded
     * <li> a single thread provides for conservative & predictable yet async
     * behaviour from a client's point of view
     * <li> the unbounded queue is not optimal but probably harmless since e.g. a
     * MuleClient would have to create a LOT of Futures for an OOM. Cancelled/timed
     * out invocations are GC'ed so the problem is rather unlikely to occur.
     * </ul>
     */
    private static final ExecutorService DefaultExecutor = Executors.newSingleThreadExecutor(
        new DaemonThreadFactory("DefaultMuleFutureMessageExecutor"));

    private final ExecutorService executor;
    private final UMOTransformer transformer;

    public FutureMessageResult(Callable callable)
    {
        this(callable, null);
    }

    public FutureMessageResult(Callable callable, UMOTransformer transformer)
    {
        this(callable, transformer, DefaultExecutor);
    }

    public FutureMessageResult(Callable callable, UMOTransformer transformer, ExecutorService executor)
    {
        // null Callable will explode
        super(callable);

        // may be null
        this.transformer = transformer;

        if (executor == null)
        {
            throw new IllegalArgumentException("ExecutorService must not be null.");
        }

        if (executor.isShutdown())
        {
            throw new IllegalArgumentException("ExecutorService must not be shutdown.");
        }

        this.executor = executor;
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
            if (transformer != null)
            {
                obj = transformer.transform(obj);
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
        executor.execute(this);
    }

}
