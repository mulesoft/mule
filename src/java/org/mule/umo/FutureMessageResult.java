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
 */
package org.mule.umo;

import EDU.oswego.cs.dl.util.concurrent.Callable;
import EDU.oswego.cs.dl.util.concurrent.FutureResult;
import org.mule.impl.MuleMessage;
import org.mule.umo.transformer.TransformerException;
import org.mule.umo.transformer.UMOTransformer;

import java.lang.reflect.InvocationTargetException;

/**
 * <code>FutureMessageResult</code> is an UMOMessage result of a remote invocation on a Mule Server.
 * this object makes the result available to the client code once the request has been processed.  This execution
 * happens asynchronously.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class FutureMessageResult extends FutureResult
{
    private UMOTransformer transformer;

    public FutureMessageResult()
    {
        super();
    }

    public FutureMessageResult(UMOTransformer transformer)
    {
        this();
        this.transformer = transformer;
    }

    public UMOMessage getMessage() throws InvocationTargetException, InterruptedException, TransformerException
    {
        Object obj = get();
        return getMessage(obj);
    }

    public UMOMessage getMessage(long timeout) throws InvocationTargetException, InterruptedException, TransformerException
    {
        Object obj = this.timedGet(timeout);
        return getMessage(obj);
    }

    private UMOMessage getMessage(Object obj) throws TransformerException
    {
        if (obj != null)
        {
            if (obj instanceof UMOMessage)
            {
                return (UMOMessage) obj;
            }
            if (transformer != null)
            {
                Object payload = transformer.transform(obj);
                return new MuleMessage(payload, null);
            }
            return new MuleMessage(obj, null);
        } else
        {
            return null;
        }
    }

    /**
     * A convenience method for executing a task.  This is not as efficient
     * as loading a thread from a pool so should not be used frequently
     * @param callable the Action to execute
     */
    public void execute(Callable callable) {
        Runnable runnable = setter(callable);
        Thread worker = new Thread(runnable);
        worker.start();
    }
}
