/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tests.parsers.api;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.dsl.api.component.AbstractAnnotatedObjectFactory;

import java.util.LinkedList;

import javax.inject.Inject;

public class TestObjectFactory extends AbstractAnnotatedObjectFactory<TestObject> implements Lifecycle {

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

  @Override
  public void stop() throws MuleException {
    lifecycleActions.addLast("stop");
  }

  @Override
  public void dispose() {
    lifecycleActions.addLast("dispose");
  }

  @Override
  public void start() throws MuleException {
    lifecycleActions.addLast("start");
  }

  @Override
  public void initialise() throws InitialisationException {
    lifecycleActions.addLast("initialise");
  }

  public LinkedList<String> getLifecycleActions() {
    return lifecycleActions;
  }

  public boolean isInjectionDoneBeforeGetObject() {
    return injectionDoneBeforeGetObject;
  }
}
