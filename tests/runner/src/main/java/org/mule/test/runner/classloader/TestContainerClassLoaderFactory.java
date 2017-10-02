/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.classloader;

import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.container.api.MuleModule;
import org.mule.runtime.container.internal.ContainerClassLoaderFactory;
import org.mule.runtime.container.internal.DefaultModuleRepository;
import org.mule.runtime.container.internal.JreModuleDiscoverer;
import org.mule.runtime.container.internal.MuleClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Extends the default {@link ContainerClassLoaderFactory} for testing in order to add boot packages and build a
 * {@link ClassLoader} for the container that do resolves classes instead of delegating to its parent and also allows to create
 * the container {@link ClassLoaderLookupPolicy} based on a {@link ClassLoader}.
 *
 * @since 4.0
 */
public class TestContainerClassLoaderFactory extends ContainerClassLoaderFactory implements AutoCloseable {

  private final Set<String> extraBootPackages;
  private final URL[] urls;
  private final URLClassLoader classLoader;
  private final DefaultModuleRepository testContainerModuleRepository;
  private ArtifactClassLoader containerClassLoader;

  /**
   * Factory class that extends the default way to create a container {@link ArtifactClassLoader} in order to support the
   * differences when running applications in standalone container vs junit.
   *
   * @param extraBootPackages {@link List} of {@link String}s extra boot packages that need to be appended to the container (junit
   *        for instance)
   * @param urls {@link URL}s that were classified to be added to the container {@link ClassLoader}
   * @param moduleRepository provides access to the modules available on the container. Non null.
   */
  public TestContainerClassLoaderFactory(final List<String> extraBootPackages, final URL[] urls,
                                         ModuleRepository moduleRepository) {
    super(moduleRepository);

    this.extraBootPackages = ImmutableSet.<String>builder().addAll(super.getBootPackages()).addAll(extraBootPackages)
        .addAll(new JreModuleDiscoverer().discover().get(0).getExportedPackages()).build();
    this.urls = urls;
    this.classLoader = new URLClassLoader(urls, null);
    this.testContainerModuleRepository = new DefaultModuleRepository(new TestContainerModuleDiscoverer(classLoader));
  }

  /**
   * Overrides method due to it has to use the {@link ClassLoader} set to this factory in order to discover modules.
   *
   * @param parentClassLoader parent classLoader. Can be null.
   * @return a non null {@link ArtifactClassLoader} containing container code that can be used as parent classloader for other
   *         mule artifacts.
   */
  @Override
  public ArtifactClassLoader createContainerClassLoader(final ClassLoader parentClassLoader) {
    final List<MuleModule> muleModules = withContextClassLoader(classLoader, () -> testContainerModuleRepository.getModules());

    MuleClassLoaderLookupPolicy lookupPolicy = new MuleClassLoaderLookupPolicy(Collections.emptyMap(), getBootPackages());

    return createArtifactClassLoader(parentClassLoader, muleModules, lookupPolicy, new ArtifactDescriptor("mule"));
  }

  /**
   * Overrides the method in order to create a {@link ArtifactClassLoader} that will have a CHILD_FIRST
   * {@link ClassLoaderLookupPolicy}, it is needed due to as difference from a mule standalone container where the parent
   * {@link ClassLoader} for the container only has bootstrap jars plugins mule modules and third-party libraries when the runner
   * runs this tests using has a full class path with all the artifacts declared as dependencies for the artifact so we have to
   * change that and change the look strategy to be CHILD_FIRST for the container.
   * <p/>
   * The {@code muleModules} parameter will be ignored due to it has all the modules in classpath, instead they are discovered
   * once again but using a {@link URLClassLoader} that has the {@link URL}'s classified for the container {@link ClassLoader}.
   *
   * @param parentClassLoader the parent {@link ClassLoader} to delegate PARENT look ups
   * @param muleModules {@link MuleModule} discovered from the launcher {@link ClassLoader} but will be not considered here due to
   *        it has all the class path
   * @param containerLookupPolicy the default {@link ClassLoaderLookupPolicy} defined for a container but will be ignored due to
   *        it has to be different when running with a full class path as parent {@link ClassLoader}
   * @param artifactDescriptor
   * @return the {@link ArtifactClassLoader} to be used for the container
   */
  @Override
  protected ArtifactClassLoader createArtifactClassLoader(final ClassLoader parentClassLoader, final List<MuleModule> muleModules,
                                                          final ClassLoaderLookupPolicy containerLookupPolicy,
                                                          ArtifactDescriptor artifactDescriptor) {
    final ArtifactDescriptor containerDescriptor = new ArtifactDescriptor("mule");

    containerClassLoader =
        new MuleArtifactClassLoader(containerDescriptor.getName(), containerDescriptor, urls, parentClassLoader,
                                    containerLookupPolicy);

    return createContainerFilteringClassLoader(withContextClassLoader(classLoader,
                                                                      () -> testContainerModuleRepository.getModules()),
                                               containerClassLoader);
  }

  public ArtifactClassLoader getContainerClassLoader() {
    return containerClassLoader;
  }

  /**
   * @return the original list of boot packages defined in {@link ContainerClassLoaderFactory} plus extra packages needed to be
   *         added in order to allow tests to run with isolation. For instance, junit is an extra package that has to be handled
   *         as boot package.
   */
  @Override
  public Set<String> getBootPackages() {
    return extraBootPackages;
  }

  public ClassLoaderLookupPolicy getContainerClassLoaderLookupPolicy(ClassLoader classLoader) {
    return super.getContainerClassLoaderLookupPolicy(classLoader, testContainerModuleRepository.getModules());
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
