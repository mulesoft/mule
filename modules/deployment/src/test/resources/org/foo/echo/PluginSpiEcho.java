/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.foo.echo;

import org.mule.runtime.module.deployment.api.EventCallback;

import java.util.ServiceLoader;

import org.foo.EchoTest;

public class PluginSpiEcho implements EventCallback {

  @Override
  public void eventReceived(String payload) throws Exception {
    new EchoTest().echo(ServiceLoader.load(org.foo.spi.SpiInterface.class, Thread.currentThread().getContextClassLoader()).iterator().next().value());
  }
}
