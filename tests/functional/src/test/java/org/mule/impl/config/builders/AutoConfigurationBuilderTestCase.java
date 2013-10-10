/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.impl.config.builders;

import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.config.ConfigurationException;
import org.mule.api.context.MuleContextFactory;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.builders.AutoConfigurationBuilder;
import org.mule.config.builders.SimpleConfigurationBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.mule.TestExceptionStrategy;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
        MessagingExceptionHandler es = muleContext.getRegistry().lookupModel("main").getExceptionListener();
        assertNotNull(es);
        assertTrue(es instanceof TestExceptionStrategy);
    }

    // @Test
    //public void testConfigureGroovy()
    // {
    // // TODO
    // }
    //
    // @Test
    //public void testConfigureGalaxySpring()
    // {
    // // TODO
    // }
    //
    // @Test
    //public void testConfigureUnkownExtension() throws ConfigurationException
    // {
    // ConfigurationBuilder configurationBuilder = new AutoConfigurationBuilder("my.dtd");
    //
    // try
    // {
    // configurationBuilder.configure(muleContext);
    // }
    // catch (ConfigurationException ce)
    // {
    // assertEquals(
    // "No suitable configuration builder for resource \"my.dtd\" found. Check you have configuration module
    // ion your classpath and are using correct file extension.",
    // ce.getCause().getMessage());
    // }
    // catch (Exception e)
    // {
    //            fail("Exception unexpected:" + e);
    //        }
    //    }
}
