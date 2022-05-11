/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.foo.echo;

import org.mule.runtime.module.deployment.api.EventCallback;

import java.lang.BarUtils;

public class PluginForbiddenJavaEcho implements EventCallback{

  @Override
  public void eventReceived(String payload) throws Exception {
    new BarUtils().doStuff("");
  }
}
