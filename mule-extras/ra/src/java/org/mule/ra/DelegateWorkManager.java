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
package org.mule.ra;

import org.mule.umo.UMOException;
import org.mule.umo.manager.UMOWorkManager;

import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;
import javax.resource.spi.work.WorkManager;

/**
 * <code>DelegateWorkManager</code> TODO
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class DelegateWorkManager implements UMOWorkManager
{
    private WorkManager workManager;

    public DelegateWorkManager(WorkManager workManager)
    {
        this.workManager = workManager;
    }

    public void doWork(Work work) throws WorkException
    {
        workManager.doWork(work);
    }

    public void doWork(Work work, long l, ExecutionContext executionContext, WorkListener workListener)
            throws WorkException
    {
        workManager.doWork(work, l, executionContext, workListener);
    }

    public long startWork(Work work) throws WorkException
    {
        return workManager.startWork(work);
    }

    public long startWork(Work work, long l, ExecutionContext executionContext, WorkListener workListener)
            throws WorkException
    {
        return workManager.startWork(work, l, executionContext, workListener);
    }

    public void scheduleWork(Work work) throws WorkException
    {
        workManager.scheduleWork(work);
    }

    public void scheduleWork(Work work, long l, ExecutionContext executionContext, WorkListener workListener)
            throws WorkException
    {
        workManager.scheduleWork(work, l, executionContext, workListener);
    }

    public void start() throws UMOException
    {
    }

    public void stop() throws UMOException
    {
    }

    public void dispose()
    {
    }
}
