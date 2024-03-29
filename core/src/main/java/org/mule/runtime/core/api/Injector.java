/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
