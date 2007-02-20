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
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.umo.UMODescriptor;
import org.mule.umo.manager.UMOContainerContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.naming.Context;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class JndiContainerContextTestCase extends AbstractContainerContextTestCase
{
    JndiContainerContext context;

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
        context = new JndiContainerContext();
        Map env = new HashMap();
        env.put(Context.INITIAL_CONTEXT_FACTORY, MuleInitialContextFactory.class.getName());
        context.setEnvironment(env);
        context.initialise(managementContext);
        Context ic = context.getContext();
        ic.bind(FruitBowl.class.getName(), new FruitBowl(new Apple(), new Banana()));
        ic.bind(Apple.class.getName(), new Apple());
    }

    public void testExternalUMOReference() throws Exception
    {
        UMOContainerContext ctx = getContainerContext();
        assertNotNull(ctx);

        UMODescriptor descriptor = getTestDescriptor("fruit Bowl", "org.mule.tck.testmodels.fruit.FruitBowl");
        FruitBowl fruitBowl = (FruitBowl)ctx.getComponent(descriptor.getImplementation());

        assertNotNull(fruitBowl);
    }

    /**
     * If no 'environment' is specified, shall use the default jndi config via 'new
     * InitialContext()'.
     */
    public void testDefaultInitialContext() throws Exception
    {
        Context icEnv = context.getContext();
        assertNotNull(icEnv);

        // reset initial context
        context.setEnvironment(null);
        context.setContext(null);
        context.initialise(managementContext);
        Context icDefault = context.getContext();
        assertNotNull(icDefault);
        assertNotSame(icEnv, icDefault);

        // reset initial context (same, but empty map)
        context.setEnvironment(Collections.EMPTY_MAP);
        context.setContext(null);
        context.initialise(managementContext);
        icDefault = context.getContext();
        assertNotNull(icDefault);

        assertNotSame(icEnv, icDefault);
    }
}
