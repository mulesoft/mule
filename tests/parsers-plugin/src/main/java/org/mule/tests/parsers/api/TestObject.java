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
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.component.AbstractComponent;

import java.util.LinkedList;

import javax.inject.Inject;

public class TestObject extends AbstractComponent implements Lifecycle {

  private final TestObjectFactory objectFactory;
  private LinkedList<String> lifecycleActions;
  @Inject
  private LockFactory lockFactory;

  public TestObject(LinkedList<String> lifecycleActions, TestObjectFactory testObjectFactory) {
    this.lifecycleActions = lifecycleActions;
    this.objectFactory = testObjectFactory;
  }

  public TestObjectFactory getObjectFactory() {
    return objectFactory;
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

  public LockFactory getLockFactory() {
    return lockFactory;
  }
}
