/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.xmpp;

public abstract class RunnableWithExceptionHandler implements Runnable
{
    public final void run()
    {
        try
        {
            doRun();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    protected abstract void doRun() throws Exception;
}


