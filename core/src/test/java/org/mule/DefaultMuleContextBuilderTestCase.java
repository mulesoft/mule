/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

import org.mule.api.MuleContext;
import org.mule.api.config.ThreadingProfile;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.context.DefaultMuleContextBuilder;
import org.mule.context.notification.ServerNotificationManager;
import org.mule.lifecycle.MuleContextLifecycleManager;
import org.mule.lifecycle.phases.MuleContextDisposePhase;
import org.mule.lifecycle.phases.MuleContextInitialisePhase;
import org.mule.lifecycle.phases.MuleContextStartPhase;
import org.mule.lifecycle.phases.MuleContextStopPhase;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.work.MuleWorkManager;

public class DefaultMuleContextBuilderTestCase extends AbstractMuleTestCase
{

    public void testBuildMuleContextDefault()
    {
        // Build
        DefaultMuleContextBuilder builder = new DefaultMuleContextBuilder();
        MuleContext muleContext = builder.buildMuleContext();

        // Assert
        assertNotNull(muleContext);
        assertEquals(DefaultMuleConfiguration.class, muleContext.getConfiguration().getClass());
        assertEquals(MuleContextLifecycleManager.class, muleContext.getLifecycleManager().getClass());
        assertEquals(MuleContextInitialisePhase.class, muleContext.getLifecycleManager().getLifecyclePairs().get(0).getBegin().getClass());
        assertEquals(MuleContextStartPhase.class, muleContext.getLifecycleManager().getLifecyclePairs().get(1).getBegin().getClass());
        assertEquals(MuleContextStopPhase.class, muleContext.getLifecycleManager().getLifecyclePairs().get(1).getEnd().getClass());
        assertEquals(MuleContextDisposePhase.class, muleContext.getLifecycleManager().getLifecyclePairs().get(0).getEnd().getClass());

        assertEquals(ServerNotificationManager.class, muleContext.getNotificationManager().getClass());
        assertEquals(MuleWorkManager.class, muleContext.getWorkManager().getClass());
    }

    public void testBuildMuleContextCustom()
    {
        // Build
        DefaultMuleContextBuilder builder = new DefaultMuleContextBuilder();
        builder.setMuleConfiguration(new MyMuleConfiguration());
        builder.setLifecycleManager(new MyLifeCycleManager());
        builder.setNotificationManager(new MyServerNotificationManager());
        builder.setWorkManager(new MyWorkManager(ThreadingProfile.DEFAULT_THREADING_PROFILE, "test"));
        MuleContext muleContext = builder.buildMuleContext();

        // Assert
        assertNotNull(muleContext);
        assertEquals(MyMuleConfiguration.class, muleContext.getConfiguration().getClass());
        assertEquals(MyLifeCycleManager.class, muleContext.getLifecycleManager().getClass());
        assertEquals(MyServerNotificationManager.class, muleContext.getNotificationManager().getClass());
        assertEquals(MyWorkManager.class, muleContext.getWorkManager().getClass());
    }

    /**
     * Override, we don't want a {@link MuleContext} created for this test case.
     */
    protected MuleContext createMuleContext() throws Exception
    {
        return null;
    }

    static class MyLifeCycleManager extends MuleContextLifecycleManager
    {
    }

    static class MyMuleConfiguration extends DefaultMuleConfiguration
    {
    }

    static class MyServerNotificationManager extends ServerNotificationManager
    {
    }

    static class MyWorkManager extends MuleWorkManager
    {
        public MyWorkManager(ThreadingProfile profile, String name)
        {
            super(profile, name, 5000);
        }
    }

}
