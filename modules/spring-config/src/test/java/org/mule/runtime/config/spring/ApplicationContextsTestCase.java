/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.context.DefaultMuleContextFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.tck.testmodels.fruit.Seed;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ApplicationContextsTestCase extends AbstractMuleTestCase
{

    private static final int INIT_WAIT_TIMEOUT_MILLIS = 5000;
    private MuleContext context;

    @After
    public void stopContext()
    {
        if (context != null && !context.isDisposed())
        {
            context.dispose();
            context = null;
        }
    }

    @Test
    public void testSanity() throws Exception
    {
        ApplicationContext appContext = new ClassPathXmlApplicationContext("application-context.xml");

        Object orange = appContext.getBean("orange");
        assertNotNull(orange);
        assertTrue(orange instanceof Orange);

        try
        {
            appContext.getBean("plum");
            fail("Bean should not have been found");
        }
        catch (NoSuchBeanDefinitionException e)
        {
            // expected
        }
    }

    /**
     * Test that an existing appContext can be added to Mule's internal Registries
     */
    @Test
    public void testSpringConfigurationBuilder() throws Exception
    {
        context = new DefaultMuleContextFactory().createMuleContext();

        ApplicationContext appContext = new ClassPathXmlApplicationContext("application-context.xml");
        ConfigurationBuilder builder = new SpringConfigurationBuilder(appContext);
        builder.configure(context);

        context.start();

        Object orange = context.getRegistry().lookupObject("orange");
        assertNotNull(orange);
        assertTrue(orange instanceof Orange);
        assertEquals("Pirulo", ((Orange) orange).getBrand());
    }

    @Test
    public void springConfigurationBuilderCircularRefs() throws Exception
    {
        context = new DefaultMuleContextFactory().createMuleContext();

        ApplicationContext appContext = new ClassPathXmlApplicationContext("application-context-circular-ref.xml");
        ConfigurationBuilder builder = new SpringConfigurationBuilder(appContext);
        builder.configure(context);

        context.start();

        Object seed = context.getRegistry().lookupObject("seed");
        ((Seed) seed).awaitInitialize(INIT_WAIT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

        Object apple = context.getRegistry().lookupObject("apple");
        assertThat(apple, not(nullValue()));
        assertThat(apple, instanceOf(Apple.class));
        assertThat(((Apple) apple).getSeed(), sameInstance(seed));
    }

    /**
     * Test that the same bean from the 2nd appContext will have precedence over the 1st appContext
     */
    @Test
    public void testSpringConfigurationBuilderPrecedence() throws Exception
    {
        context = new DefaultMuleContextFactory().createMuleContext();

        ApplicationContext appContext = new ClassPathXmlApplicationContext("application-context.xml");
        ConfigurationBuilder builder = new SpringConfigurationBuilder(appContext);
        builder.configure(context);

        appContext = new ClassPathXmlApplicationContext("application-context-2.xml");
        builder = new SpringConfigurationBuilder(appContext);
        builder.configure(context);

        context.start();

        Object orange = context.getRegistry().lookupObject("orange");
        assertNotNull(orange);
        assertTrue(orange instanceof Orange);
        assertEquals("Tropicana", ((Orange) orange).getBrand());
    }

    @Test
    public void testSpringConfigurationBuilderBackwardsPrecedence() throws Exception
    {
        context = new DefaultMuleContextFactory().createMuleContext();

        ApplicationContext appContext = new ClassPathXmlApplicationContext("application-context-2.xml");
        ConfigurationBuilder builder = new SpringConfigurationBuilder(appContext);
        builder.configure(context);

        appContext = new ClassPathXmlApplicationContext("application-context.xml");
        builder = new SpringConfigurationBuilder(appContext);
        builder.configure(context);

        context.start();

        Object orange = context.getRegistry().lookupObject("orange");
        assertNotNull(orange);
        assertTrue(orange instanceof Orange);
        assertEquals("Pirulo", ((Orange) orange).getBrand());
    }

    /**
     * Test that an existing appContext can be used as a parent AppContext for Mule
     */
    @Test
    public void testParentContext() throws Exception
    {
        context = new DefaultMuleContextFactory().createMuleContext();

        ApplicationContext appContext = new ClassPathXmlApplicationContext("application-context.xml");

        SpringXmlConfigurationBuilder builder = new SpringXmlConfigurationBuilder("mule-config.xml");
        builder.setParentContext(appContext);
        builder.configure(context);

        context.start();

        Object orange = context.getRegistry().lookupObject("orange");
        assertNotNull(orange);
        assertTrue(orange instanceof Orange);
        assertEquals("Pirulo", ((Orange) orange).getBrand());
    }

    /**
     * Test the most common approach: Create the Spring config + Mule config in a single AppContext.
     */
    @Test
    public void testAppContextTogetherWithMuleConfig() throws Exception
    {
        context = new DefaultMuleContextFactory().createMuleContext();

        SpringXmlConfigurationBuilder builder = new SpringXmlConfigurationBuilder(new String[] {"application-context.xml", "mule-config.xml"});
        builder.configure(context);

        context.start();

        Object orange = context.getRegistry().lookupObject("orange");
        assertNotNull(orange);
        assertTrue(orange instanceof Orange);
        assertEquals("Pirulo", ((Orange) orange).getBrand());
    }
}
