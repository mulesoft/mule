/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.component;

import java.util.function.Consumer;

/**
 * Utility to force an indirection in a method call.
 */
public class Delegator implements Consumer<Runnable> {

  @Override
  public void accept(Runnable r) {
    r.run();
  }

}
