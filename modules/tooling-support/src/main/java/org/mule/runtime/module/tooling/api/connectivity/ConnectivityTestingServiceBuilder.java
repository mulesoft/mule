/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.api.connectivity;

import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.api.connectivity.ConnectivityTestingService;

/**
 * Builder for creating a {@code ConnectivityTestingService} from a set of extensions and an {@code ArtifactConfiguration} that
 * describes a set of mule components.
 *
 * @since 4.0
 */
public interface ConnectivityTestingServiceBuilder {

  /**
   * Adds a dependency needed by the artifact that must be included in order to do connectivity testing.
   * <p>
   * If the dependency is a regular jar file, it will be made available to all extensions since the only possible jar dependency
   * that may be added are specific clients jar for doing connectivity testing like jdbc drivers or JMS clients.
   *
   * @param groupId group id of the artifact
   * @param artifactId artifact id of the artifact
   * @param artifactVersion version of the artifact
   * @param classifier classifier of the artifact
   * @param type type of the artifact
   * @return the builder
   */
  ConnectivityTestingServiceBuilder addDependency(String groupId, String artifactId, String artifactVersion, String classifier,
                                                  String type);

  /**
   * Configures the mule components required to do connectivity testing
   *
   * @param artifactDeclaration set of mule components required to do connectivity testing
   * @return the builder
   */
  ConnectivityTestingServiceBuilder setArtifactDeclaration(ArtifactDeclaration artifactDeclaration);

  /**
   * Creates a {@code ConnectivityTestingService} with the provided configuration
   *
   * @return the connectivity testing service
   */
  ConnectivityTestingService build();

}
