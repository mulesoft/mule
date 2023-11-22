/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.config;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.core.api.MuleContext;

import java.util.Map;
import java.util.Optional;

/**
 * Configures the elements of the {@link Registry} of a {@link MuleContext}.
 *
 * @since 4.6.0
 */
public interface ConfigurationBuilderRegistryFilter {

  /**
   * Filters every object added to the target {@link MuleContext}'s registry, allowing customization when needed.
   *
   * @param serviceId   the ID of the object being added to the registry.
   * @param serviceImpl the object being added to the registry.
   * @return the object to add to the registry, or {@link Optional#empty()} if no implementation should be added for the given
   *         {@code serviceId}.
   */
  Optional<Object> filterRegisterObject(String serviceId, Object serviceImpl);

  /**
   * @param muleContext the target {@link MuleContext}.
   * @return a {@link Map} with additional objects to add to the target {@link MuleContext}'s registry and their corresponding
   *         IDs.
   */
  Map<String, Object> getAdditionalObjectsToRegister(MuleContext muleContext);

}
