/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.api.artifact;

import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.jpms.api.JpmsUtils.createModuleLayer;
import static org.mule.runtime.jpms.api.JpmsUtils.openToModule;

import static java.lang.Boolean.getBoolean;
import static java.lang.System.getProperty;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import org.mule.runtime.container.api.MuleContainerClassLoaderWrapper;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.net.URL;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates {@link ArtifactClassLoader} for service descriptors.
 */
class ServiceModuleLayerFactory extends ServiceClassLoaderFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServiceModuleLayerFactory.class);

  private static final Set<String> SERVICE_MODULE_NAME_PREFIXES =
      new HashSet<>(asList("org.mule.service.",
                           "com.mulesoft.mule.service.",
                           "com.mulesoft.anypoint.gw.service."));
  private static final String SCHEDULER_SERVICE_MODULE_NAME = "org.mule.service.scheduler";

  private static final String CLASSLOADER_SERVICE_JPMS_MODULE_LAYER_DATAWEAVE =
      SYSTEM_PROPERTY_PREFIX + "classloader.service.jpmsModuleLayer.dataWeave";

  private static final class MuleServiceClassLoader extends MuleArtifactClassLoader {

    private MuleServiceClassLoader(String artifactId, ArtifactDescriptor artifactDescriptor, URL[] urls, ClassLoader parent,
                                   ClassLoaderLookupPolicy lookupPolicy) {
      super(artifactId, artifactDescriptor, urls, parent, lookupPolicy);
    }
  }

  private Optional<ModuleLayer> parentLayer = ofNullable(ServiceModuleLayerFactory.class.getModule().getLayer());

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
    if (artifactId.equals("service/DataWeave service")
        && !getBoolean(getProperty(CLASSLOADER_SERVICE_JPMS_MODULE_LAYER_DATAWEAVE, "false"))) {
      return new MuleArtifactClassLoader(artifactId, descriptor, descriptor.getClassLoaderConfiguration().getUrls(), parent,
                                         lookupPolicy);
    }

    LOGGER.debug(" >> Creating ModuleLayer for service: '" + artifactId + "'...");
    ModuleLayer artifactLayer = createModuleLayer(descriptor.getClassLoaderConfiguration().getUrls(), parent,
                                                  parentLayer, true, true);

    String serviceModuleName = artifactLayer.modules()
        .stream()
        .filter(module -> SERVICE_MODULE_NAME_PREFIXES.stream().anyMatch(module.getName()::startsWith))
        .findAny()
        // TODO TD-0144818 TD-0144819 TD-0144821 TD-0144822 TD-0144823 temporarily until all services are properly modularized,
        // This should fail if services do not have the proper module name
        .orElse(artifactLayer.modules().iterator().next())
        .getName();

    if (serviceModuleName.equals(SCHEDULER_SERVICE_MODULE_NAME)) {
      openToModule(artifactLayer,
                   serviceModuleName,
                   "java.base",
                   singletonList("java.lang"));
    }

    return new MuleServiceClassLoader(artifactId,
                                      descriptor,
                                      new URL[0],
                                      artifactLayer.findLoader(serviceModuleName),
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

  @Override
  public void setParentLayerFrom(Class clazz) {
    parentLayer = ofNullable(clazz.getModule().getLayer());
  }

}
