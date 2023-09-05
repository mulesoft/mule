/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.runner.classloader.container;

import static org.mule.runtime.container.internal.ContainerClassLoaderCreatorUtils.getLookupPolicy;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;

import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.container.internal.ClasspathModuleDiscoverer;
import org.mule.runtime.container.internal.ContainerModuleDiscoverer;
import org.mule.runtime.container.internal.DefaultModuleRepository;
import org.mule.runtime.container.internal.PreFilteredContainerClassLoaderCreator;
import org.mule.runtime.jpms.api.MuleContainerModule;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.net.URL;
import java.util.List;
import java.util.Set;

/**
 * Implementation of {@link PreFilteredContainerClassLoaderCreator} useful for functional tests.
 *
 * @since 4.5
 */
public class TestPreFilteredContainerClassLoaderCreator implements PreFilteredContainerClassLoaderCreator {

  private static final String TEST_MODULE_PROPERTIES = "META-INF/mule-test-module.properties";

  private final Set<String> bootPackages;
  private final ClassLoader containerSystemClassloader;
  private final DefaultModuleRepository testContainerModuleRepository;

  public TestPreFilteredContainerClassLoaderCreator(ClassLoader containerSystemClassloader, Set<String> bootPackages,
                                                    Set<String> extraPrivilegedArtifacts) {
    this.containerSystemClassloader = containerSystemClassloader;
    this.bootPackages = bootPackages;

    final ContainerModuleDiscoverer moduleDiscoverer = new ContainerModuleDiscoverer(containerSystemClassloader);
    moduleDiscoverer.addModuleDiscoverer(new ClasspathModuleDiscoverer(containerSystemClassloader, TEST_MODULE_PROPERTIES));
    this.testContainerModuleRepository =
        new DefaultModuleRepository(new TestModuleDiscoverer(extraPrivilegedArtifacts,
                                                             moduleDiscoverer));
  }

  @Override
  public List<MuleContainerModule> getMuleModules() {
    return withContextClassLoader(containerSystemClassloader, testContainerModuleRepository::getModules);
  }

  public ModuleRepository getModuleRepository() {
    return testContainerModuleRepository;
  }

  @Override
  public Set<String> getBootPackages() {
    return bootPackages;
  }

  @Override
  public ArtifactClassLoader getPreFilteredContainerClassLoader(ArtifactDescriptor artifactDescriptor,
                                                                ClassLoader parentClassLoader) {
    return new MuleArtifactClassLoader("container", artifactDescriptor,
                                       new URL[0],
                                       parentClassLoader,
                                       getLookupPolicy(parentClassLoader, getMuleModules(),
                                                       getBootPackages()));
  }

}
