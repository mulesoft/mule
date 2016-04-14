/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.work;

import org.mule.work.WorkerContext;

import java.util.concurrent.Executor;

import javax.resource.spi.work.WorkException;

/**
 * <code>WorkExecutor</code> TODO
 */
public interface WorkExecutor
{

    /**
     * This method must be implemented by sub-classes in order to provide the
     * relevant synchronization policy. It is called by the executeWork template
     * method.
     * 
     * @param work Work to be executed.
     * @throws javax.resource.spi.work.WorkException Indicates that the work has
     *             failed.
     * @throws InterruptedException Indicates that the thread in charge of the
     *             execution of the specified work has been interrupted.
     */
    void doExecute(WorkerContext work, Executor executor) throws WorkException, InterruptedException;

}
