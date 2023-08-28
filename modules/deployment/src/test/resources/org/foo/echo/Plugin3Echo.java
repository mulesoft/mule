/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.foo.echo;

import org.mule.runtime.module.deployment.api.EventCallback;

import org.foo.EchoTest;

public class Plugin3Echo implements EventCallback {

  @Override
  public void eventReceived(String payload) throws Exception {
    new EchoTest().echo(payload);
  }
}
