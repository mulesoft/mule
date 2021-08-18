/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.foo;

import static java.lang.Thread.currentThread;

import org.mule.runtime.module.deployment.api.EventCallback;

public class LoadsAppResourceCallback implements EventCallback {

  @Override
  public void eventReceived(String payload) throws Exception {
    ClassLoader tccl = currentThread().getContextClassLoader();

    if (tccl.getResource("test-resource.txt") == null) {
      throw new AssertionError("Couldn't load exported resource");
    }
    if (tccl.getResource("test-resource-not-exported.txt") != null) {
      throw new AssertionError("Could load not exported resource");
    }

    try {
      tccl.loadClass("org.bar.BarUtils");
    } catch (ClassNotFoundException e) {
      throw new AssertionError("Couldn't load exported class", e);
    }
    try {
      tccl.loadClass("org.foo.echo.Plugin2Echo");
      throw new AssertionError("Could load not exported class");
    } catch (ClassNotFoundException e) {
      // expected
    }
  }
}
