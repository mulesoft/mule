/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.security.SecurityManager;
import org.mule.config.builders.DefaultsConfigurationBuilder;
import org.mule.context.notification.MuleContextNotification;
import org.mule.lifecycle.MuleContextLifecycleManager;
import org.mule.util.JdkVersionUtils;
import org.mule.util.UUID;
import org.mule.util.queue.QueueManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mule.tck.MuleAssert.assertTrue;

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

    @Test(expected=IllegalStateException.class)
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
        NotificationListener listener = new NotificationListener();
        ctx.registerListener(listener);
        ctx.start();
        
        assertTrue(ctx.isInitialised());
        assertFalse(ctx.isInitialising());
        assertTrue(ctx.isStarted());
        assertFalse(ctx.isDisposed());
        assertFalse(ctx.isDisposing());

        assertTrue("CONTEXT_STARTING notification never fired", listener.startingNotificationFired);
        assertTrue("CONTEXT_STARTED notification never fired", listener.startedNotificationFired);
    }

    @Test(expected=IllegalStateException.class)
    public void startOnStarted() throws Exception
    {
        MuleContext ctx = ctxBuilder.buildMuleContext();
        ctx.initialise();
        
        new DefaultsConfigurationBuilder().configure(ctx);
        NotificationListener listener = new NotificationListener();
        ctx.registerListener(listener);
        ctx.start();

        assertTrue("CONTEXT_STARTING notification never fired", listener.startingNotificationFired);
        assertTrue("CONTEXT_STARTED notification never fired", listener.startedNotificationFired);
        
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
        final NotificationListener listener = new NotificationListener();
        ctx.registerListener(listener);

        ctx.start();
        ctx.dispose();
        assertFalse(ctx.isInitialised());
        assertFalse(ctx.isInitialising());
        assertFalse(ctx.isStarted());
        assertTrue(ctx.isDisposed());
        assertFalse(ctx.isDisposing());

        // disposing started must go through stop
        assertLifecycleManagerDidApplyAllPhases();

        assertTrue("CONTEXT_STOPPING notification never fired", listener.stoppingNotificationFired.get());
        assertTrue("CONTEXT_STOPPED notification never fired", listener.stoppedNotificationFired.get());
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

    @Test
    public void notificationHasMuleContextRef() throws Exception
    {
        MuleContext ctx = ctxBuilder.buildMuleContext();
        ctx.initialise();
        new DefaultsConfigurationBuilder().configure(ctx);
        
        final AtomicReference<MuleContext> contextFromNotification = new AtomicReference<MuleContext>();
        final AtomicReference<String> resourceId = new AtomicReference<String>();
        MuleContextNotificationListener<MuleContextNotification> listener = 
            new MuleContextNotificationListener<MuleContextNotification>()
        {
            public void onNotification(MuleContextNotification notification)
            {
                contextFromNotification.set(notification.getMuleContext());
                resourceId.set(notification.getResourceIdentifier());
            }
        };
        ctx.registerListener(listener);
        ctx.start();

        assertNotNull(contextFromNotification.get());
        assertSame(ctx, contextFromNotification.get());
        assertEquals(ctx.getConfiguration().getId(), resourceId.get());
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
        public void fireLifecycle(String phase) throws LifecycleException
        {
            appliedLifecyclePhases.add(phase);
            super.fireLifecycle(phase);
        }
    }
    
    static class NotificationListener implements MuleContextNotificationListener<MuleContextNotification>
    {
        final AtomicBoolean startingNotificationFired = new AtomicBoolean(false);
        final AtomicBoolean startedNotificationFired = new AtomicBoolean(false);
        final AtomicBoolean stoppingNotificationFired = new AtomicBoolean(false);
        final AtomicBoolean stoppedNotificationFired = new AtomicBoolean(false);

        public void onNotification(MuleContextNotification notification)
        {
            switch (notification.getAction())
            {
                case MuleContextNotification.CONTEXT_STARTING: 
                    startingNotificationFired.set(true); 
                    break;
                    
                case MuleContextNotification.CONTEXT_STARTED: 
                    startedNotificationFired.set(true); 
                    break;
                    
                case MuleContextNotification.CONTEXT_STOPPING: 
                    stoppingNotificationFired.set(true); 
                    break;
                    
                case MuleContextNotification.CONTEXT_STOPPED: 
                    stoppedNotificationFired.set(true); 
                    break;
            }
        }
    }
    
    @Test(expected=InitialisationException.class)
    public void testIsInValidJdk() throws InitialisationException
    {
    	try
    	{
    		JdkVersionUtils.validateJdk();
    	}
    	catch (RuntimeException e)
    	{
    		fail("Jdk version or vendor is invalid. Update the valid versions");
    	}
    	
    	String javaVersion = System.setProperty("java.version", "1.5.0_12");
    	try
    	{
	    	try
	    	{
	    		JdkVersionUtils.validateJdk();
	    		fail("Test is invalid because the Jdk version or vendor is supposed to now be invalid");
	    	}
	    	catch (RuntimeException e)
	    	{
	    		// expected
	    	}
	    	
	        MuleContext ctx = ctxBuilder.buildMuleContext();
	        assertFalse(ctx.isInitialised());
	        assertFalse(ctx.isInitialising());
	        assertFalse(ctx.isStarted());
	        assertFalse(ctx.isDisposed());
	        assertFalse(ctx.isDisposing());
	
	        ctx.initialise();
    	}
    	finally
    	{
    		System.setProperty("java.version", javaVersion);
    	}
    }
}
