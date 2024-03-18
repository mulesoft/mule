/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.boot.commons.internal;

import org.mule.runtime.module.boot.api.MuleContainer;

/**
 * A factory for {@link MuleContainer} instances. Responsible for choosing the right implementation class and setting up its
 * {@link ClassLoader}.
 *
 * @since 4.6
 */
public interface MuleContainerFactory {

  /**
   * Creates the {@link MuleContainer} instance.
   *
   * @param args Any arguments to forward to the Container (that have not been yet processed by the bootstrapping application).
   * @return A new {@link MuleContainer} instance.
   * @throws Exception If there is any problem creating the {@link MuleContainer} instance. The bootstrapping application should
   *                   exit immediately.
   */
  MuleContainer create(String[] args) throws Exception;
}
