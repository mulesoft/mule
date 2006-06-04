/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.umo;

import edu.emory.mathcs.backport.java.util.concurrent.Callable;
import edu.emory.mathcs.backport.java.util.concurrent.ExecutionException;
import edu.emory.mathcs.backport.java.util.concurrent.FutureTask;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.TimeoutException;

import org.mule.impl.MuleMessage;
import org.mule.umo.transformer.TransformerException;
import org.mule.umo.transformer.UMOTransformer;

/**
 * <code>FutureMessageResult</code> is an UMOMessage result of a remote
 * invocation on a Mule Server. this object makes the result available to the
 * client code once the request has been processed. This execution happens
 * asynchronously.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class FutureMessageResult extends FutureTask
{
    private UMOTransformer transformer;

    public FutureMessageResult(Callable callable)
    {
        super(callable);
    }

    public FutureMessageResult(Callable callable, UMOTransformer transformer)
    {
        this(callable);
        this.transformer = transformer;
    }

    public UMOMessage getMessage()
        throws InterruptedException, ExecutionException, TransformerException
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
        if (obj != null) {
            if (obj instanceof UMOMessage) {
                return (UMOMessage) obj;
            }
            if (transformer != null) {
                obj = transformer.transform(obj);
            }
            return new MuleMessage(obj);
        } else {
            return null;
        }
    }

    /**
     * A convenience method for executing a task. This is not as efficient as
     * loading a thread from a pool so should not be used frequently
     * 
     * @param callable the Action to execute
     */
    public void execute()
    {
        // TODO MULE-732 - rather use ExecutorService.submit()
        new Thread(this).start();
    }

}
