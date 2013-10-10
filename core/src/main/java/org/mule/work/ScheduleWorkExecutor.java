/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.work;

import org.mule.api.work.WorkExecutor;

import javax.resource.spi.work.WorkException;

import edu.emory.mathcs.backport.java.util.concurrent.Executor;

public class ScheduleWorkExecutor implements WorkExecutor
{

    public void doExecute(WorkerContext work, Executor executor) throws WorkException, InterruptedException
    {
        executor.execute(work);
    }

}
