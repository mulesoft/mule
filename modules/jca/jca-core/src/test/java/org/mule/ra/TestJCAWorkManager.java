/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.ra;

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
