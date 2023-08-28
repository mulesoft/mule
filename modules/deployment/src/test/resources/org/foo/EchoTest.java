/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.foo;

import org.mule.runtime.module.deployment.api.EventCallback;

public class EchoTest implements EventCallback {

  public void eventReceived(String payload) throws Exception {
    // Nothing to do
  }

  public String echo(String data) {
    return "Received: " + data;
  }
}
