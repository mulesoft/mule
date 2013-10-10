/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.object;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public abstract class AbstractObjectFactoryTestCase extends AbstractMuleContextTestCase
{

    @Test
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
    
    @Test
    public void testInstanceFailureGetInstanceWithoutObjectClass() throws Exception
    {
        AbstractObjectFactory factory = getUninitialisedObjectFactory();

        try
        {
            factory.getInstance(muleContext);
            fail("expected InitialisationException");
        }
        catch (InitialisationException iex)
        {
            // OK
        }
    }
    
    @Test
    public void testCreateWithClassButDoNotInitialise() throws Exception
    {
        AbstractObjectFactory factory = new DummyObjectFactory(Object.class);
        assertObjectClassAndName(factory);
    }
    
    @Test
    public void testCreateWithClassNameButDoNotInitialise() throws Exception
    {
        AbstractObjectFactory factory = new DummyObjectFactory(Object.class.getName());
        assertObjectClassAndName(factory);
    }
    
    @Test
    public void testSetObjectClassNameButDoNotInitialise() throws Exception
    {
        AbstractObjectFactory factory = getUninitialisedObjectFactory();
        factory.setObjectClassName(Object.class.getName());

        assertObjectClassAndName(factory);
    }

    @Test
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
    
    @Test
    public void testInitialiseWithClass() throws Exception
    {
        AbstractObjectFactory factory = getUninitialisedObjectFactory();
        factory.setObjectClass(Object.class);
        // Will init the object        
        muleContext.getRegistry().applyProcessorsAndLifecycle(factory);

        assertNotNull(factory.getInstance(muleContext));
    }

    @Test
    public void testInitialiseWithClassName() throws Exception
    {
        AbstractObjectFactory factory = getUninitialisedObjectFactory();
        factory.setObjectClassName(Object.class.getName());
        // Will init the object
        muleContext.getRegistry().applyProcessorsAndLifecycle(factory);
        
        assertNotNull(factory.getInstance(muleContext));
    }

    @Test
    public void testDispose() throws Exception
    {
        AbstractObjectFactory factory = getUninitialisedObjectFactory();
        factory.setObjectClass(Object.class);
        // Will init the object
        muleContext.getRegistry().applyProcessorsAndLifecycle(factory);
        
        factory.dispose();

        try
        {
            factory.getInstance(muleContext);
            fail("expected InitialisationException");
        }
        catch (InitialisationException iex)
        {
            // OK
        }
    }
    
    public abstract AbstractObjectFactory getUninitialisedObjectFactory();

    @Test
    public abstract void testGetObjectClass() throws Exception;

    @Test
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

    }
}
