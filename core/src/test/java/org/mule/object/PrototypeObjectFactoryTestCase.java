/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.object;

import org.mule.api.object.ObjectFactory;

public class PrototypeObjectFactoryTestCase extends AbstractObjectFactoryTestCase
{

    @Override
    public ObjectFactory getObjectFactory()
    {
        return new PrototypeObjectFactory();
    }

    @Override
    public void testGetObjectClass() throws Exception
    {
        PrototypeObjectFactory factory = (PrototypeObjectFactory) getObjectFactory();
        factory.setObjectClass(Object.class);
        muleContext.getRegistry().applyProcessorsAndLifecycle(factory);
        assertEquals(Object.class, factory.getObjectClass());
    }

    @Override
    public void testGet() throws Exception
    {
        PrototypeObjectFactory factory = (PrototypeObjectFactory) getObjectFactory();
        factory.setObjectClass(Object.class);
        muleContext.getRegistry().applyProcessorsAndLifecycle(factory);
        assertNotSame(factory.getInstance(), factory.getInstance());
    }

}
