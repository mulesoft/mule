/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.artifact;

import java.io.File;

/**
 * Resolver of an artifact's location from a reference.
 * // TODO(fernandezlautaro): MULE-10440 this class should be replaced with org.mule.runtime.module.repository.api.RepositoryService
 * @since 4.0
 */
public interface DependenciesProvider {

  /**
   * Given a reference to an artifact, it will look up the plugin's location and it will return it in a {@link File}.
   *
   * @param artifactName reference to an artifact. Non null. // TODO(fernandezlautaro): MULE-10440 same as the class comment, ideally we should rely in an object like org.mule.runtime.module.repository.api.BundleDescriptor
   * @return an {@link File} pointing to the artifact location's folder (it must be unzipped).
   * @throws DependencyNotFoundException if the dependency to look for wasn't found.
   */
  File resolve(String artifactName);
}
