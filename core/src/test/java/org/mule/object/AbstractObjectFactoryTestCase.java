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

import org.mule.api.lifecycle.InitialisationException;
import org.mule.tck.AbstractMuleTestCase;

public abstract class AbstractObjectFactoryTestCase extends AbstractMuleTestCase
{

    public void testInitialisationFailureWithoutObjectClass() throws Exception
    {
        AbstractObjectFactory factory = getUninitialisedObjectFactory();

        try
        {
            factory.initialise();
            fail("expected InitialisationException");
        }
        catch (InitialisationException iex)
        {
            // OK
        }
    }
    
    public void testInstanceFailureGetInstanceWithoutObjectClass() throws Exception
    {
        AbstractObjectFactory factory = getUninitialisedObjectFactory();

        try
        {
            factory.getInstance();
            fail("expected InitialisationException");
        }
        catch (InitialisationException iex)
        {
            // OK
        }
    }
    
    public void testCreateWithClassButDoNotInitialise() throws Exception
    {
        AbstractObjectFactory factory = new DummyObjectFactory(Object.class);
        assertObjectClassAndName(factory);
    }
    
    public void testCreateWithClassNameButDoNotInitialise() throws Exception
    {
        AbstractObjectFactory factory = new DummyObjectFactory(Object.class.getName());
        assertObjectClassAndName(factory);
    }
    
    public void testSetObjectClassNameButDoNotInitialise() throws Exception
    {
        AbstractObjectFactory factory = getUninitialisedObjectFactory();
        factory.setObjectClassName(Object.class.getName());

        assertObjectClassAndName(factory);
    }

    public void testSetObjectClassButDoNotInitialise() throws Exception
    {
        AbstractObjectFactory factory = getUninitialisedObjectFactory();
        factory.setObjectClass(Object.class);
        
        assertObjectClassAndName(factory);
    }
    
    private void assertObjectClassAndName(AbstractObjectFactory factory)
    {
        assertEquals(Object.class, factory.getObjectClass());
        assertEquals(Object.class.getName(), factory.getObjectClassName());
    }
    
    public void testInitialiseWithClass() throws Exception
    {
        AbstractObjectFactory factory = getUninitialisedObjectFactory();
        factory.setObjectClass(Object.class);
        // Will init the object        
        muleContext.getRegistry().applyProcessorsAndLifecycle(factory);

        assertNotNull(factory.getInstance());
    }

    public void testInitialiseWithClassName() throws Exception
    {
        AbstractObjectFactory factory = getUninitialisedObjectFactory();
        factory.setObjectClassName(Object.class.getName());
        // Will init the object
        muleContext.getRegistry().applyProcessorsAndLifecycle(factory);
        
        assertNotNull(factory.getInstance());
    }

    public void testDispose() throws Exception
    {
        AbstractObjectFactory factory = getUninitialisedObjectFactory();
        factory.setObjectClass(Object.class);
        // Will init the object
        muleContext.getRegistry().applyProcessorsAndLifecycle(factory);
        
        factory.dispose();
        assertNull(factory.getObjectClass());

        try
        {
            factory.getInstance();
            fail("expected InitialisationException");
        }
        catch (InitialisationException iex)
        {
            // OK
        }
    }
    
    public void testSoftReferenceGetsGarbageCollected() throws Exception
    {
        AbstractObjectFactory factory = getUninitialisedObjectFactory();
        factory.setObjectClass(Object.class);
        // Will init the object
        muleContext.getRegistry().applyProcessorsAndLifecycle(factory);

        // simulate garbage collection
        factory.objectClass.clear();
        
        Object borrowed = factory.getInstance();
        assertNotNull(borrowed);
    }

    public abstract AbstractObjectFactory getUninitialisedObjectFactory();

    public abstract void testGetObjectClass() throws Exception;

    public abstract void testGet() throws Exception;
    
    private static class DummyObjectFactory extends AbstractObjectFactory
    {
        public DummyObjectFactory(String className)
        {
            super(className);
        }
        
        public DummyObjectFactory(Class<?> klass)
        {
            super(klass);
        }
        
        public boolean isAutoWireObject()
        {
            return false;
        }
    }
}
