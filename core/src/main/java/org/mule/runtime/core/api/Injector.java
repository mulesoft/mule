/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.exception.MuleException;

/**
 * Component capable of injecting dependencies into a given object
 *
 * @since 3.7.0
 */
@NoImplement
public interface Injector {

  /**
   * Injects dependencies into the given object
   *
   * @param object the object on which dependencies are to be injected on
   * @return the injected object or a proxy to it
   * @throws MuleException
   */
  <T> T inject(T object) throws MuleException;
}
