/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.container;

import org.mule.impl.jndi.MuleInitialContextFactory;
import org.mule.tck.model.AbstractContainerContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.umo.UMODescriptor;
import org.mule.umo.manager.ObjectNotFoundException;
import org.mule.umo.manager.UMOContainerContext;

import java.util.HashMap;
import java.util.Map;

import javax.naming.Context;

public class EjbContainerContextTestCase extends AbstractContainerContextTestCase
{
    public static final String EJB_NAME = "DummyEjb";

    EjbContainerContext context;

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.model.AbstractComponentResolverTestCase#getConfiguredResolver()
     */
    public UMOContainerContext getContainerContext()
    {
        return context;
    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    protected void doSetUp() throws Exception
    {
        context = new EjbContainerContext();
        Map env = new HashMap();
        env.put(Context.INITIAL_CONTEXT_FACTORY, MuleInitialContextFactory.class.getName());
        context.setEnvironment(env);
        // context.setSecurityPolicy("open-security.policy");
        context.initialise();
        Context ic = context.getContext();
        ic.bind(EJB_NAME, new DummyEjbHomeProxy());
        ic.bind(Apple.class.getName(), new Apple());
    }

    public void testContainerContext() throws Exception
    {
        UMOContainerContext container = getContainerContext();
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
            result = container.getComponent(EJB_NAME);
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
        container.initialise();
        UMODescriptor descriptor = getTestDescriptor("some Ejb service", EJB_NAME);
        DummyEjbBean dummyEjbBean = (DummyEjbBean)container.getComponent(descriptor.getImplementation());

        assertNotNull(dummyEjbBean);
    }

    public void testInvalidObjectLookup() throws Exception
    {
        UMOContainerContext container = getContainerContext();
        container.initialise();
        assertNotNull(container);

        try
        {
            container.getComponent(Apple.class.getName());
            fail("Should throw ObjectNotFoundException for non-ejb object");
        }
        catch (ObjectNotFoundException e)
        {
            // expected
        }
    }
}
