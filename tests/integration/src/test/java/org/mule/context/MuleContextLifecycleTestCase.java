
/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.ApplicationContextBuilder;
import org.mule.test.integration.lifecycle.LifecycleBean;

public class MuleContextLifecycleTestCase extends AbstractMuleContextTestCase
{

    private static final String EXPECTED_A_CONTEXT_START_EXCEPTION_EXCEPTION = "Expected a ContextStartException exception";

    @Test
    public void failOnStartInvokesStopInOtherComponentsButNotInTheFailedOne() throws Exception
    {
        LifecycleBean lifecycleBean = getBeanFromContext("component-failing-during-startup-config.xml");
        LifecycleBean failOnStartLifecycleBean = lifecycleBean.getMuleContext().getRegistry().get("failOnStartLifecycleBean");
        LifecycleBean lifecycleBeanFromMuleContext = lifecycleBean.getMuleContext().getRegistry().get("lifecycleBean");
        lifecycleBean.getMuleContext().dispose();
        assertThat(lifecycleBeanFromMuleContext.getLifecycleInvocations(),
                contains(Initialisable.PHASE_NAME, Startable.PHASE_NAME, Stoppable.PHASE_NAME,
                        Disposable.PHASE_NAME));
        assertThat(failOnStartLifecycleBean.getLifecycleInvocations(),
                contains(Initialisable.PHASE_NAME, Startable.PHASE_NAME,
                        Disposable.PHASE_NAME));
    }
    
    @Test
    public void failOnInitialiseInvokesStopInOtherComponentsButNotInTheFailedOne() throws Exception
    {
        LifecycleBean lifecycleBean = getBeanFromContext("component-failing-during-initialise-config.xml");
        LifecycleBean failOnStartLifecycleBean = lifecycleBean.getMuleContext().getRegistry().get("failOnInitialiseLifecycleBean");
        LifecycleBean lifecycleBeanFromMuleContext = lifecycleBean.getMuleContext().getRegistry().get("lifecycleBean");
        lifecycleBean.getMuleContext().dispose();
        assertThat(lifecycleBeanFromMuleContext.getLifecycleInvocations(),
                contains(Initialisable.PHASE_NAME, Disposable.PHASE_NAME));
        assertThat(failOnStartLifecycleBean.getLifecycleInvocations(),
                contains(Initialisable.PHASE_NAME));
    }

    private LifecycleBean getBeanFromContext(String resource)
    {
        LifecycleBean lifecycleBean = null;
        
        try
        {
            muleContext = new ApplicationContextBuilder().setApplicationResources(new String[] {resource}).build();
            fail(EXPECTED_A_CONTEXT_START_EXCEPTION_EXCEPTION);
        }
        catch (LifecycleException e)
        {
            lifecycleBean = (LifecycleBean) e.getComponent();
        }
        catch (Exception e)
        {
            fail(String.format("Expected a %s exception", LifecycleException.class.getName()));
        }
        return lifecycleBean;
    }
}