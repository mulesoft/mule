/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.mule.api.registry.Registry;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.services.UniqueComponent;

import org.junit.Test;

public class ObjectFactoryTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/components/object-factory-functional-test.xml";
    }

    @Test
    public void testDefaultScope() throws Exception
    {
        Registry registry = muleContext.getRegistry();
        
        Object bean1 = registry.lookupObject("default");
        assertNotNull(bean1);
        String id1 = ((UniqueComponent) bean1).getId();
        
        Object bean2 = registry.lookupObject("default");
        assertNotNull(bean2);
        String id2 = ((UniqueComponent) bean2).getId();
        
        assertEquals(id1, id2);
    }

    @Test
    public void testSingletonScope() throws Exception
    {
        Registry registry = muleContext.getRegistry();
        
        Object bean1 = registry.lookupObject("singleton");
        assertNotNull(bean1);
        String id1 = ((UniqueComponent) bean1).getId();
        
        Object bean2 = registry.lookupObject("singleton");
        assertNotNull(bean2);
        String id2 = ((UniqueComponent) bean2).getId();
        
        assertEquals(id1, id2);
    }

    @Test
    public void testPrototypeScope() throws Exception
    {
        Registry registry = muleContext.getRegistry();
        
        Object bean1 = registry.lookupObject("prototype");
        assertNotNull(bean1);
        String id1 = ((UniqueComponent) bean1).getId();
        
        Object bean2 = registry.lookupObject("prototype");
        assertNotNull(bean2);
        String id2 = ((UniqueComponent) bean2).getId();
        
        assertFalse("IDs " + id1 + " and " + id2 + " should be different", id1.equals(id2));
    }

}


