/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.lifecycle;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.management.stats.ComponentStatistics;

import org.mockito.Mockito;

/**
 * @author David Dossot (david@dossot.net)
 */
public class LifecycleTrackerProcessor extends AbstractLifecycleTracker
    implements FlowConstructAware, MessageProcessor
{
    public static String LIFECYCLE_TRACKER_PROCESSOR_PROPERTY = "lifecycle";
    public static String FLOW_CONSRUCT_PROPERTY = "flowConstruct";

    private FlowConstruct flowConstruct;

    public void springInitialize()
    {
        getTracker().add("springInitialize");
    }

    public void springDestroy()
    {
        getTracker().add("springDestroy");
    }

    public void setFlowConstruct(final FlowConstruct flowConstruct)
    {
        getTracker().add("setService");
        this.flowConstruct = flowConstruct;
    }

    public FlowConstruct getFlowConstruct()
    {
        return flowConstruct;
    }

    public ComponentStatistics getStatistics()
    {
        return Mockito.mock(ComponentStatistics.class);
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        event.getMessage().setOutboundProperty(LIFECYCLE_TRACKER_PROCESSOR_PROPERTY, getTracker().toString());
        event.setFlowVariable(FLOW_CONSRUCT_PROPERTY, flowConstruct);
        return event;
    }
}
