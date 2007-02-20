/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.model;

import org.mule.config.ConfigurationException;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.umo.UMODescriptor;
import org.mule.umo.manager.ObjectNotFoundException;
import org.mule.umo.manager.UMOContainerContext;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractContainerContextTestCase extends AbstractMuleTestCase
{
    public void testContainerContext() throws Exception
    {
        UMOContainerContext container = getContainerContext();
        container.initialise(managementContext);
        assertNotNull(container);

        Object result = null;

        try
        {
            result = container.getComponent(null);
            fail("Should throw ObjectNotFoundException for null key");
        }
        catch (ObjectNotFoundException e)
        {
            // expected
        }

        try
        {
            result = container.getComponent("abcdefg123456!£$%^n");
            fail("Should throw ObjectNotFoundException for a key that doesn't exist");
        }
        catch (ObjectNotFoundException e)
        {
            // expected
        }

        try
        {
            result = container.getComponent(Apple.class);
            assertNotNull("Component should exist in container", result);
        }
        catch (ObjectNotFoundException e)
        {
            fail("Component should exist in the container");
        }
    }

    /**
     * Usage 2: the implementation reference on the descriptor is to a component in
     * the container
     * 
     * @throws Exception
     */
    public void testExternalUMOReference() throws Exception
    {
        UMOContainerContext container = getContainerContext();
        assertNotNull(container);
        container.initialise(managementContext);
        UMODescriptor descriptor = getTestDescriptor("fruit Bowl", "org.mule.tck.testmodels.fruit.FruitBowl");
        descriptor.setContainer("plexus");
        descriptor.initialise(managementContext);
        FruitBowl fruitBowl = (FruitBowl)container.getComponent(descriptor.getImplementation());

        assertNotNull(fruitBowl);
        assertTrue(fruitBowl.hasApple());
        assertTrue(fruitBowl.hasBanana());
    }

    public abstract UMOContainerContext getContainerContext() throws ConfigurationException;

}
