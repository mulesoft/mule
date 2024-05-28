/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.internal.classloader;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.ModuleLayerInformationSupplier;
import org.mule.runtime.module.artifact.api.classloader.ShutdownListener;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;


public class MulePluginModuleLayerClassLoader implements ArtifactClassLoader {

  private static final Logger LOGGER = getLogger(MulePluginModuleLayerClassLoader.class);

  private static final String RESOURCE_PREFIX = "resource::";

  private final String artifactId;
  private final ArtifactDescriptor artifactDescriptor;
  private final ClassLoader pluginLayerLoader;
  private final ClassLoaderLookupPolicy lookupPolicy;

  private final List<ShutdownListener> shutdownListeners = new ArrayList<>();
  private Optional<ModuleLayerInformationSupplier> moduleLayerInformation = empty();

  public MulePluginModuleLayerClassLoader(String artifactId,
                                          ArtifactDescriptor artifactDescriptor,
                                          ClassLoader pluginLayerLoader,
                                          ClassLoaderLookupPolicy lookupPolicy) {
    this.artifactId = artifactId;
    this.artifactDescriptor = artifactDescriptor;
    this.pluginLayerLoader = pluginLayerLoader;
    this.lookupPolicy = lookupPolicy;
  }

  @Override
  public URL findLocalResource(String resourceName) {
    return null;
  }

  @Override
  public ClassLoaderLookupPolicy getClassLoaderLookupPolicy() {
    return lookupPolicy;
  }

  @Override
  public String getArtifactId() {
    return artifactId;
  }

  @Override
  public <T extends ArtifactDescriptor> T getArtifactDescriptor() {
    return (T) artifactDescriptor;
  }

  @Override
  public URL findResource(String resource) {
    if (resource.startsWith(RESOURCE_PREFIX)) {
      // ?
    }

    return pluginLayerLoader.getResource(resource);
  }

  @Override
  public URL findInternalResource(String resource) {
    return findResource(resource);
  }

  @Override
  public Enumeration<URL> findResources(String name) throws IOException {
    return pluginLayerLoader.getResources(name);
  }

  @Override
  public Class<?> findLocalClass(String name) throws ClassNotFoundException {
    return pluginLayerLoader.loadClass(name);
  }

  @Override
  public Class<?> loadInternalClass(String name) throws ClassNotFoundException {
    return pluginLayerLoader.loadClass(name);
  }

  @Override
  public ClassLoader getClassLoader() {
    return pluginLayerLoader;
  }

  @Override
  public void addShutdownListener(ShutdownListener listener) {
    this.shutdownListeners.add(listener);
  }

  @Override
  public void dispose() {
    shutdownListeners();
  }

  private void shutdownListeners() {
    for (ShutdownListener listener : shutdownListeners) {
      try {
        listener.execute();
      } catch (Exception e) {
        LOGGER.error("Error executing shutdown listener", e);
      }
    }

    // Clean up references to shutdown listeners in order to avoid class loader leaks
    shutdownListeners.clear();
  }

  @Override
  public void setModuleLayerInformationSupplier(ModuleLayerInformationSupplier moduleLayerInformationSupplier) {
    this.moduleLayerInformation = of(moduleLayerInformationSupplier);
  }

  @Override
  public Optional<ModuleLayerInformationSupplier> getModuleLayerInformation() {
    return moduleLayerInformation;
  }

}
