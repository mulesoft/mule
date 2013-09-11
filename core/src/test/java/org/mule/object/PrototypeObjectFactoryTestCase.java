/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.object;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public class PrototypeObjectFactoryTestCase extends AbstractObjectFactoryTestCase
{

    @Override
    public AbstractObjectFactory getUninitialisedObjectFactory()
    {
        return new PrototypeObjectFactory();
    }

    @Override
    public void testGetObjectClass() throws Exception
    {
        PrototypeObjectFactory factory = (PrototypeObjectFactory) getUninitialisedObjectFactory();
        factory.setObjectClass(Object.class);
        muleContext.getRegistry().applyProcessorsAndLifecycle(factory);
        
        assertEquals(Object.class, factory.getObjectClass());
    }

    @Override
    public void testGet() throws Exception
    {
        PrototypeObjectFactory factory = (PrototypeObjectFactory) getUninitialisedObjectFactory();
        factory.setObjectClass(Object.class);
        muleContext.getRegistry().applyProcessorsAndLifecycle(factory);
        
        assertNotSame(factory.getInstance(muleContext), factory.getInstance(muleContext));
    }

}
