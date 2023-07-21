/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.service.api.artifact;

import static org.mule.runtime.jpms.api.JpmsUtils.openToModule;

import static java.lang.ModuleLayer.boot;
import static java.lang.ModuleLayer.defineModulesWithOneLoader;
import static java.lang.module.ModuleFinder.ofSystem;
import static java.nio.file.Paths.get;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.container.api.MuleContainerClassLoaderWrapper;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
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
import java.util.List;
import java.util.stream.Stream;

/**
 * Creates {@link ArtifactClassLoader} for service descriptors.
 */
class ServiceModuleLayerFactory extends ServiceClassLoaderFactory {

  private static final String SERVICE_MODULE_NAME_PREFIX = "org.mule.service.";
  private static final String SCHEDULER_SERVICE_MODULE_NAME = "org.mule.service.scheduler";

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
    // TODO TD-0144818 remove this special case
    if (artifactId.equals("service/DataWeave service")) {
      return new MuleArtifactClassLoader(artifactId, descriptor, descriptor.getClassLoaderConfiguration().getUrls(), parent,
                                         lookupPolicy);
    }

    Path[] paths = Stream.of(descriptor.getClassLoaderConfiguration().getUrls())
        .map(url -> get(URI.create(url.toString())))
        .toArray(size -> new Path[size]);

    ModuleFinder serviceModulesFinder = ModuleFinder.of(paths);
    List<String> modules = serviceModulesFinder
        .findAll()
        .stream()
        .map(ModuleReference::descriptor)
        .map(ModuleDescriptor::name)
        .collect(toList());
    String serviceModuleName = modules
        .stream()
        .filter(moduleName -> moduleName.startsWith(SERVICE_MODULE_NAME_PREFIX))
        .findAny()
        // TODO TD-0144818 TD-0144819 TD-0144821 TD-0144822 TD-0144823 temporarily until all services are properly modularized,
        // This should fail if services do not have the proper module name
        .orElse(modules.get(0));

    ModuleLayer containerLayer = this.getClass().getModule().getLayer();
    // TODO W-13151134 remove once the container is in its own layer
    if (containerLayer == null) {
      containerLayer = boot();
    }

    Configuration configuration = containerLayer.configuration()
        .resolve(serviceModulesFinder, ofSystem(), modules);
    Controller defineModulesWithOneLoader = defineModulesWithOneLoader(configuration,
                                                                       singletonList(containerLayer),
                                                                       parent);

    if (serviceModuleName.equals(SCHEDULER_SERVICE_MODULE_NAME)) {
      openToModule(defineModulesWithOneLoader.layer(),
                   serviceModuleName,
                   "java.base",
                   singletonList("java.lang"));
    }

    return new MuleServiceClassLoader(artifactId,
                                      descriptor,
                                      new URL[0],
                                      defineModulesWithOneLoader.layer().findLoader(serviceModuleName),
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
