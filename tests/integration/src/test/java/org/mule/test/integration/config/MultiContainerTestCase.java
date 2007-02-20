/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.config;

import org.mule.config.builders.QuickConfigurationBuilder;
import org.mule.impl.container.ContainerKeyPair;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMODescriptor;
import org.mule.umo.manager.ObjectNotFoundException;
import org.mule.umo.manager.UMOContainerContext;

/**
 * Tests Deploying and referencing components from two different spring container
 * contexts
 */
public class MultiContainerTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "org/mule/test/integration/config/multi-container-test.xml";
    }

    public void testContainer() throws Exception
    {
        UMOContainerContext context = managementContext.getRegistry().getContainerContext();
        assertNotNull(context);
        assertNotNull(context.getComponent("spring2-Apple"));
        assertNotNull(context.getComponent("spring-Apple"));
        assertNotNull(context.getComponent("spring2-Banana"));
        assertNotNull(context.getComponent("spring-Banana"));

        try
        {
            context.getComponent("WaterMelon");
            fail("Object should  not found");
        }
        catch (ObjectNotFoundException e)
        {
            // ignore
        }
    }

    public void testSpecificContainerAddressing() throws Exception
    {
        UMOContainerContext context = managementContext.getRegistry().getContainerContext();
        assertNotNull(context);
        Orange o = (Orange)context.getComponent(new ContainerKeyPair("spring1", "Orange"));
        assertNotNull(o);
        assertEquals(new Integer(8), o.getSegments());

        o = (Orange)context.getComponent(new ContainerKeyPair("spring2", "Orange"));
        assertNotNull(o);
        assertEquals(new Integer(10), o.getSegments());

        // gets the component from the first container
        o = (Orange)context.getComponent("Orange");
        assertNotNull(o);
        assertEquals(new Integer(8), o.getSegments());
    }

    public void testSpecificContainerAddressingForComponents() throws Exception
    {
        QuickConfigurationBuilder builder = new QuickConfigurationBuilder();
        UMODescriptor d = builder.createDescriptor("Orange", "myOrange", "test://foo", null, null);
        d.setContainer("spring2");
        builder.registerComponent(d);
        UMOComponent c = builder.getManagementContext().getRegistry().lookupModel("main").getComponent("myOrange");
        assertNotNull(c);
        Object o = c.getInstance();
        assertTrue(o instanceof Orange);
        Orange orange = (Orange)o;
        assertEquals(10, orange.getSegments().intValue());

        d = builder.createDescriptor("Orange", "myOrange2", "test://bar", null, null);
        d.setContainer("spring1");
        builder.registerComponent(d);
        c = builder.getManagementContext().getRegistry().lookupModel("main").getComponent("myOrange2");
        assertNotNull(c);
        o = c.getInstance();
        assertTrue(o instanceof Orange);
        orange = (Orange)o;
        assertEquals(8, orange.getSegments().intValue());

    }
}
