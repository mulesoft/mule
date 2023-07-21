/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.internal.util;

import java.util.concurrent.locks.ReentrantLock;

/**
 *
 */
public class DebuggableReentrantLock extends ReentrantLock {

  public DebuggableReentrantLock() {}

  public DebuggableReentrantLock(boolean fair) {
    super(fair);
  }

  @Override
  public Thread getOwner() {
    return super.getOwner();
  }
}
