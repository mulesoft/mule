/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.services;

import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.SERVICE;
import static org.mule.runtime.core.api.util.FileUtils.unzip;

import static java.lang.Class.forName;
import static java.lang.System.getProperty;
import static java.nio.file.Files.createTempDirectory;
import static java.util.Collections.emptyMap;
import static java.util.stream.Stream.concat;

import org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration;
import org.mule.runtime.module.artifact.api.descriptor.InvalidDescriptorLoaderException;
import org.mule.runtime.module.service.api.artifact.ServiceDescriptor;
import org.mule.runtime.module.service.internal.artifact.LibFolderClassLoaderConfigurationLoader;
import org.mule.tck.config.WeaveExpressionLanguageFactoryServiceProvider;
import org.mule.weave.v2.el.WeaveDefaultExpressionLanguageFactoryService;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

/**
 * Implementation of {@link WeaveExpressionLanguageFactoryServiceProvider} that creates a classloader for the DataWeave service
 * with its dependencies, in an isolated manner.
 *
 * @since 4.5
 */
public class IsolatedWeaveExpressionLanguageFactoryServiceProvider implements WeaveExpressionLanguageFactoryServiceProvider {

  @Override
  public DefaultExpressionLanguageFactoryService createDefaultExpressionLanguageFactoryService() {
    // Search for DW service in the classpath
    String classPath = getProperty("java.class.path");
    String modulePath = getProperty("jdk.module.path");

    return (modulePath != null
        ? concat(Stream.of(classPath.split(":")),
                 Stream.of(modulePath.split(":")))
        : Stream.of(classPath.split(":")))
            .filter(StringUtils::isNotBlank)
            .filter(pathEntry -> {
              final String[] split = pathEntry.split("/");
              final String fileName = split[split.length - 1];

              return fileName.startsWith("mule-service-weave") && fileName.endsWith("mule-service.jar");
            })
            .map(pathEntry -> new File(pathEntry))
            .findAny()
            .map(weaveServiceJarFile -> {
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

              final ClassLoaderConfiguration serviceClassLaoderConfiguration;
              try {
                serviceClassLaoderConfiguration =
                    new LibFolderClassLoaderConfigurationLoader().load(serviceExplodedDir, emptyMap(), SERVICE);
              } catch (InvalidDescriptorLoaderException e) {
                throw new IllegalStateException("Couldn't create descriptor for mule-service-weave", e);
              }

              ServiceDescriptor descriptor = new ServiceDescriptor("mule-service-weave");
              descriptor.setClassLoaderConfiguration(serviceClassLaoderConfiguration);
              ClassLoader serviceClassLoader = createServiceClassLoader(descriptor);

              return instantiateService(serviceClassLoader);
            })
            .orElseGet(() -> new WeaveDefaultExpressionLanguageFactoryService(null));
  }

  private ClassLoader createServiceClassLoader(ServiceDescriptor descriptor) {
    // Creates an URLClassLoader because in the context of unit tests there is no containerClasslaoder available, which is
    // required for the creation of an actual serviceClassLaoder.
    return new URLClassLoader(descriptor.getClassLoaderConfiguration().getUrls(),
                              new ClassLoader(this.getClass().getClassLoader()) {

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

  private DefaultExpressionLanguageFactoryService instantiateService(ClassLoader serviceClassLoader) {
    try {
      Class<DefaultExpressionLanguageFactoryService> weaveServiceClass =
          (Class<DefaultExpressionLanguageFactoryService>) forName("org.mule.weave.v2.el.WeaveDefaultExpressionLanguageFactoryService",
                                                                   false, serviceClassLoader);

      final Constructor<DefaultExpressionLanguageFactoryService> constructor =
          weaveServiceClass.getConstructor(SchedulerService.class);
      return constructor.newInstance(new Object[] {null});
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException
        | InvocationTargetException | NoSuchMethodException | SecurityException e) {
      throw new IllegalStateException("Couldn't instantiate mule-service-weave", e);
    }
  }


}
