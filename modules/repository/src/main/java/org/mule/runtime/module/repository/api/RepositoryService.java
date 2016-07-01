/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.repository.api;

import java.io.File;

/**
 * Service that provides access to bundle. A bundle may be a jar, zip or any type supported type by the platform.
 *
 * The repository may access just the local file system or an external service to download the bundles. The service will
 * use a local file system repository in order to avoid download bundles that have already being downloaded or pre-installed
 * in the server.
 *
 *
 *
 * @since 4.0
 */
public interface RepositoryService
{

    /**
     * System property key to specify a custom repository folder. By default the container will use $MULE_HOME/lib/repository
     */
    String MULE_REPOSITORY_FOLDER_PROPERTY = "mule.repository.folder";

    /**
     * System property key to specify the remote repositories to use. Multiple values must be comma separated.
     *
     * If no value is provided then the repository will be disabled causing a {@code RepositoryServiceDisabledException}
     * if any method is called.
     */
    String MULE_REMOTE_REPOSITORIES_PROPERTY = "mule.repository.repositories";


    /**
     * Finds a bundle in the the local repository or any of the external repositories configured.
     *
     * If the bundle does not exists in the local repository but was found in an external repository then it will
     * be stored in the local repositories to avoid a remote fetch if the bundle is requested again.
     *
     * @param bundleDescriptor descriptor to identify the bundle
     * @return a {@code File} where the bundle is stored in the local repository
     * @throws BundleNotFoundException when the bundle could not be located in any of the configured repositories.
     * @throws RepositoryConnectionException when there was a problem connecting to one of the external repositories.
     * @throws RepositoryServiceDisabledException when the repository service has not been properly configured.
     */
    File lookupBundle(BundleDescriptor bundleDescriptor) throws BundleNotFoundException, RepositoryConnectionException, RepositoryServiceDisabledException;

}
