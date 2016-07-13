/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher;

import org.mule.runtime.module.launcher.domain.DomainRepository;

/**
 * Factory to create instances of {@code ApplicationClassLoaderBuilder}.
 *
 * @since 4.0
 */
public class ApplicationClassLoaderBuilderFactory
{

    private final ArtifactClassLoaderBuilderFactory artifactClassLoaderBuilderFactory;
    private DomainRepository domainRepository;

    /**
     * @param artifactClassLoaderBuilderFactory  class loader builder for common artifacts.
     * @param domainRepository repository of domain artifacts.
     */
    public ApplicationClassLoaderBuilderFactory(ArtifactClassLoaderBuilderFactory artifactClassLoaderBuilderFactory, DomainRepository domainRepository)
    {
        this.artifactClassLoaderBuilderFactory = artifactClassLoaderBuilderFactory;
        this.domainRepository = domainRepository;
    }

    /**
     * @return a {@code ApplicationClassLoaderBuilder} instance.
     */
    public ApplicationClassLoaderBuilder createArtifactClassLoaderBuilder()
    {
        return new ApplicationClassLoaderBuilder(domainRepository, artifactClassLoaderBuilderFactory.createArtifactClassLoaderBuilder());
    }

}
