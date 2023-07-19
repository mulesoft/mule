/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.runner.classloader.container;

import static org.mule.runtime.container.internal.ContainerClassLoaderCreatorUtils.SYSTEM_PACKAGES;

import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.container.api.MuleContainerClassLoaderWrapper;
import org.mule.runtime.module.artifact.api.classloader.LookupStrategy;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Test utility that allows to manage the container class loader for Mule functional test cases.
 *
 * @since 4.5
 */
public interface TestContainerClassLoaderAssembler extends AutoCloseable {

  static TestContainerClassLoaderAssembler create(List<String> extraBootPackages, Set<String> extraPrivilegedArtifacts,
                                                  List<URL> muleUrls,
                                                  List<URL> optUrls) {
    return new DefaultTestContainerClassLoaderAssembler(extraBootPackages, extraPrivilegedArtifacts, muleUrls, optUrls);
  }

  /**
   * @return a collection with the system packages.
   */
  static Collection<String> getSystemPackages() {
    return SYSTEM_PACKAGES;
  }

  /**
   * @return the container class loader with all the configuration needed for the tests.
   */
  MuleContainerClassLoaderWrapper createContainerClassLoader();

  /**
   * @return a lookup strategy that allows searching for a class only in the container class loader.
   */
  LookupStrategy getContainerOnlyLookupStrategy();

  /**
   * @return the module repository used for creating the container class loader.
   */
  ModuleRepository getModuleRepository();

}
