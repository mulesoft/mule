/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.registry;

import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.registry.RegistrationException;
import org.mule.tck.AbstractMuleTestCase;

public class TransientRegistryTestCase extends AbstractMuleTestCase
{
    public void testObjectLifecycle() throws Exception
    {
        muleContext.start();

        InterfaceBasedTracker tracker = new InterfaceBasedTracker();
        muleContext.getRegistry().registerObject("test", tracker);

        muleContext.dispose();
        assertEquals("[setMuleContext, initialise, start, stop, dispose]", tracker.getTracker().toString());
    }

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

    public void testObjectBypassLifecycle() throws Exception
    {
        muleContext.start();

        InterfaceBasedTracker tracker = new InterfaceBasedTracker();
        muleContext.getRegistry().registerObject("test", tracker, MuleRegistry.LIFECYCLE_BYPASS_FLAG);
        muleContext.dispose();
        assertEquals("[setMuleContext, stop, dispose]", tracker.getTracker().toString());
    }

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

    public void testObjectBypassInjectors() throws Exception
    {
        muleContext.start();
        InterfaceBasedTracker tracker = new InterfaceBasedTracker();
        muleContext.getRegistry().registerObject("test", tracker, MuleRegistry.INJECT_PROCESSORS_BYPASS_FLAG);
        muleContext.dispose();
        assertEquals("[initialise, start, stop, dispose]", tracker.getTracker().toString());
    }

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

    public void testObjectBypassLifecycleAndInjectors() throws Exception
    {
        muleContext.start();

        InterfaceBasedTracker tracker = new InterfaceBasedTracker();
        muleContext.getRegistry().registerObject("test", tracker, MuleRegistry.LIFECYCLE_BYPASS_FLAG + MuleRegistry.INJECT_PROCESSORS_BYPASS_FLAG);
        muleContext.dispose();
        assertEquals("[stop, dispose]", tracker.getTracker().toString());
    }

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

    public void testObjectLifecycleStates() throws Exception
    {
        InterfaceBasedTracker tracker = new InterfaceBasedTracker();
        muleContext.getRegistry().registerObject("test", tracker);
        assertEquals("[setMuleContext, initialise]", tracker.getTracker().toString());

        try
        {
            muleContext.initialise();
            fail("context already initialised");
        }
        catch (IllegalStateException e)
        {
            //expected
        }

        muleContext.start();
        assertEquals("[setMuleContext, initialise, start]", tracker.getTracker().toString());

        try
        {
            muleContext.start();
            fail("context already started");
        }
        catch (IllegalStateException e)
        {
            //expected
        }

        muleContext.stop();
        assertEquals("[setMuleContext, initialise, start, stop]", tracker.getTracker().toString());

        try
        {
            muleContext.stop();
            fail("context already stopped");
        }
        catch (IllegalStateException e)
        {
            //expected
        }

        muleContext.dispose();
        assertEquals("[setMuleContext, initialise, start, stop, dispose]", tracker.getTracker().toString());

        try
        {
            muleContext.dispose();
            fail("context already disposed");
        }
        catch (IllegalStateException e)
        {
            //expected
        }

    }

    public void testObjectLifecycleRestart() throws Exception
    {
        InterfaceBasedTracker tracker = new InterfaceBasedTracker();
        muleContext.getRegistry().registerObject("test", tracker);

        muleContext.start();
        assertEquals("[setMuleContext, initialise, start]", tracker.getTracker().toString());

        muleContext.stop();
        assertEquals("[setMuleContext, initialise, start, stop]", tracker.getTracker().toString());

        muleContext.start();
        assertEquals("[setMuleContext, initialise, start, stop, start]", tracker.getTracker().toString());

        muleContext.dispose();
        assertEquals("[setMuleContext, initialise, start, stop, start, stop, dispose]", tracker.getTracker().toString());
    }

    public void testObjectLifecycleRestartWithTransientRegistryDirectly() throws Exception
    {
        TransientRegistry reg = new TransientRegistry(muleContext);
        InterfaceBasedTracker tracker = new InterfaceBasedTracker();
        reg.registerObject("test", tracker);

        reg.fireLifecycle(Initialisable.PHASE_NAME);
        reg.fireLifecycle(Startable.PHASE_NAME);
        assertEquals("[setMuleContext, initialise, start]", tracker.getTracker().toString());

        reg.fireLifecycle(Stoppable.PHASE_NAME);
        assertEquals("[setMuleContext, initialise, start, stop]", tracker.getTracker().toString());

        reg.fireLifecycle(Startable.PHASE_NAME);
        assertEquals("[setMuleContext, initialise, start, stop, start]", tracker.getTracker().toString());

        reg.dispose();
        assertEquals("[setMuleContext, initialise, start, stop, start, stop, dispose]", tracker.getTracker().toString());
    }


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

//        reg.fireLifecycle(Startable.PHASE_NAME);
//        assertEquals("[setMuleContext, initialise, start]", tracker.getTracker().toString());

        reg.fireLifecycle(Stoppable.PHASE_NAME);
        assertEquals("[setMuleContext, initialise, start, stop]", tracker.getTracker().toString());

//        reg.fireLifecycle(Stoppable.PHASE_NAME);
//        assertEquals("[setMuleContext, initialise, start, stop]", tracker.getTracker().toString());

        reg.dispose();
        assertEquals("[setMuleContext, initialise, start, stop, dispose]", tracker.getTracker().toString());

        reg.dispose();
        assertEquals("[setMuleContext, initialise, start, stop, dispose]", tracker.getTracker().toString());

    }


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


    public void testLifecycleStateOutOfSequenceDisposeFirstWithTransientRegistryDirectly() throws Exception
    {
        TransientRegistry reg = new TransientRegistry(muleContext);
        //fire stop directly
        reg.fireLifecycle(Disposable.PHASE_NAME);

        InterfaceBasedTracker tracker = new InterfaceBasedTracker();
        try
        {
            reg.registerObject("test", tracker);
            fail("Cannot register objects on a disposed registry");
        }
        catch (RegistrationException e)
        {
            //Expected
        }
    }


public void testLifecycleStateOutOfSequenceStartFirst() throws Exception
    {
        muleContext.start();
        InterfaceBasedTracker tracker = new InterfaceBasedTracker();
        muleContext.getRegistry().registerObject("test", tracker);
        //Initialise called implicitly because you cannot start a component without initialising it first
        assertEquals("[setMuleContext, initialise, start]", tracker.getTracker().toString());

        muleContext.dispose();
        //Stop called implicitly because you cannot dispose component without stopping it first
        assertEquals("[setMuleContext, initialise, start, stop, dispose]", tracker.getTracker().toString());
    }

    public void testLifecycleStateOutOfSequenceStopFirst() throws Exception
    {
        try
        {
            muleContext.stop();
            fail("Cannot not stop the context if not started");
        }
        catch (IllegalStateException e)
        {
            //expected
        }

        muleContext.start();
        muleContext.stop();
        InterfaceBasedTracker tracker = new InterfaceBasedTracker();
        muleContext.getRegistry().registerObject("test", tracker);
        //Start is bypassed because the component was added when the registry was stopped, hence no need to start the component
        //Stop isn't called either because start was not called
        //Initialised is called because in order for a component to be stopped it needs to be initialised
        assertEquals("[setMuleContext, initialise, start, stop]", tracker.getTracker().toString());

        muleContext.dispose();
        assertEquals("[setMuleContext, initialise, start, stop, dispose]", tracker.getTracker().toString());
    }


    public void testLifecycleStateOutOfSequenceDisposeFirst() throws Exception
    {
        muleContext.dispose();

        InterfaceBasedTracker tracker = new InterfaceBasedTracker();
        try
        {
            muleContext.getRegistry().registerObject("test", tracker);
            fail("cannot register objects on a disposed registry");
        }
        catch (RegistrationException e)
        {
            //Expected
        }
    }


    public class InterfaceBasedTracker extends AbstractLifecycleTracker
    {

    }


}
