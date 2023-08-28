/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.privileged.object;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import org.mule.runtime.core.api.object.AbstractObjectFactory;
import org.mule.runtime.core.api.object.AbstractObjectFactoryTestCase;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;

public class PrototypeObjectFactoryTestCase extends AbstractObjectFactoryTestCase {

  @Override
  public AbstractObjectFactory getUninitialisedObjectFactory() {
    return new PrototypeObjectFactory();
  }

  @Override
  public void testGetObjectClass() throws Exception {
    PrototypeObjectFactory factory = (PrototypeObjectFactory) getUninitialisedObjectFactory();
    factory.setObjectClass(Object.class);
    ((MuleContextWithRegistry) muleContext).getRegistry().applyProcessorsAndLifecycle(factory);

    assertEquals(Object.class, factory.getObjectClass());
  }

  @Override
  public void testGet() throws Exception {
    PrototypeObjectFactory factory = (PrototypeObjectFactory) getUninitialisedObjectFactory();
    factory.setObjectClass(Object.class);
    ((MuleContextWithRegistry) muleContext).getRegistry().applyProcessorsAndLifecycle(factory);

    assertNotSame(factory.getInstance(muleContext), factory.getInstance(muleContext));
  }

}
