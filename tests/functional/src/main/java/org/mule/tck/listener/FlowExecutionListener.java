/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.listener;

import static org.junit.Assert.fail;

import org.mule.api.MuleContext;
import org.mule.api.context.notification.PipelineMessageNotificationListener;
import org.mule.context.notification.NotificationException;
import org.mule.context.notification.PipelineMessageNotification;
import org.mule.util.concurrent.Latch;

import java.util.concurrent.TimeUnit;

/**
 * Listener for flow execution complete action.
 */
public class FlowExecutionListener
{

    private final Latch flowExecutedLatch = new Latch();
    private String flowName;
    private int timeout = 10000;

    /**
     * Constructor for releasing latch when any flow execution completes
     */
    public FlowExecutionListener(MuleContext muleContext)
    {
        createFlowExecutionListener(muleContext);
    }

    /**
     * Constructor for releasing latch when flow with name flowName completes
     */
    public FlowExecutionListener(String flowName, MuleContext muleContext)
    {
        this.flowName = flowName;
        createFlowExecutionListener(muleContext);
    }

    private void createFlowExecutionListener(MuleContext muleContext)
    {
        try
        {
            muleContext.registerListener(new PipelineMessageNotificationListener<PipelineMessageNotification>()
            {
                @Override
                public void onNotification(PipelineMessageNotification notification)
                {
                    if (flowName != null && !notification.getResourceIdentifier().equals(flowName))
                    {
                        return;
                    }
                    if (notification.getAction() == PipelineMessageNotification.PROCESS_COMPLETE)
                    {
                        flowExecutedLatch.release();
                    }
                }
            });
        }
        catch (NotificationException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void waitUntilFlowIsComplete()
    {
        try
        {
            if (!flowExecutedLatch.await(timeout, TimeUnit.MILLISECONDS))
            {
                fail(String.format("Flow %s never completed an execution", (flowName == null ? "ANY FLOW" : flowName)));
            }
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }
    }

    public FlowExecutionListener setTimeoutInMillis(int timeout)
    {
        this.timeout = timeout;
        return this;
    }
}
