/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.foo.echo;

import org.mule.runtime.module.deployment.api.EventCallback;

import org.slf4j.BarUtils;

public class PluginForbiddenMuleThirdPartyEcho implements EventCallback{

  @Override
  public void eventReceived(String payload) throws Exception {
    new BarUtils().doStuff("");
  }
}
