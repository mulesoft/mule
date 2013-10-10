/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.management.support;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextBuilder;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.context.DefaultMuleContextBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.module.management.agent.JmxAgent;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

            JmxAgent jmxAgent = new JmxAgent();
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
