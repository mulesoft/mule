/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.repository.api;

import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;

import java.io.File;

/**
 * Service that provides access to bundles. A bundle may be a jar, zip or any type supported type by the platform.
 * <p>
 * The repository may access just the local file system or an external service to download the bundles. The service will use a
 * local file system repository in order to avoid download bundles that have already being downloaded or pre-installed in the
 * server.
 *
 * @since 4.0
 */
public interface RepositoryService {

  /**
   * Finds a bundle in the the local repository or any of the external repositories configured.
   * <p>
   * If the bundle does not exists in the local repository but was found in an external repository then it will be stored in the
   * local repositories to avoid a remote fetch if the bundle is requested again.
   *
   * @param bundleDependency descriptor to identify the bundle
   * @return a {@code File} where the bundle is stored in the local repository
   * @throws BundleNotFoundException when the bundle could not be located in any of the configured repositories.
   * @throws RepositoryConnectionException when there was a problem connecting to one of the external repositories.
   * @throws RepositoryServiceDisabledException when the repository service has not been properly configured.
   */
  File lookupBundle(BundleDependency bundleDependency);



}
