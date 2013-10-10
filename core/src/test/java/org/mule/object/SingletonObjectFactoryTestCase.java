/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.object;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class SingletonObjectFactoryTestCase extends AbstractObjectFactoryTestCase
{

    @Override
    public AbstractObjectFactory getUninitialisedObjectFactory()
    {
        return new SingletonObjectFactory();
    }

    @Override
    public void testGetObjectClass() throws Exception
    {
        SingletonObjectFactory factory = (SingletonObjectFactory) getUninitialisedObjectFactory();
        factory.setObjectClass(Object.class);
        factory.initialise();
        
        assertEquals(Object.class, factory.getObjectClass());
    }

    @Override
    public void testGet() throws Exception
    {
        SingletonObjectFactory factory = (SingletonObjectFactory) getUninitialisedObjectFactory();
        factory.setObjectClass(Object.class);
        factory.initialise();
        
        assertSame(factory.getInstance(muleContext), factory.getInstance(muleContext));
    }

}
