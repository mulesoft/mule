/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.resources;

import org.mule.api.registry.ServiceRegistry;
import org.mule.extensions.introspection.Extension;
import org.mule.extensions.resources.GenerableResource;
import org.mule.extensions.resources.ResourcesGenerator;
import org.mule.extensions.resources.spi.GenerableResourceContributor;

import com.google.common.collect.ImmutableList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Base implementation of {@link ResourcesGenerator}
 * that takes care of the basic contract except for actually writing the resources to
 * a persistent store. Implementations are only required to provide that piece of logic
 * by using the {@link #write(GenerableResource)}
 * template method
 *
 * @since 3.7.0
 */
public abstract class AbstractResourcesGenerator implements ResourcesGenerator
{

    private Map<String, GenerableResource> resources = new HashMap<>();
    private ServiceRegistry serviceRegistry;

    public AbstractResourcesGenerator(ServiceRegistry serviceRegistry)
    {
        this.serviceRegistry = serviceRegistry;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GenerableResource getOrCreateResource(String filepath)
    {
        GenerableResource resource = resources.get(filepath);

        if (resource == null)
        {
            resource = new ImmutableGenerableResource(filepath);
            resources.put(filepath, resource);
        }

        return resource;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void generateFor(Extension extension)
    {
        for (GenerableResourceContributor contributor : serviceRegistry.lookupProviders(GenerableResourceContributor.class, getClass().getClassLoader()))
        {
            contributor.contribute(extension, this);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<GenerableResource> dumpAll()
    {
        ImmutableList.Builder<GenerableResource> generatedResources = ImmutableList.builder();
        for (GenerableResource resource : resources.values())
        {
            generatedResources.add(resource);
            write(resource);
        }

        return generatedResources.build();
    }

    /**
     * Template method to actually write the given
     * {@code resource} to a persistent store
     *
     * @param resource a non null {@link GenerableResource}
     */
    protected abstract void write(GenerableResource resource);
}
