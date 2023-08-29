/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.runner.classloader.container;

import static org.mule.runtime.container.internal.ContainerClassLoaderCreatorUtils.getLookupPolicy;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;

import org.mule.runtime.container.api.MuleModule;
import org.mule.runtime.container.internal.DefaultModuleRepository;
import org.mule.runtime.container.internal.PreFilteredContainerClassLoaderCreator;
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

  private final Set<String> bootPackages;
  private final ClassLoader containerSystemClassloader;
  private final DefaultModuleRepository testContainerModuleRepository;

  public TestPreFilteredContainerClassLoaderCreator(ClassLoader containerSystemClassloader, Set<String> bootPackages) {
    this.containerSystemClassloader = containerSystemClassloader;
    this.bootPackages = bootPackages;
    this.testContainerModuleRepository =
        new DefaultModuleRepository(new TestContainerModuleDiscoverer(containerSystemClassloader));
  }

  @Override
  public List<MuleModule> getMuleModules() {
    return withContextClassLoader(containerSystemClassloader, testContainerModuleRepository::getModules);
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
