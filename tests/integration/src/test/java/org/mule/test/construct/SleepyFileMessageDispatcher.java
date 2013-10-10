/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.construct;

import org.mule.api.MuleEvent;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.transport.file.FileMessageDispatcher;

public class SleepyFileMessageDispatcher extends FileMessageDispatcher
{
    public SleepyFileMessageDispatcher(OutboundEndpoint endpoint)
    {
        super(endpoint);
    }

    @Override
    protected void doDispatch(MuleEvent event) throws Exception
    {
        String sleepTime = event.getMessage().getInvocationProperty(
            FlowSyncAsyncProcessingStrategyTestCase.SLEEP_TIME);

        Thread.sleep(Integer.valueOf(sleepTime));

        super.doDispatch(event);

    }

}
