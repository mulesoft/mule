/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl.config.builders;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.config.ConfigurationException;
import org.mule.api.context.MuleContextFactory;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.builders.AutoConfigurationBuilder;
import org.mule.config.builders.SimpleConfigurationBuilder;
import org.mule.construct.Flow;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public class AutoConfigurationBuilderTestCase extends AbstractMuleContextTestCase
{

    protected MuleContext createMuleContext() throws Exception
    {
        MuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
        return muleContextFactory.createMuleContext(new SimpleConfigurationBuilder(null));
    }

    @Test
    public void testConfigureSpring() throws ConfigurationException, InitialisationException
    {
        ConfigurationBuilder configurationBuilder = new AutoConfigurationBuilder(
            "org/mule/test/spring/config1/test-xml-mule2-config.xml");
        configurationBuilder.configure(muleContext);

        // Just a few of the asserts from AbstractConfigBuilderTestCase
        Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("appleComponent");
        assertNotNull(flow.getExceptionListener());
        assertTrue(flow.getExceptionListener() instanceof MessagingExceptionHandler);
    }

}
