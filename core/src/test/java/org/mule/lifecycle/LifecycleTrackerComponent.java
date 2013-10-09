/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.lifecycle;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.component.Component;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.management.stats.ComponentStatistics;

import org.mockito.Mockito;

/**
 * @author David Dossot (david@dossot.net)
 */
public class LifecycleTrackerComponent extends AbstractLifecycleTracker implements FlowConstructAware, Component
{

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
        return event;
    }
}
