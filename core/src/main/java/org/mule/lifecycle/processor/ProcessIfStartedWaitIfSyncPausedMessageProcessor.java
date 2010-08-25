/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.lifecycle.processor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.LifecycleState;
import org.mule.api.lifecycle.Startable;

public class ProcessIfStartedWaitIfSyncPausedMessageProcessor extends
    ProcessIfStartedWaitIfPausedMessageProcessor
{

    public ProcessIfStartedWaitIfSyncPausedMessageProcessor(Startable startable, LifecycleState lifecycleState)
    {
        super(startable, lifecycleState);
    }

    @Override
    protected MuleEvent processNext(MuleEvent event) throws MuleException
    {
        if (event.getEndpoint().getExchangePattern().hasResponse())
        {
            return super.processNext(event);
        }
        else
        {
            return next.process(event);
        }
    }

}
