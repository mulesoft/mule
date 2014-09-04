/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.application;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.mule.context.notification.MuleContextNotification;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

/**
 * This tests verifies that the {@link org.mule.module.launcher.application.DefaultMuleApplication}
 * status is set correctly depending on its {@link org.mule.api.MuleContext}'s lifecycle phase
 */
public class DefaultMuleApplicationStatusTestCase extends AbstractMuleContextTestCase
{

    private DefaultMuleApplication application;

    @Override
    protected void doSetUp() throws Exception
    {
        application = new DefaultMuleApplication(null, null, null);
        application.setMuleContext(muleContext);
    }

    @Test
    public void initialState()
    {
        assertStatus(ApplicationStatus.CREATED);
    }

    @Test
    public void initialised()
    {
        // the context was initialised before we gave it to the application, so we need
        // to fire the notification again since the listener wasn't there
        muleContext.fireNotification(new MuleContextNotification(muleContext, MuleContextNotification.CONTEXT_INITIALISED));
        assertStatus(ApplicationStatus.INITIALISED);
    }

    @Test
    public void started() throws Exception
    {
        muleContext.start();
        assertStatus(ApplicationStatus.STARTED);
    }

    @Test
    public void stopped() throws Exception
    {
        muleContext.start();
        muleContext.stop();
        assertStatus(ApplicationStatus.STOPPED);
    }

    @Test
    public void destroyed()
    {
        muleContext.dispose();
        assertStatus(ApplicationStatus.DESTROYED);
    }

    @Test
    public void deploymentFailedOnInit()
    {
        try
        {
            application.init();
            fail("Was expecting init to fail");
        }
        catch (Exception e)
        {
            assertStatus(ApplicationStatus.DEPLOYMENT_FAILED);
        }
    }

    @Test
    public void deploymentFailedOnStart()
    {
        try
        {
            application.start();
            fail("Was expecting start to fail");
        }
        catch (Exception e)
        {
            assertStatus(ApplicationStatus.DEPLOYMENT_FAILED);
        }
    }

    private void assertStatus(ApplicationStatus status)
    {
        assertThat(application.getStatus(), is(status));
    }
}
