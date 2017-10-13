/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.foo;

import static java.lang.Thread.currentThread;

import org.mule.functional.api.component.EventCallback;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;

public class LoadsAppResourceCallback implements EventCallback {

  @Override
  public void eventReceived(CoreEvent event, Object component, MuleContext muleContext) throws Exception {
    ClassLoader tccl = currentThread().getContextClassLoader();

    if (tccl.getResource("test-resource.txt") == null) {
      throw new AssertionError("Couldn't load exported resource");
    }
    if (tccl.getResource("test-resource-not-exported.txt") != null) {
      throw new AssertionError("Could load not exported resource");
    }

    try {
      tccl.loadClass("org.bar1.BarUtils");
    } catch (ClassNotFoundException e) {
      throw new AssertionError("Couldn't load exported class", e);
    }
    try {
      tccl.loadClass("org.bar2.BarUtils");
      throw new AssertionError("Could load not exported class");
    } catch (ClassNotFoundException e) {
      // expected
    }
  }
}
