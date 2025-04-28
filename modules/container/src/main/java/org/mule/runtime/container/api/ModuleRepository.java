/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.container.api;

import static org.mule.runtime.container.internal.ClasspathModuleDiscoverer.MODULE_PROPERTIES;

import org.mule.runtime.container.internal.ClasspathModuleDiscoverer;
import org.mule.runtime.container.internal.CompositeModuleDiscoverer;
import org.mule.runtime.container.internal.DefaultModuleRepository;
import org.mule.runtime.container.internal.JreModuleDiscoverer;
import org.mule.runtime.jpms.api.MuleContainerModule;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Provides access to all Mule modules available on the container.
 */
public interface ModuleRepository {

  /**
   * Creates a ModuleRepository based on the modules available on the provided {@code classLoader}.
   *
   * @param classLoader     this is ignored.
   * @param temporaryFolder where to write the generated SPI mapping files.
   * @return a new {@link ModuleRepository} with the discovered information from the current runtime context.
   * @deprecated since 4.6 use {@link #createModuleRepository(File)} instead.
   */
  @Deprecated
  public static ModuleRepository createModuleRepository(ClassLoader classLoader, File temporaryFolder) {
    return createModuleRepository(temporaryFolder);
  }

  /**
   * Creates a ModuleRepository based on the modules available on this class classLoader.
   *
   * @param temporaryFolder where to write the generated SPI mapping files.
   * @return a new {@link ModuleRepository} with the discovered information from the current runtime context.
   */
  public static ModuleRepository createModuleRepository(File temporaryFolder) {
    return new DefaultModuleRepository(new CompositeModuleDiscoverer(new JreModuleDiscoverer(),
                                                                     new ClasspathModuleDiscoverer(temporaryFolder,
                                                                                                   MODULE_PROPERTIES)));
  }

  /**
   * Creates a ModuleRepository based on the modules available on the provided {@code classLoader}.
   *
   * @param classLoader                   this is ignored.
   * @param serviceInterfaceToServiceFile determines the SPI mapping file for the fully qualified interface service name.
   * @param fileToResource                obtains a {@link URL} from the SPI mapping file and the fully qualified interface
   *                                      service name
   * @return a new {@link ModuleRepository} with the discovered information from the current runtime context.
   *
   * @since 4.5
   * @deprecated since 4.6 use {@link #createModuleRepository(Function, BiFunction)} instead.
   */
  @Deprecated
  public static ModuleRepository createModuleRepository(ClassLoader classLoader,
                                                        Function<String, File> serviceInterfaceToServiceFile,
                                                        BiFunction<String, File, URL> fileToResource) {
    return new DefaultModuleRepository(new CompositeModuleDiscoverer(new JreModuleDiscoverer(),
                                                                     new ClasspathModuleDiscoverer(serviceInterfaceToServiceFile,
                                                                                                   fileToResource,
                                                                                                   MODULE_PROPERTIES)));
  }

  /**
   * Creates a ModuleRepository based on the modules available on the provided {@code classLoader}.
   *
   * @param serviceInterfaceToServiceFile determines the SPI mapping file for the fully qualified interface service name.
   * @param fileToResource                obtains a {@link URL} from the SPI mapping file and the fully qualified interface
   *                                      service name
   * @return a new {@link ModuleRepository} with the discovered information from the current runtime context.
   *
   * @since 4.5
   */
  public static ModuleRepository createModuleRepository(Function<String, File> serviceInterfaceToServiceFile,
                                                        BiFunction<String, File, URL> fileToResource) {
    return new DefaultModuleRepository(new CompositeModuleDiscoverer(new JreModuleDiscoverer(),
                                                                     new ClasspathModuleDiscoverer(serviceInterfaceToServiceFile,
                                                                                                   fileToResource,
                                                                                                   MODULE_PROPERTIES)));
  }

  /**
   * @return a non null list of {@link MuleModule}
   */
  List<MuleContainerModule> getModules();
}
