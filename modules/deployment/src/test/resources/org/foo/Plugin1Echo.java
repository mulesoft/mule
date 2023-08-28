/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.foo;

import org.mule.runtime.module.deployment.api.EventCallback;

import org.bar.BarUtils;

public class Plugin1Echo implements EventCallback {

  @Override
  public void eventReceived(String payload) throws Exception {
    new BarUtils().doStuff(payload);
  }
}
