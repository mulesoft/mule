/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
