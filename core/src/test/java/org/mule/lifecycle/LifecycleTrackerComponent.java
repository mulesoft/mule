/*
 * $Id: LifecycleTrackerComponent.java 15966 2009-11-04 23:25:23Z rossmason $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.lifecycle;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.component.Component;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.service.Service;
import org.mule.api.service.ServiceAware;
import org.mule.management.stats.ComponentStatistics;

import org.mockito.Mockito;

/**
 * @author David Dossot (david@dossot.net)
 */
public class LifecycleTrackerComponent extends AbstractLifecycleTracker implements ServiceAware, Component
{

    private Service service;

    public void springInitialize()
    {
        getTracker().add("springInitialize");
    }

    public void springDestroy()
    {
        getTracker().add("springDestroy");
    }

    public void setService(final Service service)
    {
        getTracker().add("setService");
        this.service = service;
    }

    public Service getService()
    {
        return service;
    }

    public ComponentStatistics getStatistics()
    {
        return Mockito.mock(ComponentStatistics.class);
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        return event;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        // Mirror behaviour in AbstractComponent
        if (service != null && service.getLifecycleManager() != null
            && service.getLifecycleManager().getState().isInitialised())
        {
            return;
        }
        super.initialise();
    }

}
