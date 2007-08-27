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

import org.mule.impl.MuleDescriptor;
import org.mule.impl.model.MuleProxy;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.util.object.ObjectFactory;
import org.mule.util.object.ObjectPool;

import com.mockobjects.dynamic.Mock;

public abstract class AbstractProxyPoolFactoryTestCase extends AbstractMuleTestCase
{
    public void testCreateProxyFromFactory() throws Exception
    {
        Mock mockPool = new Mock(ObjectPool.class);
        MuleDescriptor descriptor = getTestDescriptor("apple", Apple.class.getName());
        ObjectFactory factory = getProxyFactory(descriptor, (ObjectPool) mockPool.proxy());
        Object result = factory.create();
        assertNotNull(result);
        MuleProxy proxy = (MuleProxy) result;
        assertEquals("apple", proxy.getDescriptor().getName());
        mockPool.verify();
    }

    public abstract ObjectFactory getProxyFactory(MuleDescriptor descriptor, ObjectPool pool);
}
