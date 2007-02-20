/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.hivemind;

import org.mule.tck.model.AbstractContainerContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.umo.manager.ObjectNotFoundException;
import org.mule.umo.manager.UMOContainerContext;

/**
 * Test case for a context container whose backend is an HiveMind Registry
 * 
 * @author <a href="mailto:mlusetti@gmail.com">Massimo Lusetti</a>
 * @version $Revision$
 */
public class HiveMindContextTestCase extends AbstractContainerContextTestCase
{
    HiveMindContext context;

    protected void doSetUp() throws Exception
    {
        context = new HiveMindContext();
        context.initialise(managementContext);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.model.AbstractComponentResolverTestCase#getConfiguredResolver()
     */
    public UMOContainerContext getContainerContext()
    {
        return context;
    }

    /**
     * Container should never be null
     * 
     * @throws Exception
     */
    public void testContainerNotNull() throws Exception
    {
        assertNotNull(getContainerContext());
    }

    /**
     * Registry should never be null
     */
    public void testRegistryNotNull()
    {
        assertNotNull(context.getRegistry());
    }

    /**
     * Test getting an object before initializing the Registry
     */
    public void testIllegalState()
    {
        try
        {
            HiveMindContext ctx = new HiveMindContext();
            ctx.getComponent(new Object());
            fail();
        }
        catch (IllegalStateException ise)
        {
            // Expected
        }
        catch (ObjectNotFoundException obfe)
        {
            fail("Should throw a IllegalStateExeption instead of ObjectNotFoundException");
        }
    }

    /**
     * When an object is not in the Registry
     */
    public void testObjectNotFound()
    {
        try
        {
            context.getComponent(new String("Not present").getClass());
            fail("Should have thrown ObjectNotFoundException");
        }
        catch (ObjectNotFoundException onfe)
        {
            // Expeced
        }
    }

    /**
     * When an object is not in the Registry caused by a key not recognized
     */
    public void testObjectNotFoundByKeyType()
    {
        try
        {
            context.getComponent(new FruitBowl());
            fail("Should have thrown ObjectNotFoundException");
        }
        catch (ObjectNotFoundException onfe)
        {
            // Expeced
        }
    }

    /**
     * Shouldn't be possibile to get a component after disposing the container
     * 
     * @throws Exception
     */
    public void testDispose() throws Exception
    {
        HiveMindContext context = new HiveMindContext();
        context.initialise(managementContext);
        context.dispose();

        try
        {
            context.getComponent(Class.class);
            fail("Shouldn't be possibile to get a component after disposing the container");
        }
        catch (NullPointerException npe)
        {
            // Expected
        }
    }

    /**
     * Test the real Registry built to serve the correct services
     * 
     * @throws Exception
     */
    public void testFruitBowl() throws Exception
    {
        FruitBowl result = null;
        try
        {
            result = (FruitBowl)context.getComponent(FruitBowl.class.getName());
            assertNotNull("Component FruitBwol should exist in container", result);
            Apple apple = result.getApple();
            assertNotNull("Component Apple should be in FruitBowl", apple);
        }
        catch (ObjectNotFoundException e)
        {
            fail("Component should exist in the container");
        }
    }

}
