/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.model;

import org.mule.config.ConfigurationException;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.umo.UMOComponent;
import org.mule.umo.manager.ObjectNotFoundException;
import org.mule.umo.manager.UMOContainerContext;

public abstract class AbstractContainerContextTestCase extends AbstractMuleTestCase
{

    public void testContainerContext() throws Exception
    {
        UMOContainerContext container = getContainerContext();
        container.initialise();
        assertNotNull(container);

        doNullTest(container);
        doBadKeyTest(container);
        doContentTest(container);
    }

    protected void doNullTest(UMOContainerContext container)
    {
        try
        {
            container.getComponent(null);
            fail("Should throw ObjectNotFoundException for null key");
        }
        catch (ObjectNotFoundException e)
        {
            // expected
        }
    }

    protected void doBadKeyTest(UMOContainerContext container)
    {
        try
        {
            container.getComponent("abcdefg123456!�$%^n");
            fail("Should throw ObjectNotFoundException for a key that doesn't exist");
        }
        catch (ObjectNotFoundException e)
        {
            // expected
        }
    }

    protected void doContentTest(UMOContainerContext container) throws Exception
    {
        Object result = container.getComponent(Apple.class);
        assertNotNull("Component should exist in container", result);
    }

    /**
     * Usage 2: the implementation reference on the descriptor is to a component in
     * the container
     *
     * @throws Exception
     */
    public void testExternalUMOReference() throws Exception
    {
        getAndVerifyExternalReference(getTestComponent("fruit Bowl", FruitBowl.class));
    }

    protected void getAndVerifyExternalReference(UMOComponent component) throws Exception
    {
        verifyExternalReference(getExternalReference(component));
    }

    protected Object getExternalReference(UMOComponent component) throws Exception
    {
        UMOContainerContext container = getContainerContext();
        assertNotNull(container);
        container.initialise();
        component.initialise();
        FruitBowl fruitBowl = (FruitBowl) container.getComponent(getFruitBowlComponentName());
        fail("Need to figure out whether this test is relevant for Mule 2.0");
        // TODO MULE-1908
        return null;
        //return container.getComponent(descriptor.getService());
    }

    protected void verifyExternalReference(Object object)
    {
        FruitBowl fruitBowl = (FruitBowl) object;

        assertNotNull(fruitBowl);
        assertTrue(fruitBowl.hasApple());
        assertTrue(fruitBowl.hasBanana());
    }

    public abstract UMOContainerContext getContainerContext() throws ConfigurationException;

    protected String getFruitBowlComponentName()
    {
        return FruitBowl.class.getName();
    }
}
