/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher;

import static java.lang.String.format;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.launcher.domain.Domain;
import org.mule.runtime.module.launcher.domain.DomainRepository;
import org.mule.runtime.module.launcher.plugin.ArtifactPluginDescriptor;

import java.io.File;
import java.io.IOException;

/**
 * {@link ArtifactClassLoader} builder for class loaders required by {@link org.mule.runtime.module.launcher.application.Application} artifacts
 *
 * @since 4.0
 */
public class ApplicationClassLoaderBuilder
{

    private final ArtifactClassLoaderBuilder artifactClassLoaderBuilder;
    private final DomainRepository domainRepository;
    private String domain;

    /**
     * @param domainRepository repository for domain artifacts
     * @param artifactClassLoaderBuilder base artifact classloader builder used by this application builder
     */
    ApplicationClassLoaderBuilder(DomainRepository domainRepository, ArtifactClassLoaderBuilder artifactClassLoaderBuilder)
    {
        this.domainRepository = domainRepository;
        this.artifactClassLoaderBuilder = artifactClassLoaderBuilder;
    }


    /**
     * @return a {@code ArtifactClassLoader} created from the provided configuration.
     * @throws IOException exception cause when it was not possible to access the file provided as dependencies
     */
    public ArtifactClassLoader build() throws IOException
    {
        ArtifactClassLoader parentClassLoader = null;
        if (domain != null)
        {
            Domain domain = domainRepository.getDomain(this.domain);
            if (domain == null)
            {
                throw new IllegalArgumentException(format("Domain '%s' does not exists", domain));
            }
            parentClassLoader = domain.getArtifactClassLoader();
        }
        return artifactClassLoaderBuilder
                .setParentClassLoader(parentClassLoader)
                .build();
    }

    /**
     * @param artifactId unique identifier for this artifact. For instance, for Applications, it can be the app name.
     * @return the builder
     */
    public ApplicationClassLoaderBuilder setArtifactId(String artifactId)
    {
        artifactClassLoaderBuilder.setArtifactId(artifactId);
        return this;
    }

    /**
     * @param domain the domain name to which the application that is going to use this classloader belongs.
     * @return the builder
     */
    public ApplicationClassLoaderBuilder setDomain(String domain)
    {
        this.domain = domain;
        return this;
    }

    /**
     * @param pluginsSharedLibFolder folder in which libraries shared by the plugins are located
     * @return the builder
     */
    public ApplicationClassLoaderBuilder setPluginsSharedLibFolder(File pluginsSharedLibFolder)
    {
        artifactClassLoaderBuilder.setPluginsSharedLibFolder(pluginsSharedLibFolder);
        return this;
    }

    /**
     * @param plugins set of plugins descriptors that will be used by the application.
     * @return the builder
     */
    public ApplicationClassLoaderBuilder addArtifactPluginDescriptor(ArtifactPluginDescriptor... plugins)
    {
        artifactClassLoaderBuilder.addArtifactPluginDescriptor(plugins);
        return this;
    }

    /**
     * @param artifactDescriptor the descriptor of the artifact for which the class loader is going to be created.
     * @return the builder
     */
    public ApplicationClassLoaderBuilder setArtifactDescriptor(ArtifactDescriptor artifactDescriptor)
    {
        artifactClassLoaderBuilder.setArtifactDescriptor(artifactDescriptor);
        return this;
    }
}
