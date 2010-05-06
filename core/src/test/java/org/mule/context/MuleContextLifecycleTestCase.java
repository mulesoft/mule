/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.context;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextBuilder;
import org.mule.api.context.notification.MuleContextNotificationListener;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.lifecycle.LifecyclePhase;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.security.SecurityManager;
import org.mule.config.builders.DefaultsConfigurationBuilder;
import org.mule.context.notification.MuleContextNotification;
import org.mule.lifecycle.MuleContextLifecycleManager;
import org.mule.util.UUID;
import org.mule.util.queue.QueueManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MuleContextLifecycleTestCase 
{
    private MuleContextBuilder ctxBuilder;
    private SensingLifecycleManager lifecycleManager;
    
    @Before
    public void setup() throws Exception
    {
        ctxBuilder = new DefaultMuleContextBuilder();
        lifecycleManager = new SensingLifecycleManager();
        ctxBuilder.setLifecycleManager(lifecycleManager);
    }
    
    //
    // Initialize
    //
    @Test
    public void initaliseSuccessful() throws Exception
    {
        MuleContext ctx = ctxBuilder.buildMuleContext();
        assertFalse(ctx.isInitialised());
        assertFalse(ctx.isInitialising());
        assertFalse(ctx.isStarted());
        assertFalse(ctx.isDisposed());
        assertFalse(ctx.isDisposing());

        ctx.initialise();
        assertTrue(ctx.isInitialised());
        assertFalse(ctx.isInitialising());
        assertFalse(ctx.isStarted());
        assertFalse(ctx.isDisposed());
        assertFalse(ctx.isDisposing());
    }

    @Test(expected=IllegalStateException.class)
    public void initialiseOnInitialised() throws MuleException
    {
        MuleContext ctx = ctxBuilder.buildMuleContext();
        ctx.initialise();

        // Can't call twice
        ctx.initialise();
    }
    
    @Test(expected=IllegalStateException.class)
    public void initialiseOnStarted() throws Exception
    {
        MuleContext ctx = ctxBuilder.buildMuleContext();
        ctx.initialise();
        new DefaultsConfigurationBuilder().configure(ctx);
        ctx.start();

        // Attempt to initialise once started should fail!
        ctx.initialise();
    }

    @Test(expected=IllegalStateException.class)
    public void initialiseOnStopped() throws Exception
    {
        MuleContext ctx = ctxBuilder.buildMuleContext();
        ctx.initialise();
        new DefaultsConfigurationBuilder().configure(ctx);
        ctx.start();
        ctx.stop();
        
        // Attempt to initialise once stopped should fail!
        ctx.initialise();
    }

    @Test(expected=InitialisationException.class)
    public void initialiseOnDisposed() throws Exception
    {
        MuleContext ctx = ctxBuilder.buildMuleContext();
        ctx.initialise();
        new DefaultsConfigurationBuilder().configure(ctx);
        ctx.start();
        ctx.stop();
        ctx.dispose();

        // Attempt to initialise once disposed should fail!
        ctx.initialise();
    }

    //
    // Start
    //    
    @Test(expected=IllegalStateException.class)
    public void startBeforeInitialise() throws Exception
    {
        MuleContext ctx = ctxBuilder.buildMuleContext();
        ctx.start();
    }
    
    @Test
    public void startOnInitialised() throws Exception
    {
        MuleContext ctx = ctxBuilder.buildMuleContext();
        ctx.initialise();
        new DefaultsConfigurationBuilder().configure(ctx);
        final AtomicBoolean startingNotifFired = new AtomicBoolean(false);
        final AtomicBoolean startedNotifFired = new AtomicBoolean(false);
        ctx.registerListener(new MuleContextNotificationListener<MuleContextNotification>()
        {
            public void onNotification(MuleContextNotification notification)
            {
                if (notification.getAction() == MuleContextNotification.CONTEXT_STARTING)
                {
                    startingNotifFired.set(true);
                }
                if (notification.getAction() == MuleContextNotification.CONTEXT_STARTED)
                {
                    startedNotifFired.set(true);
                }
            }
        });
        ctx.start();
        assertTrue(ctx.isInitialised());
        assertFalse(ctx.isInitialising());
        assertTrue(ctx.isStarted());
        assertFalse(ctx.isDisposed());
        assertFalse(ctx.isDisposing());

        assertTrue("CONTEXT_STARTING notification never fired", startingNotifFired.get());
        assertTrue("CONTEXT_STARTED notification never fired", startedNotifFired.get());

    }

    @Test(expected=IllegalStateException.class)
    public void startOnStarted() throws Exception
    {
        MuleContext ctx = ctxBuilder.buildMuleContext();
        ctx.initialise();
        new DefaultsConfigurationBuilder().configure(ctx);
        final AtomicBoolean startingNotifFired = new AtomicBoolean(false);
        final AtomicBoolean startedNotifFired = new AtomicBoolean(false);
        ctx.registerListener(new MuleContextNotificationListener<MuleContextNotification>()
        {
            public void onNotification(MuleContextNotification notification)
            {
                if (notification.getAction() == MuleContextNotification.CONTEXT_STARTING)
                {
                    startingNotifFired.set(true);
                }
                if (notification.getAction() == MuleContextNotification.CONTEXT_STARTED)
                {
                    startedNotifFired.set(true);
                }
            }
        });
        ctx.start();

        assertTrue("CONTEXT_STARTING notification never fired", startingNotifFired.get());
        assertTrue("CONTEXT_STARTED notification never fired", startedNotifFired.get());
        
        // Can't call twice
        ctx.start();
    }

    @Test
    public void startOnStopped() throws Exception
    {
        MuleContext ctx = ctxBuilder.buildMuleContext();
        ctx.initialise();
        new DefaultsConfigurationBuilder().configure(ctx);
        ctx.start();

        ctx.stop();
        ctx.start();
        assertTrue(ctx.isInitialised());
        assertFalse(ctx.isInitialising());
        assertTrue(ctx.isStarted());
        assertFalse(ctx.isDisposed());
        assertFalse(ctx.isDisposing());
    }

    @Test(expected=IllegalStateException.class)
    public void startOnDisposed() throws Exception
    {
        MuleContext ctx = ctxBuilder.buildMuleContext();
        ctx.initialise();
        ctx.dispose();
        
        // Attempt to start once disposed should fail!
        ctx.start();
    }
    
    //
    // Stop
    //
    @Test(expected=IllegalStateException.class)
    public void stopBeforeInitialise() throws Exception
    {
        MuleContext ctx = ctxBuilder.buildMuleContext();

        // Attempt to stop before initialise should fail!
        ctx.stop();
    }
 
    @Test(expected=IllegalStateException.class)
    public void stopOnInitialised() throws Exception
    {
        MuleContext ctx = ctxBuilder.buildMuleContext();
        ctx.initialise();
        
        // cannot stop if not started
        ctx.stop();
    }
    
    @Test
    public void stopOnStarted() throws Exception
    {
        MuleContext ctx = buildStartedMuleContext();
        
        ctx.stop();
        assertTrue(ctx.isInitialised());
        assertFalse(ctx.isInitialising());
        assertFalse(ctx.isStarted());
        assertFalse(ctx.isDisposed());
        assertFalse(ctx.isDisposing());
    }
    
    @Test(expected=IllegalStateException.class)
    public void stopOnStopped() throws Exception
    {
        MuleContext ctx = buildStartedMuleContext();
        ctx.stop();
        
        // Can't call twice
        ctx.stop();
    }
    
    @Test(expected=IllegalStateException.class)
    public void stopOnDisposed() throws Exception
    {
        MuleContext ctx = buildStartedMuleContext();
        ctx.stop();
        ctx.dispose();
        
        // Attempt to stop once disposed should fail!
        ctx.stop();
    }

    //
    // Dispose
    //
    @Test
    public void disposeBeforeInitialised()
    {
        MuleContext ctx = ctxBuilder.buildMuleContext();
        ctx.dispose();
        assertFalse(ctx.isInitialised());
        assertFalse(ctx.isInitialising());
        assertFalse(ctx.isStarted());
        assertTrue(ctx.isDisposed());
        assertFalse(ctx.isDisposing());
        
        assertLifecycleManagerDidApplyPhases(Disposable.PHASE_NAME);
    }
    
    @Test
    public void disposeOnInitialised() throws Exception
    {
        MuleContext ctx = ctxBuilder.buildMuleContext();
        ctx.initialise();
        ctx.dispose();
        assertFalse(ctx.isInitialised());
        assertFalse(ctx.isInitialising());
        assertFalse(ctx.isStarted());
        assertTrue(ctx.isDisposed());
        assertFalse(ctx.isDisposing());
        
        assertLifecycleManagerDidApplyPhases(Initialisable.PHASE_NAME, Disposable.PHASE_NAME);
    }
    
    @Test
    public void disposeOnStarted() throws Exception
    {
        MuleContext ctx = ctxBuilder.buildMuleContext();
        ctx.initialise();
        new DefaultsConfigurationBuilder().configure(ctx);
        final AtomicBoolean stoppingNotifFired = new AtomicBoolean(false);
        final AtomicBoolean stoppedNotifFired = new AtomicBoolean(false);
        ctx.registerListener(new MuleContextNotificationListener<MuleContextNotification>()
        {
            public void onNotification(MuleContextNotification notification)
            {
                if (notification.getAction() == MuleContextNotification.CONTEXT_STOPPING)
                {
                    stoppingNotifFired.set(true);
                }
                if (notification.getAction() == MuleContextNotification.CONTEXT_STOPPED)
                {
                    stoppedNotifFired.set(true);
                }
            }
        });
        ctx.start();
        ctx.dispose();
        assertFalse(ctx.isInitialised());
        assertFalse(ctx.isInitialising());
        assertFalse(ctx.isStarted());
        assertTrue(ctx.isDisposed());
        assertFalse(ctx.isDisposing());

        // disposing started must go through stop
        assertLifecycleManagerDidApplyAllPhases();

        assertTrue("CONTEXT_STOPPING notification never fired", stoppingNotifFired.get());
        assertTrue("CONTEXT_STOPPED notification never fired", stoppedNotifFired.get());
    }
    
    @Test
    public void disposeOnStopped() throws Exception
    {
        MuleContext  ctx = ctxBuilder.buildMuleContext();
        ctx.initialise();
        new DefaultsConfigurationBuilder().configure(ctx);
        ctx.start();
        ctx.stop();
        ctx.dispose();
        assertFalse(ctx.isInitialised());
        assertFalse(ctx.isInitialising());
        assertFalse(ctx.isStarted());
        assertTrue(ctx.isDisposed());
        assertFalse(ctx.isDisposing());
        
        assertLifecycleManagerDidApplyAllPhases();
    }
    
    @Test(expected=IllegalStateException.class)
    public void disposeOnDisposed() throws Exception
    {
        MuleContext ctx = ctxBuilder.buildMuleContext();
        ctx.initialise();
        ctx.dispose();
        
        // can't call twice
        ctx.dispose();
    }
    
    private MuleContext buildStartedMuleContext() throws Exception
    {
        MuleContext ctx = ctxBuilder.buildMuleContext();
        ctx.initialise();
    
        // DefaultMuleContext refuses to start without these objects in place
        SecurityManager securityManager = Mockito.mock(SecurityManager.class);
        ctx.getRegistry().registerObject(UUID.getUUID(), securityManager);
        
        QueueManager queueManager = Mockito.mock(QueueManager.class);
        ctx.getRegistry().registerObject(UUID.getUUID(), queueManager);
        
        ctx.start();
        return ctx;
    }

    private void assertLifecycleManagerDidApplyPhases(String... phaseNames)
    {
        assertTrue(lifecycleManager.didApplyPhases(phaseNames));
    }
    
    private void assertLifecycleManagerDidApplyAllPhases()
    {
        assertLifecycleManagerDidApplyPhases(
            Initialisable.PHASE_NAME,
            Startable.PHASE_NAME,
            Stoppable.PHASE_NAME,
            Disposable.PHASE_NAME);
    }

    private static class SensingLifecycleManager extends MuleContextLifecycleManager
    {
        private List<String> appliedLifecyclePhases;
        
        public SensingLifecycleManager()
        {
            super();
            appliedLifecyclePhases = new ArrayList<String>();
        }

        public boolean didApplyPhases(String... phaseNames)
        {
            List<String> expectedPhases = Arrays.asList(phaseNames);
            return expectedPhases.equals(appliedLifecyclePhases);
        }

        @Override
        protected void doApplyPhase(LifecyclePhase phase) throws LifecycleException
        {
            appliedLifecyclePhases.add(phase.getName());
            super.doApplyPhase(phase);
        }
    }
}
