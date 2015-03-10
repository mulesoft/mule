/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.management.support;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextBuilder;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.context.DefaultMuleContextBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.module.management.agent.JmxApplicationAgent;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

public class JmxMissingIdTestCase extends AbstractMuleTestCase
{
    private MuleContext muleContext;

    @Test
    public void testContextIdAndJmxAgentIsOk() throws Exception
    {
        DefaultMuleConfiguration config = new DefaultMuleConfiguration();
        config.setId("MY_SERVER");
        MuleContextBuilder contextBuilder = new DefaultMuleContextBuilder();
        contextBuilder.setMuleConfiguration(config);
        muleContext = new DefaultMuleContextFactory().createMuleContext(contextBuilder);

        muleContext.start();
    }

    @Test
    public void testNoContextIdAndJmxAgentMustFail() throws Exception
    {
        try
        {
            DefaultMuleConfiguration config = new DefaultMuleConfiguration();
            config.setId(null);
            MuleContextBuilder contextBuilder = new DefaultMuleContextBuilder();
            contextBuilder.setMuleConfiguration(config);
            muleContext = new DefaultMuleContextFactory().createMuleContext(contextBuilder);

            JmxApplicationAgent jmxAgent = new JmxApplicationAgent();
            muleContext.getRegistry().registerAgent(jmxAgent);

            muleContext.start();
            
            fail("Should have failed.");
        }
        catch (Exception e)
        {
            // this form makes code coverage happier
            assertTrue(true);
        }
    }
}
