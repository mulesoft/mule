/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.tck.model;

import com.mockobjects.dynamic.Mock;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.model.MuleProxy;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.util.ObjectFactory;
import org.mule.util.ObjectPool;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
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
