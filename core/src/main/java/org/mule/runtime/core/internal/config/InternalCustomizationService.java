/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config;

import org.mule.runtime.api.artifact.ArtifactType;
import org.mule.runtime.api.config.custom.CustomizationService;

import java.util.Map;
import java.util.Optional;

/**
 * Extended {@link CustomizationService} contract with additional non-API behavior.
 */
public interface InternalCustomizationService extends CustomizationService {

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

  /**
   * Provides access to the default services defined for the corresponding mule context.
   *
   * @return the registered default services. Non null.
   */
  Map<String, CustomService> getDefaultServices();

  /**
   * Allows to fine-tune the service customization depending on the properties of the artifact.
   *
   * @param artifactProperties the artifactProperties of the deployment for which services are being customized
   * @since 4.10
   */
  void setArtifactProperties(Map<String, String> artifactProperties);

  /**
   * Allows to fine-tune the service customization depending on the type of the artifact.
   *
   * @param artifactType the type of the deployment for which services are being customized
   * @since 4.10
   */
  void setArtifactType(ArtifactType artifactType);

}
