/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.runner.classloader.container;

import static org.mule.runtime.container.internal.ContainerClassLoaderCreatorUtils.getLookupPolicy;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;

import org.mule.runtime.container.api.MuleModule;
import org.mule.runtime.container.internal.DefaultModuleRepository;
import org.mule.runtime.container.internal.JreModuleDiscoverer;
import org.mule.runtime.container.internal.PreFilteredContainerClassLoaderCreator;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
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
    containerClassLoader = new MuleArtifactClassLoader("container", artifactDescriptor,
                                                       new URL[0],
                                                       parentClassLoader,
                                                       getLookupPolicy(parentClassLoader, getMuleModules(),
                                                                       getBootPackages()));
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
