/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk 
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.tck.model;

import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.umo.UMODescriptor;
import org.mule.umo.model.ComponentNotFoundException;
import org.mule.umo.model.UMOContainerContext;

/**
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractComponentResolverTestCase extends AbstractMuleTestCase
{
    public void testContainerContext()
    {
        UMOContainerContext container = getContainerContext();

        assertNotNull(container);

        Object result = null;

        try
        {
            result = container.getComponent(null);
            fail("Should throw ComponentNotFoundException for null key");
        }
        catch (ComponentNotFoundException e)
        {
            // expected
        }

        try
        {
            result = container.getComponent("abcdefg123456!£$%^n");
            fail("Should throw ComponentNotFoundException for a key that doesn't exist");
        }
        catch (ComponentNotFoundException e)
        {
            // expected
        }

        try
        {
            result = container.getComponent(Apple.class);
            assertNotNull("Component should exist in container", result);
        } catch (ComponentNotFoundException e)
        {
            fail("Component should exist in the container");
        }
    }

    /**
     * Usage 2: the implementation reference on the descriptor is to a component in the container
     *
     * @throws Exception
     */
    public void testExternalUMOReference() throws Exception
    {
        UMOContainerContext ctx = getContainerContext();
        assertNotNull(ctx);

        UMODescriptor descriptor = getTestDescriptor("fruit Bowl", "org.mule.tck.testmodels.fruit.FruitBowl");
        FruitBowl fruitBowl = (FruitBowl) ctx.getComponent(descriptor.getImplementation());

        assertNotNull(fruitBowl);
        assertTrue(fruitBowl.hasApple());
        assertTrue(fruitBowl.hasBanana());
    }

    public abstract UMOContainerContext getContainerContext();

}
