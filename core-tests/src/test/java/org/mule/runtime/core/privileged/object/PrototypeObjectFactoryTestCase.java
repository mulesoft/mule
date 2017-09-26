/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.object;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import org.mule.runtime.core.api.object.AbstractObjectFactory;
import org.mule.runtime.core.api.object.AbstractObjectFactoryTestCase;
import org.mule.runtime.core.internal.context.MuleContextWithRegistries;

public class PrototypeObjectFactoryTestCase extends AbstractObjectFactoryTestCase {

  @Override
  public AbstractObjectFactory getUninitialisedObjectFactory() {
    return new PrototypeObjectFactory();
  }

  @Override
  public void testGetObjectClass() throws Exception {
    PrototypeObjectFactory factory = (PrototypeObjectFactory) getUninitialisedObjectFactory();
    factory.setObjectClass(Object.class);
    ((MuleContextWithRegistries) muleContext).getRegistry().applyProcessorsAndLifecycle(factory);

    assertEquals(Object.class, factory.getObjectClass());
  }

  @Override
  public void testGet() throws Exception {
    PrototypeObjectFactory factory = (PrototypeObjectFactory) getUninitialisedObjectFactory();
    factory.setObjectClass(Object.class);
    ((MuleContextWithRegistries) muleContext).getRegistry().applyProcessorsAndLifecycle(factory);

    assertNotSame(factory.getInstance(muleContext), factory.getInstance(muleContext));
  }

}
