/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.api.artifact;

import static org.mule.runtime.jpms.api.JpmsUtils.openToModule;

import static java.util.Collections.singletonList;

import org.mule.runtime.container.api.ContainerDependantArtifactClassLoaderFactory;
import org.mule.runtime.container.api.MuleContainerClassLoaderWrapper;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.lang.ModuleLayer.Controller;
import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Creates {@link ArtifactClassLoader} for service descriptors.
 */
public class ServiceModuleLayerFactory extends ServiceClassLoaderFactory {

  private static final class MuleServiceClassLoader extends MuleArtifactClassLoader {

    private MuleServiceClassLoader(String artifactId, ArtifactDescriptor artifactDescriptor, URL[] urls, ClassLoader parent,
                                   ClassLoaderLookupPolicy lookupPolicy) {
      super(artifactId, artifactDescriptor, urls, parent, lookupPolicy);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @deprecated since 4.6, use {@link #create(String, ServiceDescriptor, MuleContainerClassLoaderWrapper)}.
   */
  @Override
  @Deprecated
  public ArtifactClassLoader create(String artifactId, ServiceDescriptor descriptor, ClassLoader parent,
                                    ClassLoaderLookupPolicy lookupPolicy) {
    if (artifactId.equals("service/DataWeave service")) {
      return new MuleArtifactClassLoader(artifactId, descriptor, descriptor.getClassLoaderConfiguration().getUrls(), parent,
                                         lookupPolicy);
    }

    Path[] paths = Stream.of(descriptor.getClassLoaderConfiguration().getUrls())
        .map(url -> Paths.get(URI.create(url.toString())))
        .toArray(size -> new Path[size]);

    ModuleFinder serviceModulesFinder = ModuleFinder.of(paths);
    List<String> modules = serviceModulesFinder
        .findAll()
        .stream()
        .map(ModuleReference::descriptor)
        .map(ModuleDescriptor::name)
        .collect(Collectors.toList());
    String serviceModuleName = modules
        .stream()
        .filter(moduleName -> moduleName.startsWith("org.mule.service."))
        .findAny()
        // temporarily until all services are properly modularized
        .orElse(modules.get(0));

    ModuleLayer containerLayer = this.getClass().getModule().getLayer();
    // remove once the container is in its own layer
    if (containerLayer == null) {
      containerLayer = ModuleLayer.boot();
    }


    Configuration configuration = containerLayer.configuration()
        .resolve(serviceModulesFinder, ModuleFinder.ofSystem(), modules);

    Controller defineModulesWithOneLoader = ModuleLayer
        .defineModulesWithOneLoader(configuration, List.of(containerLayer),
                                    parent);

    openToModule(defineModulesWithOneLoader.layer(),
                 serviceModuleName,
                 "java.base",
                 singletonList("java.lang"));

    return new MuleServiceClassLoader(artifactId, descriptor, new URL[0], defineModulesWithOneLoader
        .layer().findLoader(serviceModuleName),
                                      lookupPolicy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ArtifactClassLoader create(String artifactId, ServiceDescriptor descriptor,
                                    MuleContainerClassLoaderWrapper containerClassLoader) {
    return create(artifactId, descriptor,
                  containerClassLoader.getContainerClassLoader().getClassLoader(),
                  containerClassLoader.getContainerClassLoaderLookupPolicy());
  }
}
