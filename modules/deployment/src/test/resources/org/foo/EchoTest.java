/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.foo;

import org.mule.functional.api.component.EventCallback;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.MuleContext;

public class EchoTest implements EventCallback {

  public void eventReceived(CoreEvent event, Object component, MuleContext muleContext) throws Exception {
    // Nothing to do
  }

  public String echo(String data) {
    return "Received: " + data;
  }
}
