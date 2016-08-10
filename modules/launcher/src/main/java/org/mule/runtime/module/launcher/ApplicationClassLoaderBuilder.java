/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher;

import static org.mule.runtime.core.util.Preconditions.checkState;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.launcher.application.ArtifactPluginFactory;
import org.mule.runtime.module.launcher.application.MuleApplicationClassLoaderFactory;
import org.mule.runtime.module.launcher.domain.Domain;
import org.mule.runtime.module.launcher.plugin.ArtifactPluginDescriptorLoader;
import org.mule.runtime.module.launcher.plugin.ArtifactPluginRepository;

import java.io.IOException;

/**
 * {@link ArtifactClassLoader} builder for class loaders required by {@link org.mule.runtime.module.launcher.application.Application} artifacts
 *
 * @since 4.0
 */
public class ApplicationClassLoaderBuilder extends AbstractArtifactClassLoaderBuilder<ApplicationClassLoaderBuilder>
{

    private Domain domain;

    /**
     * Creates a new builder for creating {@link org.mule.runtime.module.launcher.application.Application} artifacts.
     * <p>
     * The {@code domainRepository} is used to locate the domain that this application belongs to and the {@code artifactClassLoaderBuilder}
     * is used for building the common parts of artifacts.
     *
     * @param artifactClassLoaderFactory factory for the classloader specific to the artifact resource and classes
     * @param artifactPluginRepository repository of plugins contained by the runtime
     * @param artifactPluginFactory factory for creating artifact plugins
     */
    public ApplicationClassLoaderBuilder(MuleApplicationClassLoaderFactory artifactClassLoaderFactory,
                                         ArtifactPluginRepository artifactPluginRepository,
                                         ArtifactPluginFactory artifactPluginFactory)
    {
        super(artifactClassLoaderFactory, artifactPluginRepository, artifactPluginFactory);
    }

    /**
     * Creates a new {@code ArtifactClassLoader} using the provided configuration. It will create
     * the proper class loader hierarchy and filters so application classes, resources, plugins and it's domain
     * resources are resolve correctly.
     *
     * @return a {@code ArtifactClassLoader} created from the provided configuration.
     * @throws IOException exception cause when it was not possible to access the file provided as dependencies
     */
    public MuleApplicationClassLoader build() throws IOException
    {
        checkState(domain != null, "Domain cannot be null");
        return (MuleApplicationClassLoader) super.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    ArtifactClassLoader getParentClassLoader()
    {
        return this.domain.getArtifactClassLoader();
    }

    /**
     * @param domain the domain artifact to which the application that is going to use this classloader belongs.
     * @return the builder
     */
    public ApplicationClassLoaderBuilder setDomain(Domain domain)
    {
        this.domain = domain;
        return this;
    }

}
