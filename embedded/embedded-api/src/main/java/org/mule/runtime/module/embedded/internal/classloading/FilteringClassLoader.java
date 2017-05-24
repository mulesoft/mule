/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.embedded.internal.classloading;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Boolean.valueOf;
import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.util.Collections.emptyList;
import org.mule.runtime.module.embedded.internal.NotExportedClassException;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO MULE-11882 - Consolidate classloading isolation
public class FilteringClassLoader extends ClassLoader {

  private static final String SERVICE_PREFIX = "META-INF/services/";

  protected static final Logger logger = LoggerFactory.getLogger(FilteringClassLoader.class);
  public static final String SYSTEM_PROPERTY_PREFIX = "mule.";
  public static final String MULE_LOG_VERBOSE_CLASSLOADING = SYSTEM_PROPERTY_PREFIX + "classloading.verbose";

  private final ClassLoaderFilter filter;
  private final List<ExportedService> exportedServices;

  /**
   * Creates a new filtering classLoader
   *
   * @param filter filters access to classes and resources from the artifact classLoader. Non null
   * @param exportedServices exportedServices service providers that will be available from the filtered class loader. Non null.
   */
  public FilteringClassLoader(ClassLoaderFilter filter,
                              List<ExportedService> exportedServices) {
    checkArgument(filter != null, "Filter cannot be null");
    checkArgument(exportedServices != null, "exportedServices cannot be null");

    this.filter = filter;
    this.exportedServices = exportedServices;
  }

  @Override
  public Class<?> loadClass(String name) throws ClassNotFoundException {
    if (filter.exportsClass(name)) {
      return super.loadClass(name);
    } else {
      throw new NotExportedClassException(name, filter);
    }
  }

  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    if (filter.exportsClass(name)) {
      return super.loadClass(name, resolve);
    } else {
      throw new NotExportedClassException(name, filter);
    }
  }

  @Override
  public URL getResource(String name) {
    if (isServiceResource(name)) {
      Optional<ExportedService> exportedService = getExportedServiceStream(name).findFirst();

      if (exportedService.isPresent()) {
        logClassloadingTrace(format("Service resource '%s' found: '%s", name, exportedService.get()));
        return exportedService.get().getResource();
      } else {
        logClassloadingTrace(format("Service resource '%s' not found", name));
        return null;
      }
    } else {
      if (filter.exportsResource(name)) {
        return getResourceFromDelegate(name);
      } else {
        logClassloadingTrace(format("Resource '%s' not found in classloader.", name));
        logClassloadingTrace(format("Filter applied for resource: %s", name));

        return null;
      }
    }
  }

  protected URL getResourceFromDelegate(String name) {
    return findResource(name);
  }

  @Override
  public Enumeration<URL> getResources(String name) throws IOException {
    if (isServiceResource(name)) {
      List<URL> exportedServiceProviders = getExportedServiceStream(name).map(s -> s.getResource()).collect(Collectors.toList());

      if (exportedServiceProviders.isEmpty()) {
        logClassloadingTrace(format("Service resource '%s' not found", name));
      } else {

        logClassloadingTrace(format("Service resources '%s' found: '%s", name, exportedServiceProviders));
      }
      return new EnumerationAdapter<>(exportedServiceProviders);
    } else if (filter.exportsResource(name)) {
      return getResourcesFromDelegate(name);
    } else {
      logClassloadingTrace(format("Resources '%s' not found ", name));
      logClassloadingTrace(format("Filter applied for resources: '%s", name));
      return new EnumerationAdapter<>(emptyList());
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
    if (isVerboseClassLoading()) {
      logger.info(message);
    } else if (logger.isTraceEnabled()) {
      logger.trace(message);
    }
  }

  private Boolean isVerboseClassLoading() {
    return valueOf(getProperty(MULE_LOG_VERBOSE_CLASSLOADING));
  }

  protected Enumeration<URL> getResourcesFromDelegate(String name) throws IOException {
    return findResources(name);
  }

}
