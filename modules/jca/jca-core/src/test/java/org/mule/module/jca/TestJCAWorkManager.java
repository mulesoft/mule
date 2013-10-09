/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */

package org.mule.module.jca;

import java.util.ArrayList;
import java.util.List;

import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;
import javax.resource.spi.work.WorkManager;

public class TestJCAWorkManager implements WorkManager
{

    private List doWorkList = new ArrayList();
    private List scheduledWorkList = new ArrayList();
    private List startWorkList = new ArrayList();

    public void doWork(Work arg0) throws WorkException
    {
        doWorkList.add(arg0);
    }

    public void doWork(Work arg0, long arg1, ExecutionContext arg2, WorkListener arg3) throws WorkException
    {
        doWorkList.add(arg0);
    }

    public void scheduleWork(Work arg0) throws WorkException
    {
        scheduledWorkList.add(arg0);
    }

    public void scheduleWork(Work arg0, long arg1, ExecutionContext arg2, WorkListener arg3) throws WorkException
    {
        scheduledWorkList.add(arg0);
    }

    public long startWork(Work arg0) throws WorkException
    {
        startWorkList.add(arg0);
        return 0;
    }

    public long startWork(Work arg0, long arg1, ExecutionContext arg2, WorkListener arg3) throws WorkException
    {
        startWorkList.add(arg0);
        return 0;
    }

    public List getDoWorkList()
    {
        return doWorkList;
    }

    public List getScheduledWorkList()
    {
        return scheduledWorkList;
    }

    public List getStartWorkList()
    {
        return startWorkList;
    }

}
