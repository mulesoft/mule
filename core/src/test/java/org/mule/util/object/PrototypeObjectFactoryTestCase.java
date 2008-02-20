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

public class PrototypeObjectFactoryTestCase extends AbstractObjectFactoryTestCase
{

    // @Override
    public ObjectFactory getObjectFactory()
    {
        PrototypeObjectFactory factory = new PrototypeObjectFactory();
        factory.setObjectClass(Object.class);
        return factory;
    }

    // @Override
    public void testGetObjectClass()
    {
        // TODO HH: auto-generated method stub
    }

    // @Override
    public void testGet()
    {
        // TODO HH: auto-generated method stub
    }

}
