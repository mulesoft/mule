/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific;

import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.config.ConfigurationException;
import org.mule.api.context.MuleContextFactory;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.service.Service;
import org.mule.component.simple.PassThroughComponent;
import org.mule.component.simple.StaticComponent;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.config.spring.parsers.specific.CheckExclusiveClassAttributeObjectFactory.CheckExclusiveClassAttributeObjectFactoryException;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.routing.nested.DefaultNestedRouter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.util.object.PooledObjectFactory;
import org.mule.util.object.SingletonObjectFactory;

import org.springframework.beans.factory.BeanDefinitionStoreException;

public class ComponentDefinitionParserTestCase extends AbstractMuleTestCase
{

    private MuleContextFactory muleContextFactory = new DefaultMuleContextFactory();

    public void testObjectFactoryComponent() throws Exception
    {
        ConfigurationBuilder configBuilder = new SpringXmlConfigurationBuilder(
            "org/mule/config/spring/parsers/specific/component-ok-test.xml");
        muleContext = muleContextFactory.createMuleContext(configBuilder);
        Service service = muleContext.getRegistry().lookupService("service");
        validateCorrectServiceCreation(service);
        assertEquals(SingletonObjectFactory.class, service.getComponentFactory().getClass());
        assertEquals(1, service.getNestedRouter().getRouters().size());
    }

    public void testShortcutComponent() throws Exception
    {
        ConfigurationBuilder configBuilder = new SpringXmlConfigurationBuilder(
            "org/mule/config/spring/parsers/specific/component-ok-test.xml");
        muleContext = muleContextFactory.createMuleContext(configBuilder);
        Service service = muleContext.getRegistry().lookupService("service2");
        validateCorrectServiceCreation(service);
        assertEquals(PooledObjectFactory.class, service.getComponentFactory().getClass());
        assertEquals(2, service.getNestedRouter().getRouters().size());
    }

    public void testClassAttributeAndObjectFactory() throws Exception
    {
        try
        {
            ConfigurationBuilder configBuilder = new SpringXmlConfigurationBuilder(
                "org/mule/config/spring/parsers/specific/component-bad-test.xml");
            muleContextFactory.createMuleContext(configBuilder);
            throw new IllegalStateException("Expected config to fail");
        }
        catch (Exception e)
        {
            assertEquals(ConfigurationException.class, e.getClass());
            assertEquals(InitialisationException.class, e.getCause().getClass());
            assertEquals(BeanDefinitionStoreException.class, e.getCause().getCause().getClass());
            assertEquals(CheckExclusiveClassAttributeObjectFactoryException.class, 
                e.getCause().getCause().getCause().getClass());
        }
    }

    protected void validateCorrectServiceCreation(Service service) throws Exception
    {
        assertNotNull(service);
        assertNotNull(service.getComponentFactory());
        assertFalse(service.getComponentFactory().getInstance() instanceof PassThroughComponent);
        assertTrue(service.getComponentFactory().getInstance() instanceof StaticComponent);
        assertNotNull(service.getNestedRouter());
        assertTrue(service.getNestedRouter().getRouters().get(0) instanceof DefaultNestedRouter);
    }

    protected MuleContext createMuleContext() throws Exception
    {
        return null;
    }

}
