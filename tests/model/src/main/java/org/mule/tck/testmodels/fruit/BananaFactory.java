/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.testmodels.fruit;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.lifecycle.InitialisationCallback;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.object.ObjectFactory;

/**
 * <code>BananaFactory</code> is a test factory that creates Bananas
 */
public class BananaFactory implements ObjectFactory {

  @Override
  public void initialise() throws InitialisationException {
    // nothing to do
  }

  @Override
  public void dispose() {
    // nothing to do
  }

  @Override
  public Object getInstance(MuleContext muleContext) throws Exception {
    return new Banana();
  }

  @Override
  public Class<?> getObjectClass() {
    return Banana.class;
  }

  @Override
  public void addObjectInitialisationCallback(InitialisationCallback callback) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isSingleton() {
    return false;
  }

  @Override
  public boolean isExternallyManagedLifecycle() {
    return false;
  }

  @Override
  public boolean isAutoWireObject() {
    return false;
  }
}
