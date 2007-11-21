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

import org.mule.components.simple.PassThroughComponent;
import org.mule.components.simple.StaticComponent;
import org.mule.config.spring.parsers.specific.CheckExclusiveClassAttributeObjectFactory.CheckExclusiveClassAttributeObjectFactoryException;
import org.mule.routing.nested.NestedRouter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOManagementContext;
import org.mule.util.object.PooledObjectFactory;
import org.mule.util.object.SingletonObjectFactory;

import org.springframework.beans.factory.BeanDefinitionStoreException;

public class ComponentDefinitionParserTestCase extends AbstractMuleTestCase
{

    public void testObjectFactoryComponent() throws Exception
    {
        managementContext = getBuilder().configure("org/mule/config/spring/parsers/specific/component-ok-test.xml");
        UMOComponent component = managementContext.getRegistry().lookupComponent("service");
        validateCorrectComponentCreation(component);
        assertEquals(SingletonObjectFactory.class, component.getServiceFactory().getClass());
        assertEquals(1, component.getNestedRouter().getRouters().size());
    }

    public void testShortcutComponent() throws Exception
    {
        managementContext = getBuilder().configure("org/mule/config/spring/parsers/specific/component-ok-test.xml");
        UMOComponent component = managementContext.getRegistry().lookupComponent("service2");
        validateCorrectComponentCreation(component);
        assertEquals(PooledObjectFactory.class, component.getServiceFactory().getClass());
        assertEquals(2, component.getNestedRouter().getRouters().size());
    }

    public void testClassAttributeAndObjectFactory() throws Exception
    {
        try
        {
            managementContext = getBuilder().configure("org/mule/config/spring/parsers/specific/component-bad-test.xml");
            throw new IllegalStateException("Expected config to fail");
        }
        catch (Exception e)
        {
            assertEquals(BeanDefinitionStoreException.class, e.getClass());
            assertEquals(CheckExclusiveClassAttributeObjectFactoryException.class, e.getCause().getClass());
        }
    }

    protected void validateCorrectComponentCreation(UMOComponent component) throws Exception
    {
        assertNotNull(component);
        assertNotNull(component.getServiceFactory());
        assertFalse(component.getServiceFactory().getOrCreate() instanceof PassThroughComponent);
        assertTrue(component.getServiceFactory().getOrCreate() instanceof StaticComponent);
        assertNotNull(component.getNestedRouter());
        assertTrue(component.getNestedRouter().getRouters().get(0) instanceof NestedRouter);
    }

    protected UMOManagementContext createManagementContext() throws Exception
    {
        return null;
    }

}
