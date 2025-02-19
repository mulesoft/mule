/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.api.artifact;

import static org.mule.runtime.jpms.api.JpmsUtils.createModuleLayer;
import static org.mule.runtime.jpms.api.JpmsUtils.openToModule;

import static java.lang.ModuleLayer.boot;
import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;

import org.mule.api.annotation.jpms.RequiredOpens;
import org.mule.api.annotation.jpms.ServiceModule;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.container.api.MuleContainerClassLoaderWrapper;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.exception.ArtifactClassloaderCreationException;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.service.internal.artifact.ModuleLayerGraph;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates {@link ArtifactClassLoader} for service descriptors.
 */
class ServiceModuleLayerFactory extends ServiceClassLoaderFactory implements IServiceClassLoaderFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServiceModuleLayerFactory.class);
  private static final String CONTAINER_LAYER_NAME = "Mule Container Module Layer";

  // prefixes of services provided by the Mule Runtime team
  private static final Set<String> MULE_SERVICE_MODULE_NAME_PREFIXES =
      new HashSet<>(asList("org.mule.service.",
                           "com.mulesoft.mule.service."));

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
  @Deprecated(since = "4.6")
  public ArtifactClassLoader create(String artifactId, ServiceDescriptor descriptor, ClassLoader parent,
                                    ClassLoaderLookupPolicy lookupPolicy)
      throws ArtifactClassloaderCreationException {
    try {
      return doCreate(artifactId, descriptor, parent, lookupPolicy);
    } catch (Exception e) {
      throw new ArtifactClassloaderCreationException("Exception creating classloader for service '" + artifactId + "'", e);
    }
  }

  private ArtifactClassLoader doCreate(String artifactId, ServiceDescriptor descriptor, ClassLoader parent,
                                       ClassLoaderLookupPolicy lookupPolicy) {
    URL[] classLoaderConfigurationUrls = descriptor.getClassLoaderConfiguration().getUrls();

    LOGGER.debug(" >> Creating ModuleLayer for service: '{}'...", artifactId);
    ModuleLayer artifactLayer = createModuleLayer(classLoaderConfigurationUrls, parent,
                                                  parentLayer, true, true);

    final Class<? extends Annotation> serviceModuleAnnotationClass = getServiceModuleAnnotationClass(parent);

    final Module serviceModule = artifactLayer.modules()
        .stream()
        .filter(module -> module.isAnnotationPresent(serviceModuleAnnotationClass))
        .findAny()
        .orElseThrow(() -> new NoSuchElementException("No module annotated with 'ServiceModule' for '" + artifactId + "' in "
            + descriptor.getBundleDescriptor().getArtifactFileName()));

    String serviceModuleName = serviceModule.getName();

    // Only for services owned by the Mule Runtime team
    if (MULE_SERVICE_MODULE_NAME_PREFIXES.stream().anyMatch(serviceModuleName::startsWith)) {
      propagateOpensToService(parent, artifactLayer, serviceModuleAnnotationClass, serviceModule, serviceModuleName);
    }

    ArtifactClassLoader serviceClassLoader =
        new MuleServiceClassLoader(artifactId, descriptor, new URL[0], artifactLayer.findLoader(serviceModuleName), lookupPolicy);

    ModuleLayerGraph.setModuleLayerId(artifactLayer, artifactId);
    artifactLayer.parents().stream().filter(parentArtifactLayer -> !parentArtifactLayer.equals(boot())).findFirst()
        .ifPresent(parentArtifactLayer -> ModuleLayerGraph.setModuleLayerId(parentArtifactLayer, CONTAINER_LAYER_NAME));

    ModuleLayerGraph graph = new ModuleLayerGraph(artifactLayer);
    serviceClassLoader.setModuleLayerInformationSupplier(graph);
    if (parent instanceof ArtifactClassLoader parentArtifactClassLoader) {
      parentArtifactClassLoader.setModuleLayerInformationSupplier(graph);
    }
    return serviceClassLoader;
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
                                    MuleContainerClassLoaderWrapper containerClassLoader)
      throws ArtifactClassloaderCreationException {
    return create(artifactId, descriptor,
                  containerClassLoader.getContainerClassLoader().getClassLoader(),
                  containerClassLoader.getContainerClassLoaderLookupPolicy());
  }

  @Override
  public void setParentLayerFrom(Class clazz) {
    parentLayer = ofNullable(clazz.getModule().getLayer());
  }

}
