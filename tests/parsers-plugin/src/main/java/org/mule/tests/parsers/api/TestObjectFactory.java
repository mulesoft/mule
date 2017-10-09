/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tests.parsers.api;

import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.dsl.api.component.AbstractComponentFactory;

import java.util.LinkedList;

import javax.inject.Inject;

public class TestObjectFactory extends AbstractComponentFactory<TestObject> {

  private LinkedList<String> lifecycleActions = new LinkedList<>();
  private LinkedList<String> createdObjectLifecycleActions = new LinkedList<>();
  private boolean injectionDoneBeforeGetObject = false;
  @Inject
  private ObjectStoreManager objectStoreManager;

  @Override
  public TestObject doGetObject() throws Exception {
    if (objectStoreManager != null) {
      injectionDoneBeforeGetObject = true;
    }
    return new TestObject(createdObjectLifecycleActions, this);
  }

  public LinkedList<String> getLifecycleActions() {
    return lifecycleActions;
  }

  public boolean isInjectionDoneBeforeGetObject() {
    return injectionDoneBeforeGetObject;
  }
}
