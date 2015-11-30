/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.registry;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.registry.MuleRegistry;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TransientRegistryLifecycleTestCase extends TransientRegistryTestCase
{
    @Test
    public void testObjectLifecycleWithTransientRegistryDirectly() throws Exception
    {
        TransientRegistry reg = new TransientRegistry(muleContext);
        reg.initialise();
        reg.fireLifecycle(Startable.PHASE_NAME);

        InterfaceBasedTracker tracker = new InterfaceBasedTracker();
        reg.registerObject("test", tracker);

        reg.dispose();
        assertEquals("[setMuleContext, initialise, start, stop, dispose]", tracker.getTracker().toString());
    }

    @Test
    public void testObjectBypassLifecycleWithTransientRegistryDirectly() throws Exception
    {
        TransientRegistry reg = new TransientRegistry(muleContext);
        reg.initialise();
        reg.fireLifecycle(Startable.PHASE_NAME);

        InterfaceBasedTracker tracker = new InterfaceBasedTracker();
        reg.registerObject("test", tracker, MuleRegistry.LIFECYCLE_BYPASS_FLAG);
        reg.dispose();
        assertEquals("[setMuleContext, stop, dispose]", tracker.getTracker().toString());
    }

    @Test
    public void testObjectBypassInjectorsWithTransientRegistryDirectly() throws Exception
    {
        TransientRegistry reg = new TransientRegistry(muleContext);
        reg.initialise();
        reg.fireLifecycle(Startable.PHASE_NAME);

        InterfaceBasedTracker tracker = new InterfaceBasedTracker();
        reg.registerObject("test", tracker, MuleRegistry.INJECT_PROCESSORS_BYPASS_FLAG);
        reg.dispose();
        assertEquals("[initialise, start, stop, dispose]", tracker.getTracker().toString());
    }

    @Test
    public void testObjectBypassLifecycleAndInjectorsWithTransientRegistryDirectly() throws Exception
    {
        TransientRegistry reg = new TransientRegistry(muleContext);
        reg.initialise();
        reg.fireLifecycle(Startable.PHASE_NAME);

        InterfaceBasedTracker tracker = new InterfaceBasedTracker();
        reg.registerObject("test", tracker, MuleRegistry.LIFECYCLE_BYPASS_FLAG + MuleRegistry.INJECT_PROCESSORS_BYPASS_FLAG);
        reg.dispose();
        assertEquals("[stop, dispose]", tracker.getTracker().toString());
    }

    @Test
    public void testObjectLifecycleStatesWithTransientRegistryDirectly() throws Exception
    {
        TransientRegistry reg = new TransientRegistry(muleContext);
        InterfaceBasedTracker tracker = new InterfaceBasedTracker();
        reg.registerObject("test", tracker);
        assertEquals("[setMuleContext]", tracker.getTracker().toString());
        reg.initialise();
        assertEquals("[setMuleContext, initialise]", tracker.getTracker().toString());


        reg.fireLifecycle(Startable.PHASE_NAME);
        assertEquals("[setMuleContext, initialise, start]", tracker.getTracker().toString());

        try
        {
            reg.fireLifecycle(Startable.PHASE_NAME);
            fail("Registry is already started");
        }
        catch (Exception e)
        {
            //expected
        }

        reg.fireLifecycle(Stoppable.PHASE_NAME);
        assertEquals("[setMuleContext, initialise, start, stop]", tracker.getTracker().toString());

        try
        {
            reg.fireLifecycle(Stoppable.PHASE_NAME);
            fail("Registry is already stopped");
        }
        catch (Exception e)
        {
            //expected
        }

        reg.dispose();
        assertEquals("[setMuleContext, initialise, start, stop, dispose]", tracker.getTracker().toString());

        try
        {
            reg.dispose();
            fail("Registry is already disposed");
        }
        catch (Exception e)
        {
            //expected
        }
    }

    @Test
    public void testLifecycleState() throws Exception
    {
        TransientRegistry reg = new TransientRegistry(muleContext);
        reg.fireLifecycle(Initialisable.PHASE_NAME);
        reg.fireLifecycle(Startable.PHASE_NAME);

        InterfaceBasedTracker tracker = new InterfaceBasedTracker();
        reg.registerObject("test", tracker);
        assertEquals("[setMuleContext, initialise, start]", tracker.getTracker().toString());

        reg.fireLifecycle(Disposable.PHASE_NAME);
        assertEquals("[setMuleContext, initialise, start, stop, dispose]", tracker.getTracker().toString());
    }

    @Test
    public void testLifecycleStateOutOfSequenceStartFirstWithTransientRegistryDirectly() throws Exception
    {
        TransientRegistry reg = new TransientRegistry(muleContext);
        try
        {
            //fire start directly
            reg.fireLifecycle(Startable.PHASE_NAME);
            fail("Cannot start without initialising first");
        }
        catch (IllegalStateException e)
        {
            //expected
        }

        InterfaceBasedTracker tracker = new InterfaceBasedTracker();
        reg.registerObject("test", tracker);

        reg.fireLifecycle(Initialisable.PHASE_NAME);
        reg.fireLifecycle(Startable.PHASE_NAME);

        //Initialise called implicitly because you cannot start a component without initialising it first
        assertEquals("[setMuleContext, initialise, start]", tracker.getTracker().toString());

        reg.fireLifecycle(Disposable.PHASE_NAME);
        //Stop called implicitly because you cannot dispose component without stopping it first
        assertEquals("[setMuleContext, initialise, start, stop, dispose]", tracker.getTracker().toString());
    }

    @Test
    public void testLifecycleStateOutOfSequenceStopFirstWithTransientRegistryDirectly() throws Exception
    {
        TransientRegistry reg = new TransientRegistry(muleContext);
        try
        {
            //fire stop directly
            reg.fireLifecycle(Stoppable.PHASE_NAME);
            fail("Cannot stop without starting first");
        }
        catch (IllegalStateException e)
        {
            //expected
        }

        InterfaceBasedTracker tracker = new InterfaceBasedTracker();
        reg.registerObject("test", tracker);

        reg.fireLifecycle(Initialisable.PHASE_NAME);
        reg.fireLifecycle(Startable.PHASE_NAME);
        reg.fireLifecycle(Stoppable.PHASE_NAME);

        //Start is bypassed because the component was added when the registry was stopped, hence no need to start the component
        //Stop isn't called either because start was not called
        //Initialised is called because in order for a component to be stopped it needs to be initialised
        assertEquals("[setMuleContext, initialise, start, stop]", tracker.getTracker().toString());

        reg.fireLifecycle(Disposable.PHASE_NAME);
        assertEquals("[setMuleContext, initialise, start, stop, dispose]", tracker.getTracker().toString());
    }

    public class InterfaceBasedTracker extends AbstractLifecycleTracker
    {
        // no custom methods
    }

    public class JSR250ObjectLifecycleTracker implements MuleContextAware
    {
        private final List<String> tracker = new ArrayList<String>();

        public List<String> getTracker() {
            return tracker;
        }

        public void setMuleContext(MuleContext context)
        {
            tracker.add("setMuleContext");
        }

        @PostConstruct
        public void init()
        {
            tracker.add("initialise");
        }

        @PreDestroy
        public void dispose()
        {
            tracker.add("dispose");
        }
    }
}
