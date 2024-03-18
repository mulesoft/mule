/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.foo.echo;

import org.mule.runtime.module.deployment.test.api.EventCallback;

import org.mule.runtime.api.util.BarUtils;

public class PluginForbiddenMuleContainerEcho implements EventCallback{

  @Override
  public void eventReceived(String payload) throws Exception {
    new BarUtils().doStuff("");
  }
}
