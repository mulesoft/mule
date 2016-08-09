/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.api.listener;

import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.core.api.config.Config;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.api.tls.TlsContextFactory;

public interface HttpListenerConfig extends Config, NamedObject, AnnotatedObject, Startable, Stoppable {

  /**
   * Stops listening for incoming request. No inbound connections will be accepted after stop.
   *
   * @throws MuleException
   */
  void stop() throws MuleException;

  /**
   * Starts the listening for incoming request.
   *
   * @throws MuleException
   */
  void start() throws MuleException;

  /**
   * @return true if the listener config has tls config which also means that the protocol used is https
   */
  boolean hasTlsConfig();

  /**
   * @return the port in which the config is listening for request.
   */
  public int getPort();

  /**
   * @return the host in which the config is listening for request.
   */
  public String getHost();

  /**
   * @return TLS context for this config, or null if TLS is not enabled
   */
  TlsContextFactory getTlsContext();
}
