/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.api.manager;

import org.mule.runtime.api.service.Service;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * Provides access to the actual service wrapped within a {@link Proxy}.
 *
 * @since 4.5
 */
public interface ServiceProxyInvocationHandler extends InvocationHandler {

  /**
   * @return the proxied service.
   */
  public abstract Service getService();

}
