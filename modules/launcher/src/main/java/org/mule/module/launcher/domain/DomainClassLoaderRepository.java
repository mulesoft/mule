/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.domain;

import org.mule.module.launcher.artifact.ArtifactClassLoader;

/**
 * Repository for domain class loaders.
 *
 * Keeps track of the different domain class loader so it can be retrieve
 * to be set as parent class loader of mule app class loaders
 */
public interface DomainClassLoaderRepository
{

    /**
     * @param domain name of the domain owner of the class loader
     * @return the ArtifactClassLoader used by the domain.
     */
    ArtifactClassLoader getDomainClassLoader(String domain);

    /**
     * @return the ArtifactClassLoader used by the default domain.
     */
    ArtifactClassLoader getDefaultDomainClassLoader();
}
