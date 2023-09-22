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
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.partitioningBy;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.container.api.MuleContainerClassLoaderWrapper;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates {@link ArtifactClassLoader} for service descriptors.
 */
class ServiceModuleLayerFactory extends ServiceClassLoaderFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServiceModuleLayerFactory.class);

  private static final Set<String> KNOWN_JPMS_INVALID_LIBS =
      new HashSet<>(asList("parboiled_2.12",
                           "scala-parser-combinators_2.12"));

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
    URL[] classLoaderConfigurationUrls = descriptor.getClassLoaderConfiguration().getUrls();

    if (artifactId.equals("service/DataWeave service")) {
      // TODO TD-0144818 remove this special case
      if (!getBoolean(CLASSLOADER_SERVICE_JPMS_MODULE_LAYER_DATAWEAVE)) {
        return new MuleArtifactClassLoader(artifactId, descriptor, descriptor.getClassLoaderConfiguration().getUrls(), parent,
                                           lookupPolicy);
      }

      final Map<Boolean, List<URL>> serviceUrlsByJpmsValidity = Stream.of(classLoaderConfigurationUrls)
          .collect(partitioningBy(url -> {
            final String fileName;
            try {
              fileName = Paths.get(url.toURI()).getFileName().toString();
            } catch (URISyntaxException e) {
              throw new MuleRuntimeException(e);
            }

            return KNOWN_JPMS_INVALID_LIBS.stream().noneMatch(fileName::contains);
          }));

      parent = new URLClassLoader(serviceUrlsByJpmsValidity.get(false).toArray(URL[]::new), parent);
      classLoaderConfigurationUrls = serviceUrlsByJpmsValidity.get(true).toArray(URL[]::new);
    }

    LOGGER.debug(" >> Creating ModuleLayer for service: '" + artifactId + "'...");
    ModuleLayer artifactLayer = createModuleLayer(classLoaderConfigurationUrls, parent,
                                                  parentLayer, true, true);

    final Module serviceModule = artifactLayer.modules()
        .stream()
        .filter(module -> SERVICE_MODULE_NAME_PREFIXES.stream().anyMatch(module.getName()::startsWith))
        .findAny()
        .orElseThrow(() -> new NoSuchElementException("No module with an expected prefix (" + SERVICE_MODULE_NAME_PREFIXES
            + ") for '" + artifactId + "'"));

    String serviceModuleName = serviceModule.getName();
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
