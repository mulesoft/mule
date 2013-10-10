/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
