/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extras.spring;

import org.mule.config.ConfigurationBuilder;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Orange;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringMultipleConfigs2871TestCase extends FunctionalTestCase
{
    public String getConfigResources()
    {
        return "mule-config-with-spring-imports.xml";
    }

    public void testAdditionalSpringConfigs() throws Exception
    {
        Apple apple = (Apple) managementContext.getRegistry().lookupObject("apple");
        assertNotNull("Bean 'apple' should exist in container", apple);
        assertTrue("Apple object is misconfigured", apple.isWashed());

        Orange orange = (Orange) managementContext.getRegistry().lookupObject("orange");
        assertNotNull("Bean 'orange' should exist in container", orange);
        assertEquals("Orange object is misconfigured", "mule", orange.getBrand());
    }

    protected ConfigurationBuilder getBuilder() throws Exception
    {
        ApplicationContext ctx = new ClassPathXmlApplicationContext("test-application-context.xml");
        return new SpringXmlConfigurationBuilder(ctx);
    }
}
