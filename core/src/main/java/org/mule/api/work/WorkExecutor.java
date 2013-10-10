/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
