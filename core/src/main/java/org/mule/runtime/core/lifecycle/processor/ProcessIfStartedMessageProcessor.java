/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.lifecycle.processor;

import org.mule.api.MuleEvent;
import org.mule.api.NameableObject;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.lifecycle.LifecycleState;
import org.mule.api.lifecycle.Startable;
import org.mule.config.i18n.CoreMessages;
import org.mule.processor.AbstractFilteringMessageProcessor;

public class ProcessIfStartedMessageProcessor extends AbstractFilteringMessageProcessor
{

    protected Startable startable;
    protected LifecycleState lifecycleState;

    public ProcessIfStartedMessageProcessor(Startable startable, LifecycleState lifecycleState)
    {
        this.startable = startable;
        this.lifecycleState = lifecycleState;
    }

    @Override
    protected boolean accept(MuleEvent event)
    {
        return lifecycleState.isStarted();
    }

    @Override
    protected MuleEvent handleUnaccepted(MuleEvent event) throws LifecycleException
    {
        throw new LifecycleException(CoreMessages.isStopped(getStartableName(startable)), event.getMessage());
    }

    protected String getStartableName(Startable startableObject)
    {
        if (startableObject instanceof NameableObject)
        {
            return ((NameableObject) startableObject).getName();
        }
        else
        {
            return startableObject.toString();
        }
    }

}
