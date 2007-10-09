/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.mule.commonspool;

import org.mule.tck.model.AbstractProxyPoolFactoryTestCase;

// TODO Update after MULE-2233
public class CommonsPoolProxyFactoryTestCase extends AbstractProxyPoolFactoryTestCase
{
//    public ObjectFactory getProxyFactory(MuleDescriptor descriptor, ObjectPool pool)
//    {
//        CommonsPoolProxyFactory factory = new CommonsPoolProxyFactory(descriptor, new TestSedaModel());
//        factory.setPool(pool);
//        return factory;
//    }

    public void testLifeCycleMethods() throws Exception
    {
//        Mock mockPool = new Mock(ObjectPool.class);
//        mockPool.expect("onAdd", C.IS_NOT_NULL);
//        mockPool.expect("onRemove", C.IS_NOT_NULL);
//        MuleDescriptor descriptor = getTestDescriptor("apple", Apple.class.getName());
//        CommonsPoolProxyFactory factory = (CommonsPoolProxyFactory)getProxyFactory(descriptor,
//            (ObjectPool)mockPool.proxy());
//
//        assertNotNull(factory);
//
//        Object obj = factory.makeObject();
//        assertNotNull(obj);
//        assertTrue(factory.validateObject(obj));
//        factory.activateObject(obj);
//        factory.passivateObject(obj);
//        factory.destroyObject(obj);
    }
}
