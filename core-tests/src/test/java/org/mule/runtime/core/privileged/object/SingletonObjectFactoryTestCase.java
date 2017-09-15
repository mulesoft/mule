/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.object;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import org.mule.runtime.core.api.object.AbstractObjectFactory;
import org.mule.runtime.core.api.object.AbstractObjectFactoryTestCase;

public class SingletonObjectFactoryTestCase extends AbstractObjectFactoryTestCase {

  @Override
  public AbstractObjectFactory getUninitialisedObjectFactory() {
    return new SingletonObjectFactory();
  }

  @Override
  public void testGetObjectClass() throws Exception {
    SingletonObjectFactory factory = (SingletonObjectFactory) getUninitialisedObjectFactory();
    factory.setObjectClass(Object.class);
    factory.initialise();

    assertEquals(Object.class, factory.getObjectClass());
  }

  @Override
  public void testGet() throws Exception {
    SingletonObjectFactory factory = (SingletonObjectFactory) getUninitialisedObjectFactory();
    factory.setObjectClass(Object.class);
    factory.initialise();

    assertSame(factory.getInstance(muleContext), factory.getInstance(muleContext));
  }

}
