/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring;

import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Orange;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ApplicationContextsTestCase extends AbstractMuleTestCase
{
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
    public void testSpringConfigurationBuilder() throws Exception
    {
        MuleContext muleContext = new DefaultMuleContextFactory().createMuleContext();
        
        ApplicationContext appContext = new ClassPathXmlApplicationContext("application-context.xml");
        ConfigurationBuilder builder = new SpringConfigurationBuilder(appContext);
        builder.configure(muleContext); 

        muleContext.start();
        
        Object orange = muleContext.getRegistry().lookupObject("orange");
        assertNotNull(orange);
        assertTrue(orange instanceof Orange);
        assertEquals("Pirulo", ((Orange) orange).getBrand());
    }

    /** 
     * Test that the same bean from the 2nd appContext will have precedence over the 1st appContext 
     */
    public void testSpringConfigurationBuilderPrecedence() throws Exception
    {
        MuleContext muleContext = new DefaultMuleContextFactory().createMuleContext();
        
        ApplicationContext appContext = new ClassPathXmlApplicationContext("application-context.xml");
        ConfigurationBuilder builder = new SpringConfigurationBuilder(appContext);
        builder.configure(muleContext); 

        appContext = new ClassPathXmlApplicationContext("application-context-2.xml");
        builder = new SpringConfigurationBuilder(appContext);
        builder.configure(muleContext); 

        muleContext.start();
        
        Object orange = muleContext.getRegistry().lookupObject("orange");
        assertNotNull(orange);
        assertTrue(orange instanceof Orange);
        assertEquals("Tropicana", ((Orange) orange).getBrand());
    }

    public void testSpringConfigurationBuilderBackwardsPrecedence() throws Exception
    {
        MuleContext muleContext = new DefaultMuleContextFactory().createMuleContext();
        
        ApplicationContext appContext = new ClassPathXmlApplicationContext("application-context-2.xml");
        ConfigurationBuilder builder = new SpringConfigurationBuilder(appContext);
        builder.configure(muleContext); 

        appContext = new ClassPathXmlApplicationContext("application-context.xml");
        builder = new SpringConfigurationBuilder(appContext);
        builder.configure(muleContext); 

        muleContext.start();
        
        Object orange = muleContext.getRegistry().lookupObject("orange");
        assertNotNull(orange);
        assertTrue(orange instanceof Orange);
        assertEquals("Pirulo", ((Orange) orange).getBrand());
    }

    /** 
     * Test that the same bean from the TransientRegistry will have precedence over the 1st appContext 
     */
    public void testTransientRegistryPrecedence() throws Exception
    {
        MuleContext muleContext = new DefaultMuleContextFactory().createMuleContext();
        
        muleContext.getRegistry().registerObject("orange", new Orange(12, 5.5, "Tutti Frutti"));
        
        ApplicationContext appContext = new ClassPathXmlApplicationContext("application-context.xml");
        ConfigurationBuilder builder = new SpringConfigurationBuilder(appContext);
        builder.configure(muleContext); 

        muleContext.start();
        
        Object orange = muleContext.getRegistry().lookupObject("orange");
        assertNotNull(orange);
        assertTrue(orange instanceof Orange);
        assertEquals("Tutti Frutti", ((Orange) orange).getBrand());
    }

    /** 
     * Test that an existing appContext can be used as a parent AppContext for Mule 
     */
    public void testParentContext() throws Exception
    {
        MuleContext muleContext = new DefaultMuleContextFactory().createMuleContext();
        
        ApplicationContext appContext = new ClassPathXmlApplicationContext("application-context.xml");

        SpringXmlConfigurationBuilder builder = new SpringXmlConfigurationBuilder("mule-config.xml");
        builder.setParentContext(appContext);
        builder.configure(muleContext); 

        muleContext.start();
        
        Object orange = muleContext.getRegistry().lookupObject("orange");
        assertNotNull(orange);
        assertTrue(orange instanceof Orange);
        assertEquals("Pirulo", ((Orange) orange).getBrand());
    }

    /**
     * Test the most common approach: Create the Spring config + Mule config in a single AppContext.
     */
    public void testAppContextTogetherWithMuleConfig() throws Exception
    {
        MuleContext muleContext = new DefaultMuleContextFactory().createMuleContext();
        
        SpringXmlConfigurationBuilder builder = new SpringXmlConfigurationBuilder("application-context.xml, mule-config.xml");
        builder.configure(muleContext); 

        muleContext.start();
        
        Object orange = muleContext.getRegistry().lookupObject("orange");
        assertNotNull(orange);
        assertTrue(orange instanceof Orange);
        assertEquals("Pirulo", ((Orange) orange).getBrand());
    }
}
