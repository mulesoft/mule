/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.work.MuleWorkManager;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DefaultMuleContextBuilderTestCase extends AbstractMuleTestCase
{
    @Test
    public void testBuildMuleContextDefault()
    {
        // Build
        DefaultMuleContextBuilder builder = new DefaultMuleContextBuilder();
        MuleContext muleContext = builder.buildMuleContext();

        // Assert
        assertNotNull(muleContext);
        assertEquals(DefaultMuleConfiguration.class, muleContext.getConfiguration().getClass());
        assertEquals(MuleContextLifecycleManager.class, muleContext.getLifecycleManager().getClass());
        assertEquals(ServerNotificationManager.class, muleContext.getNotificationManager().getClass());
        assertEquals(MuleWorkManager.class, muleContext.getWorkManager().getClass());
    }

    @Test
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
        // subclass just for testing
    }

    static class MyMuleConfiguration extends DefaultMuleConfiguration
    {
        // subclass just for testing
    }

    static class MyServerNotificationManager extends ServerNotificationManager
    {
        // subclass just for testing
    }

    static class MyWorkManager extends MuleWorkManager
    {
        public MyWorkManager(ThreadingProfile profile, String name)
        {
            super(profile, name, 5000);
        }
    }
}
