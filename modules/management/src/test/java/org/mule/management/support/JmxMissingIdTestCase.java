/*
 * $Id: JmxSupportTestCase.java 11343 2008-03-13 10:58:26Z tcarlson $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.management.support;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextBuilder;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.context.DefaultMuleContextBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.module.management.agent.JmxAgent;

import junit.framework.TestCase;

public class JmxMissingIdTestCase extends TestCase
{
    MuleContext muleContext;
    
    @Override
    protected void tearDown() throws Exception
    {
        super.tearDown();
        muleContext = null;
    }

    public void testContextIdAndJmxAgentIsOk() throws Exception
    {
        DefaultMuleConfiguration config = new DefaultMuleConfiguration();
        config.setId("MY_SERVER");
        MuleContextBuilder contextBuilder = new DefaultMuleContextBuilder();
        contextBuilder.setMuleConfiguration(config);
        muleContext = new DefaultMuleContextFactory().createMuleContext(contextBuilder);

        muleContext.start();
    }
    
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
