/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.lifecycle.processor;

import org.mule.api.MuleEvent;
import org.mule.api.NamedObject;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.lifecycle.LifecycleState;
import org.mule.api.lifecycle.Startable;
import org.mule.config.i18n.CoreMessages;
import org.mule.processor.FilteringtInterceptingMessageProcessor;

public class ProcessIfStartedMessageProcessor extends FilteringtInterceptingMessageProcessor
{

    protected Startable startable;
    protected LifecycleState lifecycleState;

    public ProcessIfStartedMessageProcessor(Startable startable, LifecycleState lifecycleState)
    {
        this.startable = startable;
        this.lifecycleState = lifecycleState;
    }

    @Override
    protected boolean accept()
    {
        return lifecycleState.isStarted();
    }

    @Override
    protected MuleEvent handleUnaccepted(MuleEvent event) throws LifecycleException
    {
        throw new LifecycleException(CoreMessages.isStopped(getStartableName(startable)), event.getMessage());
    }

    protected String getStartableName(Startable startable)
    {
        if (startable instanceof NamedObject)
        {
            return ((NamedObject) startable).getName();
        }
        else
        {
            return startable.toString();
        }
    }

};
