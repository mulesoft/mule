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
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.partitioningBy;

import org.mule.api.annotation.jpms.RequiredOpens;
import org.mule.api.annotation.jpms.ServiceModule;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.container.api.MuleContainerClassLoaderWrapper;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
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
    }

    LOGGER.debug(" >> Creating ModuleLayer for service: '" + artifactId + "'...");
    ModuleLayer artifactLayer = createModuleLayer(classLoaderConfigurationUrls, parent,
                                                  parentLayer, true, true);

    final Class<? extends Annotation> serviceModuleAnnotationClass = getServiceModuleAnnotationClass(parent);

    final Module serviceModule = artifactLayer.modules()
        .stream()
        .filter(module -> module.isAnnotationPresent(serviceModuleAnnotationClass))
        .findAny()
        .orElseThrow(() -> new NoSuchElementException("No module annotated with 'ServiceModule' for '" + artifactId + "'"));

    String serviceModuleName = serviceModule.getName();

    propagateOpensToService(parent, artifactLayer, serviceModuleAnnotationClass, serviceModule, serviceModuleName);

    return new MuleServiceClassLoader(artifactId,
                                      descriptor,
                                      new URL[0],
                                      artifactLayer.findLoader(serviceModuleName),
                                      lookupPolicy);
  }

  // this relies on reflection because the annotation in the services is loaded with the container classloader, but this code may
  // be running in another classloader.
  private Class<? extends Annotation> getServiceModuleAnnotationClass(ClassLoader parent) {
    try {
      // Use the annotation from the appropriate classloader
      return (Class<? extends Annotation>) parent.loadClass(ServiceModule.class.getName());
    } catch (ClassNotFoundException e) {
      throw new MuleRuntimeException(e);
    }
  }

  // this relies on reflection because the annotation in the services is loaded with the container classloader, but this code may
  // be running in another classloader.
  private void propagateOpensToService(ClassLoader parent,
                                       ModuleLayer artifactLayer,
                                       final Class<? extends Annotation> serviceModuleAnnotationClass,
                                       final Module serviceModule,
                                       String serviceModuleName) {
    try {
      final Class<? extends Annotation> requiredOpensAnnotationClass =
          (Class<? extends Annotation>) parent.loadClass(RequiredOpens.class.getName());
      final Annotation serviceModuleAnnotation = serviceModule.getAnnotation(serviceModuleAnnotationClass);
      final Object[] requiredOpensAll =
          (Object[]) serviceModuleAnnotationClass.getMethod("requiredOpens").invoke(serviceModuleAnnotation);

      for (Object requiredOpens : requiredOpensAll) {
        final String moduleName = (String) requiredOpensAnnotationClass.getMethod("moduleName").invoke(requiredOpens);
        final String[] packageNames = (String[]) requiredOpensAnnotationClass.getMethod("packageNames").invoke(requiredOpens);

        openToModule(artifactLayer,
                     serviceModuleName,
                     moduleName,
                     asList(packageNames));
      }
    } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new MuleRuntimeException(e);
    }
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
