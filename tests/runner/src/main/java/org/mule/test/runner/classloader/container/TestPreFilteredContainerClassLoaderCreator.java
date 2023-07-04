/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.runner.classloader.container;

import static org.mule.runtime.container.internal.ContainerClassLoaderCreatorUtils.getLookupPolicy;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;

import static java.util.Collections.emptyMap;

import org.mule.runtime.container.api.MuleModule;
import org.mule.runtime.container.internal.DefaultModuleRepository;
import org.mule.runtime.container.internal.JreModuleDiscoverer;
import org.mule.runtime.container.internal.MuleClassLoaderLookupPolicy;
import org.mule.runtime.container.internal.PreFilteredContainerClassLoaderCreator;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

/**
 * Implementation of {@link PreFilteredContainerClassLoaderCreator} useful for functional tests.
 *
 * @since 4.5
 */
public class TestPreFilteredContainerClassLoaderCreator implements PreFilteredContainerClassLoaderCreator {

  private final Set<String> extraBootPackages;
  private final URL[] muleUrls;
  private final URL[] optUrls;
  private final URLClassLoader classLoader;
  private final DefaultModuleRepository testContainerModuleRepository;
  private ArtifactClassLoader containerClassLoader;

  public TestPreFilteredContainerClassLoaderCreator(final List<String> extraBootPackages, final URL[] muleUrls,
                                                    final URL[] optUrls) {
    this.extraBootPackages = ImmutableSet.<String>builder().addAll(BOOT_PACKAGES).addAll(extraBootPackages)
        .addAll(new JreModuleDiscoverer().discover().get(0).getExportedPackages()).build();
    this.muleUrls = muleUrls;
    this.optUrls = optUrls;
    this.classLoader = new URLClassLoader(muleUrls, null);
    this.testContainerModuleRepository = new DefaultModuleRepository(new TestContainerModuleDiscoverer(classLoader));
  }

  @Override
  public List<MuleModule> getMuleModules() {
    return withContextClassLoader(classLoader, testContainerModuleRepository::getModules);
  }

  @Override
  public Set<String> getBootPackages() {
    return extraBootPackages;
  }

  @Override
  public ArtifactClassLoader getPreFilteredContainerClassLoader(ArtifactDescriptor artifactDescriptor,
                                                                ClassLoader parentClassLoader) {
    // This cannot be a simple URLClassLoader because mockito is in the classloader for the test-runner-plugin and how the
    // subclass mockito maker creates the dynamic mock class.
    // An attempt should be made to check if a simple URLClassLoader works if inline mocks are used.
    ClassLoader containerOptClassLoader =
        new MuleArtifactClassLoader(artifactDescriptor.getName() + "/opt", artifactDescriptor,
                                    optUrls,
                                    parentClassLoader,
                                    new MuleClassLoaderLookupPolicy(emptyMap(), getBootPackages()));

    containerClassLoader = new MuleArtifactClassLoader(artifactDescriptor.getName(), artifactDescriptor,
                                                       muleUrls,
                                                       containerOptClassLoader,
                                                       new MuleClassLoaderLookupPolicy(emptyMap(), getBootPackages()));
    return containerClassLoader;
  }

  /**
   * @return the class loader of the container that was created by the last call to
   *         {@link #getPreFilteredContainerClassLoader(ArtifactDescriptor, ClassLoader)}.
   */
  public ArtifactClassLoader getBuiltPreFilteredContainerClassLoader() {
    return containerClassLoader;
  }

  /**
   * @param parentClassLoader class loader used as parent of the container's. It's the classLoader that will load Mule classes.
   * @return the lookup policy for the container class loader with the given parent.
   */
  public ClassLoaderLookupPolicy getContainerClassLoaderLookupPolicy(ClassLoader parentClassLoader) {
    return getLookupPolicy(parentClassLoader, testContainerModuleRepository.getModules(), getBootPackages());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() {
    try {
      classLoader.close();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
