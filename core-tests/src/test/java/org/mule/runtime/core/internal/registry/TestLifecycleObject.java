/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.registry;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.core.api.MuleContext;

import javax.inject.Inject;

public class TestLifecycleObject implements Lifecycle {

  private int initialise = 0;
  private int start = 0;
  private int stop = 0;
  private int dispose = 0;

  @Inject
  private ObjectStoreManager objectStoreManager;

  @Inject
  private MuleContext muleContext;

  @Override
  public void initialise() throws InitialisationException {
    initialise++;
  }

  @Override
  public void start() throws MuleException {
    start++;
  }

  @Override
  public void stop() throws MuleException {
    stop++;
  }

  @Override
  public void dispose() {
    dispose++;
  }

  public int getInitialise() {
    return initialise;
  }

  public int getStart() {
    return start;
  }

  public int getStop() {
    return stop;
  }

  public int getDispose() {
    return dispose;
  }

  public ObjectStoreManager getObjectStoreManager() {
    return objectStoreManager;
  }

  public MuleContext getMuleContext() {
    return muleContext;
  }
}
