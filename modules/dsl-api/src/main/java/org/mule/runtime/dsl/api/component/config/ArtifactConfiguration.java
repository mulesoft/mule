/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.api.component.config;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;

/**
 * An {@code ArtifactConfiguration} is a programmatic descriptor of a mule configuration.
 *
 * @since 4.0
 */
//TODO MULE-11496 Delete this configuration once everything has an ExtensionModel and can be represented with ArtifactDeclaration
public class ArtifactConfiguration {

  private List<ComponentConfiguration> componentConfiguration = new ArrayList<>();

  /**
   * Creates an {@code ArtifactConfiguration} from a collection of root mule configuration components.
   *
   * Each {@link ComponentConfiguration} may have other nested configuration components.
   *
   * @param componentConfigurations collection of root configuration elements of a mule configuration. Non null.
   */
  public ArtifactConfiguration(List<ComponentConfiguration> componentConfigurations) {
    checkArgument(componentConfigurations != null, "Component configurations cannot be null");
    this.componentConfiguration = componentConfigurations;
  }

  private ArtifactConfiguration() {}

  /**
   * @return collection of root mule configuration elements.
   */
  public List<ComponentConfiguration> getComponentConfiguration() {
    return componentConfiguration;
  }
}
