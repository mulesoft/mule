/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.lifecycle;

import org.mule.api.MuleContext;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Callable;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.service.Service;
import org.mule.api.service.ServiceAware;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

public class JSR250LifecycleTrackerComponent implements Startable, Stoppable, MuleContextAware, ServiceAware, Callable
{

    private final List<String> tracker = new ArrayList<String>();

    public List<String> getTracker() {
        return tracker;
    }

    public void setProperty(final String value) {
        tracker.add("setProperty");
    }

    public void setMuleContext(final MuleContext context) {
        tracker.add("setMuleContext");
    }

    @PostConstruct
    public void initialise() {
        tracker.add("jsr250 initialise");
    }

    @PreDestroy
    public void dispose() {
        tracker.add("jsr250 dispose");
    }

    public void start() throws MuleException {
        tracker.add("start");
    }

    public void stop() throws MuleException {
        tracker.add("stop");
    }

    public void setService(final Service service)
    {
        getTracker().add("setService");
    }

    public Object onCall(final MuleEventContext eventContext) throws Exception {
        // dirty trick to get the component instance that was used for the
        // request
        return this;
    }
}
