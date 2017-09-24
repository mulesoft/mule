/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.object;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

public abstract class AbstractObjectFactoryTestCase extends AbstractMuleContextTestCase {

  @Test
  public void testInitialisationFailureWithoutObjectClass() throws Exception {
    AbstractObjectFactory factory = getUninitialisedObjectFactory();
    addMockComponentLocation(factory);

    try {
      factory.initialise();
      fail("expected InitialisationException");
    } catch (InitialisationException iex) {
      // OK
    }
  }

  @Test
  public void testInstanceFailureGetInstanceWithoutObjectClass() throws Exception {
    AbstractObjectFactory factory = getUninitialisedObjectFactory();
    addMockComponentLocation(factory);

    try {
      factory.getInstance(muleContext);
      fail("expected InitialisationException");
    } catch (InitialisationException iex) {
      // OK
    }
  }

  @Test
  public void testCreateWithClassButDoNotInitialise() throws Exception {
    AbstractObjectFactory factory = new DummyObjectFactory(Object.class);
    assertObjectClassAndName(factory);
  }

  @Test
  public void testCreateWithClassNameButDoNotInitialise() throws Exception {
    AbstractObjectFactory factory = new DummyObjectFactory(Object.class.getName());
    assertObjectClassAndName(factory);
  }

  @Test
  public void testSetObjectClassNameButDoNotInitialise() throws Exception {
    AbstractObjectFactory factory = getUninitialisedObjectFactory();
    factory.setObjectClassName(Object.class.getName());

    assertObjectClassAndName(factory);
  }

  @Test
  public void testSetObjectClassButDoNotInitialise() throws Exception {
    AbstractObjectFactory factory = getUninitialisedObjectFactory();
    factory.setObjectClass(Object.class);

    assertObjectClassAndName(factory);
  }

  private void assertObjectClassAndName(AbstractObjectFactory factory) {
    assertEquals(Object.class, factory.getObjectClass());
    assertEquals(Object.class.getName(), factory.getObjectClassName());
  }

  @Test
  public void testInitialiseWithClass() throws Exception {
    AbstractObjectFactory factory = getUninitialisedObjectFactory();
    factory.setObjectClass(Object.class);
    // Will init the object
    ((MuleContextWithRegistries) muleContext).getRegistry().applyProcessorsAndLifecycle(factory);

    assertNotNull(factory.getInstance(muleContext));
  }

  @Test
  public void testInitialiseWithClassName() throws Exception {
    AbstractObjectFactory factory = getUninitialisedObjectFactory();
    factory.setObjectClassName(Object.class.getName());
    // Will init the object
    ((MuleContextWithRegistries) muleContext).getRegistry().applyProcessorsAndLifecycle(factory);

    assertNotNull(factory.getInstance(muleContext));
  }

  @Test
  public void testDispose() throws Exception {
    AbstractObjectFactory factory = getUninitialisedObjectFactory();
    addMockComponentLocation(factory);
    factory.setObjectClass(Object.class);
    // Will init the object
    ((MuleContextWithRegistries) muleContext).getRegistry().applyProcessorsAndLifecycle(factory);

    factory.dispose();

    try {
      factory.getInstance(muleContext);
      fail("expected InitialisationException");
    } catch (InitialisationException iex) {
      // OK
    }
  }

  public abstract AbstractObjectFactory getUninitialisedObjectFactory();

  @Test
  public abstract void testGetObjectClass() throws Exception;

  @Test
  public abstract void testGet() throws Exception;

  private static class DummyObjectFactory extends AbstractObjectFactory {

    public DummyObjectFactory(String className) {
      super(className);
    }

    public DummyObjectFactory(Class<?> klass) {
      super(klass);
    }

  }
}
