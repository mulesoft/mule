/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
