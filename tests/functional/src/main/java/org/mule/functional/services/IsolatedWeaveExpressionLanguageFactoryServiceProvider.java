/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.services;

import static org.mule.runtime.api.artifact.ArtifactType.SERVICE;
import static org.mule.runtime.core.api.util.FileUtils.unzip;
import static org.mule.runtime.module.service.api.artifact.ServiceClassLoaderFactoryProvider.serviceClassLoaderConfigurationLoader;

import static java.lang.Class.forName;
import static java.lang.System.getProperty;
import static java.nio.file.Files.createTempDirectory;
import static java.util.Collections.emptyMap;
import static java.util.stream.Stream.concat;

import org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration;
import org.mule.runtime.module.artifact.api.descriptor.InvalidDescriptorLoaderException;
import org.mule.runtime.module.service.api.artifact.ServiceDescriptor;
import org.mule.tck.config.WeaveExpressionLanguageFactoryServiceProvider;
import org.mule.weave.v2.el.provider.WeaveDefaultExpressionLanguageFactoryService;
import org.mule.weave.v2.el.metadata.WeaveExpressionLanguageMetadataServiceImpl;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

/**
 * Implementation of {@link WeaveExpressionLanguageFactoryServiceProvider} that creates a classloader for the DataWeave service
 * with its dependencies, in an isolated manner.
 *
 * @since 4.5
 */
public class IsolatedWeaveExpressionLanguageFactoryServiceProvider implements WeaveExpressionLanguageFactoryServiceProvider {

  private static Supplier<Optional<ClassLoader>> WEAVE_SERVICE_CLASSLOADER_SUPPLIER = new LazyValue<>(() -> {
    // Search for DW service in the classpath
    String classPath = getProperty("java.class.path");
    String modulePath = getProperty("jdk.module.path");
    String pathSeparator = getProperty("path.separator");
    // Needs special handling for windows builds
    String fileSeparator = getProperty("file.separator").replace("\\", "\\\\");;

    return (modulePath != null
        ? concat(Stream.of(classPath.split(pathSeparator)),
                 Stream.of(modulePath.split(pathSeparator)))
        : Stream.of(classPath.split(pathSeparator)))
        .filter(StringUtils::isNotBlank)
        .filter(pathEntry -> {
          final String[] split = pathEntry.split(fileSeparator);
          final String fileName = split[split.length - 1];

          return fileName.startsWith("mule-service-weave") && fileName.endsWith("mule-service.jar");
        })
        .map(pathEntry -> new File(pathEntry))
        .findAny()
        .map(IsolatedWeaveExpressionLanguageFactoryServiceProvider::createWeaveServiceClassLoaderFromExplodedServiceDir);
  });

  @Override
  public DefaultExpressionLanguageFactoryService createDefaultExpressionLanguageFactoryService() {
    return WEAVE_SERVICE_CLASSLOADER_SUPPLIER.get()
        .map(this::instantiateExpressionLanguageService)
        .orElseGet(() -> new WeaveDefaultExpressionLanguageFactoryService(null));
  }

  @Override
  public ExpressionLanguageMetadataService createExpressionLanguageMetadataService() {
    return WEAVE_SERVICE_CLASSLOADER_SUPPLIER.get()
        .map(this::instantiateExpressionLanguageMetadataService)
        .orElseGet(() -> new WeaveExpressionLanguageMetadataServiceImpl());
  }

  private static ClassLoader createWeaveServiceClassLoaderFromExplodedServiceDir(File weaveServiceJarFile) {
    // Unpack the service because java doesn't allow to create a classloader with jars within a zip out of the box.
    File serviceExplodedDir;
    try {
      serviceExplodedDir = createTempDirectory("mule-service-weave").toFile();
    } catch (IOException e) {
      throw new IllegalStateException("Couldn't create temporary dir for mule-service-weave", e);
    }
    try {
      unzip(weaveServiceJarFile, serviceExplodedDir);
    } catch (IOException e) {
      throw new IllegalStateException("Couldn't unpack mule-service-weave", e);
    }

    final ClassLoaderConfiguration serviceClassLaoderConfiguration = getClassLoaderConfiguration(serviceExplodedDir);

    ServiceDescriptor descriptor = new ServiceDescriptor("mule-service-weave");
    descriptor.setClassLoaderConfiguration(serviceClassLaoderConfiguration);

    return createServiceClassLoader(descriptor);
  }

  private static ClassLoaderConfiguration getClassLoaderConfiguration(File serviceExplodedDir) {
    final ClassLoaderConfiguration serviceClassLaoderConfiguration;
    try {
      serviceClassLaoderConfiguration =
          serviceClassLoaderConfigurationLoader().load(serviceExplodedDir, emptyMap(), SERVICE);
    } catch (InvalidDescriptorLoaderException e) {
      throw new IllegalStateException("Couldn't create descriptor for mule-service-weave", e);
    }
    return serviceClassLaoderConfiguration;
  }

  private static ClassLoader createServiceClassLoader(ServiceDescriptor descriptor) {
    // Creates an URLClassLoader because in the context of unit tests there is no containerClasslaoder available, which is
    // required for the creation of an actual serviceClassLaoder.
    return new URLClassLoader(descriptor.getClassLoaderConfiguration().getUrls(),
                              new ClassLoader(IsolatedWeaveExpressionLanguageFactoryServiceProvider.class.getClassLoader()) {

                                @Override
                                protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                                  if (name.startsWith("org.mule.weave.")) {
                                    // Force DW classes to be loaded by the service CL, not the root one
                                    throw new ClassNotFoundException(name);
                                  } else {
                                    return super.loadClass(name, resolve);
                                  }
                                }
                              });
  }

  private DefaultExpressionLanguageFactoryService instantiateExpressionLanguageService(ClassLoader serviceClassLoader) {
    try {
      Class<DefaultExpressionLanguageFactoryService> weaveServiceClass =
          (Class<DefaultExpressionLanguageFactoryService>) forName("org.mule.weave.v2.el.provider.WeaveDefaultExpressionLanguageFactoryService",
                                                                   false, serviceClassLoader);

      final Constructor<DefaultExpressionLanguageFactoryService> constructor =
          weaveServiceClass.getConstructor(SchedulerService.class);
      return constructor.newInstance(new Object[] {null});
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException
        | InvocationTargetException | NoSuchMethodException | SecurityException e) {
      throw new IllegalStateException("Couldn't instantiate mule-service-weave", e);
    }
  }

  private ExpressionLanguageMetadataService instantiateExpressionLanguageMetadataService(ClassLoader serviceClassLoader) {
    try {
      Class<ExpressionLanguageMetadataService> weaveServiceClass =
          (Class<ExpressionLanguageMetadataService>) forName("org.mule.weave.v2.el.metadata.WeaveExpressionLanguageMetadataServiceImpl",
                                                             false, serviceClassLoader);

      final Constructor<ExpressionLanguageMetadataService> constructor = weaveServiceClass.getConstructor();
      return constructor.newInstance();
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException
        | InvocationTargetException | NoSuchMethodException | SecurityException e) {
      throw new IllegalStateException("Couldn't instantiate mule-service-weave", e);
    }
  }


}
