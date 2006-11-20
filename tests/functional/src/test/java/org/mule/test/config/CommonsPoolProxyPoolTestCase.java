/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.config;

import org.mule.config.pool.CommonsPoolProxyFactory;
import org.mule.config.pool.CommonsPoolProxyPool;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.model.MuleProxy;
import org.mule.tck.AbstractMuleTestCase;

public class CommonsPoolProxyPoolTestCase extends AbstractMuleTestCase
{

    public void testOnRemoveCallsDispose() throws Exception
    {
        MuleDescriptor descriptor = getTestDescriptor("test", "java.lang.Object");
        CommonsPoolProxyFactory factory = new CommonsPoolProxyFactory(descriptor);
        CommonsPoolProxyPool pool = new CommonsPoolProxyPool(descriptor, factory);
        factory.setPool(pool);

        Object obj = factory.makeObject();
        factory.destroyObject(obj);

        // if calling dispose throws an IllegalStateException it means
        // the component has already been disposed by the pool.
        boolean exceptionWasThrown = false;
        try
        {
            ((MuleProxy)obj).dispose();
        }
        catch (IllegalStateException isex)
        {
            assertEquals("Component has already been disposed of", isex.getMessage());
            exceptionWasThrown = true;
        }

        if (!exceptionWasThrown)
        {
            fail("Expected exception has never been thrown. Was the component disposed before?");
        }
    }
}
