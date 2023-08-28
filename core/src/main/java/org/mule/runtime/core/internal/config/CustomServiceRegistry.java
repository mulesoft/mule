/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.config;

import org.mule.runtime.api.config.custom.CustomizationService;

import java.util.Map;
import java.util.Optional;

public interface CustomServiceRegistry extends CustomizationService {

  /**
   * Provides the configuration of a particular service.
   *
   * @param serviceId identifier of the service.
   * @return the service definition
   */
  Optional<CustomService> getOverriddenService(String serviceId);

  /**
   * Provides access to the custom services defined for the corresponding mule context.
   *
   * @return the registered custom services. Non null.
   */
  Map<String, CustomService> getCustomServices();
}
