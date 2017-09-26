/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.registry;

import org.mule.runtime.core.api.MuleContext;

import java.util.Collection;

/**
 * A component capable of providing all the registered {@link Registry} instances
 *
 * @since 3.7.0
 */
public interface RegistryProvider {

  /**
   * Returns an immutable view of all active {@link Registry} instances for the current {@link MuleContext}. {@link Collection}s
   * returned by this method will not remain synced with the {@link MuleContext}. If a {@link Registry} is added or removed this
   * {@link Collection} will not be automatically updated.
   */
  Collection<Registry> getRegistries();
}
