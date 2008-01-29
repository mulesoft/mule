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

import org.mule.api.config.ConfigurationException;
import org.mule.api.context.ContainerContext;
import org.mule.api.context.ObjectNotFoundException;
import org.mule.api.service.Service;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.FruitBowl;

public abstract class AbstractContainerContextTestCase extends AbstractMuleTestCase
{

    public void testContainerContext() throws Exception
    {
        ContainerContext container = getContainerContext();
        container.initialise();
        assertNotNull(container);

        doNullTest(container);
        doBadKeyTest(container);
        doContentTest(container);
    }

    protected void doNullTest(ContainerContext container)
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

    protected void doBadKeyTest(ContainerContext container)
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

    protected void doContentTest(ContainerContext container) throws Exception
    {
        Object result = container.getComponent(Apple.class);
        assertNotNull("Service should exist in container", result);
    }

    /**
     * Usage 2: the implementation reference on the descriptor is to a service in
     * the container
     *
     * @throws Exception
     */
    public void testExternalUMOReference() throws Exception
    {
        getAndVerifyExternalReference(getTestService("fruit Bowl", FruitBowl.class));
    }

    protected void getAndVerifyExternalReference(Service service) throws Exception
    {
        verifyExternalReference(getExternalReference(service));
    }

    protected Object getExternalReference(Service service) throws Exception
    {
        ContainerContext container = getContainerContext();
        assertNotNull(container);
        container.initialise();
        service.initialise();
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

    public abstract ContainerContext getContainerContext() throws ConfigurationException;

    protected String getFruitBowlComponentName()
    {
        return FruitBowl.class.getName();
    }
}
