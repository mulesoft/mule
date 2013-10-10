/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.work;

import org.mule.api.work.WorkExecutor;
import org.mule.util.concurrent.Latch;

import java.util.concurrent.Executor;

import javax.resource.spi.work.WorkException;

public class StartWorkExecutor implements WorkExecutor
{

    public void doExecute(WorkerContext work, Executor executor) throws WorkException, InterruptedException
    {
        Latch latch = work.provideStartLatch();
        executor.execute(work);
        latch.await();
    }

}
