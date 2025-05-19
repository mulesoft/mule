/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.classloader;

import static org.mule.runtime.api.util.MuleSystemProperties.MULE_LOG_VERBOSE_CLASSLOADING;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.module.artifact.api.classloader.BlockingLoggerResolutionClassRegistry.getLoggerClassRegistry;

import static java.lang.Boolean.valueOf;
import static java.lang.Integer.toHexString;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.lang.System.identityHashCode;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.enumeration;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.api.annotation.NoInstantiate;
import org.mule.runtime.module.artifact.api.classloader.exception.NotExportedClassException;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines a {@link ClassLoader} that filter which classes and resources can be resolved based on a {@link ClassLoaderFilter}
 * <p/>
 * Resources used to provide SPI are not managed as standard resources, ie, not filtered through the {@link ClassLoaderFilter},
 * but filtered using {@link ExportedService} definitions. Only the service providers defined as exported in the modules will be
 * available from this class loader.
 */
@NoInstantiate
public class FilteringArtifactClassLoader extends ClassLoader implements ArtifactClassLoader {

  static {
    registerAsParallelCapable();
    getLoggerClassRegistry().registerClassNeedingBlockingLoggerResolution(FilteringArtifactClassLoader.class);
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(FilteringArtifactClassLoader.class);
  private static final String SERVICE_PREFIX = "META-INF/services/";

  private final ArtifactClassLoader artifactClassLoader;
  private final ClassLoaderFilter filter;
  private final List<ExportedService> exportedServices;
  private Optional<ModuleLayerInformationSupplier> moduleLayerInformation = empty();

  private final boolean verboseLogging;

  /**
   * Creates a new filtering classLoader
   *
   * @param parent              The parent class loader
   * @param artifactClassLoader artifact classLoader to filter. Non null
   * @param filter              filters access to classes and resources from the artifact classLoader. Non null
   * @param exportedServices    service providers that will be available from the filtered class loader. Non null.
   */
  public FilteringArtifactClassLoader(ClassLoader parent, ArtifactClassLoader artifactClassLoader, ClassLoaderFilter filter,
                                      List<ExportedService> exportedServices) {
    super(parent);

    checkArgument(artifactClassLoader != null, "ArtifactClassLoader cannot be null");
    checkArgument(filter != null, "Filter cannot be null");
    checkArgument(exportedServices != null, "exportedServiceProviders cannot be null");

    this.artifactClassLoader = artifactClassLoader;
    this.filter = filter;
    this.exportedServices = exportedServices;

    verboseLogging = valueOf(getProperty(MULE_LOG_VERBOSE_CLASSLOADING));
  }

  private boolean isVerboseLogging() {
    return verboseLogging || LOGGER.isDebugEnabled();
  }

  /**
   * Creates a new filtering classLoader
   *
   * @param artifactClassLoader artifact classLoader to filter. Non null
   * @param filter              filters access to classes and resources from the artifact classLoader. Non null
   * @param exportedServices    service providers that will be available from the filtered class loader. Non null.
   */
  public FilteringArtifactClassLoader(ArtifactClassLoader artifactClassLoader, ClassLoaderFilter filter,
                                      List<ExportedService> exportedServices) {
    this(getSystemClassLoader(), artifactClassLoader, filter, exportedServices);
  }

  @Override
  public Class<?> loadClass(String name) throws ClassNotFoundException {
    if (filter.exportsClass(name)) {
      return artifactClassLoader.getClassLoader().loadClass(name);
    } else {
      throw new NotExportedClassException(name, getArtifactId(), filter);
    }
  }

  @Override
  public URL getResource(String name) {
    if (isServiceResource(name)) {
      Optional<ExportedService> exportedService = getExportedServiceStream(name).findFirst();

      if (exportedService.isPresent()) {
        if (isVerboseLogging()) {
          logClassloadingTrace(format("Service resource '%s' found in classloader for '%s': '%s", name, getArtifactId(),
                                      exportedService.get()));
        }
        return exportedService.get().getResource();
      } else {
        if (isVerboseLogging()) {
          logClassloadingTrace(format("Service resource '%s' not found in classloader for '%s'.", name, getArtifactId()));
        }
        return null;
      }
    } else {
      if (filter.exportsResource(name)) {
        return getResourceFromDelegate(artifactClassLoader, name);
      } else {
        if (isVerboseLogging()) {
          logClassloadingTrace(format("Resource '%s' not found in classloader for '%s'.", name, getArtifactId()));
          logClassloadingTrace(format("Filter applied for resource '%s': %s", name, getArtifactId()));
        }

        return null;
      }
    }
  }

  protected URL getResourceFromDelegate(ArtifactClassLoader artifactClassLoader, String name) {
    return artifactClassLoader.findResource(name);
  }

  @Override
  public Enumeration<URL> getResources(String name) throws IOException {
    if (isServiceResource(name)) {
      List<URL> exportedServiceProviders = getExportedServiceStream(name).map(s -> s.getResource()).collect(Collectors.toList());

      if (isVerboseLogging()) {
        if (exportedServiceProviders.isEmpty()) {
          logClassloadingTrace(format("Service resource '%s' not found in classloader for '%s'.", name, getArtifactId()));
        } else {

          logClassloadingTrace(format("Service resources '%s' found in classloader for '%s': '%s", name, getArtifactId(),
                                      exportedServiceProviders));
        }
      }
      return enumeration(exportedServiceProviders);
    } else if (filter.exportsResource(name)) {
      return getResourcesFromDelegate(artifactClassLoader, name);
    } else {
      if (isVerboseLogging()) {
        logClassloadingTrace(format("Resources '%s' not found in classloader for '%s'.", name, getArtifactId()));
        logClassloadingTrace(format("Filter applied for resources '%s': %s", name, getArtifactId()));
      }
      return enumeration(emptyList());
    }
  }

  private Stream<ExportedService> getExportedServiceStream(String name) {
    String serviceInterface = getServiceInterfaceFromResource(name);

    return this.exportedServices.stream().filter(s -> serviceInterface.equals(s.getServiceInterface()));
  }

  private String getServiceInterfaceFromResource(String name) {
    return name.substring(SERVICE_PREFIX.length());
  }

  private boolean isServiceResource(String name) {
    return name.startsWith(SERVICE_PREFIX);
  }

  private void logClassloadingTrace(String message) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace(message);
    } else {
      LOGGER.info(message);
    }
  }

  protected Enumeration<URL> getResourcesFromDelegate(ArtifactClassLoader artifactClassLoader, String name) throws IOException {
    return artifactClassLoader.findResources(name);
  }

  @Override
  public URL findResource(String name) {
    return artifactClassLoader.findResource(name);
  }

  @Override
  public URL findInternalResource(String resource) {
    return findResource(resource);
  }

  @Override
  public Enumeration<URL> findResources(String name) throws IOException {
    return artifactClassLoader.findResources(name);
  }

  @Override
  public Class<?> findLocalClass(String name) throws ClassNotFoundException {
    return artifactClassLoader.findLocalClass(name);
  }

  @Override
  protected Package getPackage(String name) {
    return filter.exportsPackage(name) ? super.getPackage(name) : null;
  }

  @Override
  protected Package[] getPackages() {
    Package[] packagesList = super.getPackages();
    return stream(packagesList).filter(aPackage -> filter.exportsPackage(aPackage.getName())).toArray(Package[]::new);
  }

  @Override
  public Class<?> loadInternalClass(String name) throws ClassNotFoundException {
    return artifactClassLoader.getClassLoader().loadClass(name);
  }

  @Override
  public String toString() {
    return format("%s[%s]@%s", getClass().getName(), artifactClassLoader.getArtifactId(), toHexString(identityHashCode(this)));
  }

  @Override
  public String getArtifactId() {
    return artifactClassLoader.getArtifactId();
  }

  @Override
  public <T extends ArtifactDescriptor> T getArtifactDescriptor() {
    return artifactClassLoader.getArtifactDescriptor();
  }

  @Override
  public ClassLoader getClassLoader() {
    return this;
  }

  @Override
  public void addShutdownListener(ShutdownListener listener) {
    artifactClassLoader.addShutdownListener(listener);
  }

  @Override
  public ClassLoaderLookupPolicy getClassLoaderLookupPolicy() {
    return artifactClassLoader.getClassLoaderLookupPolicy();
  }

  @Override
  public void dispose() {
    // Nothing to do here as this is just wrapper for another classLoader
  }

  @Override
  public URL findLocalResource(String resourceName) {
    return artifactClassLoader.findLocalResource(resourceName);
  }

  public ArtifactClassLoader getArtifactClassLoader() {
    return artifactClassLoader;
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
