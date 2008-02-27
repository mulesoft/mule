/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.object;

public class SingletonObjectFactoryTestCase extends AbstractObjectFactoryTestCase
{

    // @Override
    public ObjectFactory getObjectFactory()
    {
        SingletonObjectFactory factory = new SingletonObjectFactory();
        factory.setObjectClass(Object.class);
        return factory;
    }

    // @Override
    public void testGetObjectClass() throws Exception
    {
        factory.initialise();
        assertEquals(Object.class, factory.getObjectClass());
    }

    // @Override
    public void testGet() throws Exception
    {
        factory.initialise();
        assertSame(factory.getInstance(), factory.getInstance());
    }

}
