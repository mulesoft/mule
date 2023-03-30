/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.container.internal;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;

import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.container.api.MuleContainerClassLoaderWrapper;
import org.mule.runtime.container.api.MuleModule;
import org.mule.runtime.container.api.TestContainerClassLoaderAssembler;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderFilter;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.FilteringArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.LookupStrategy;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.net.URL;
import java.util.List;
import java.util.Set;

/**
 * Default implementation of {@link TestContainerClassLoaderAssembler}.
 */
public class DefaultTestContainerClassLoaderAssembler implements TestContainerClassLoaderAssembler {

  private final DefaultModuleRepository moduleRepository;
  private final TestPreFilteredContainerClassLoaderCreator testContainerClassLoaderCreator;
  private final ContainerClassLoaderFactory containerClassLoaderFactory;

  public DefaultTestContainerClassLoaderAssembler(List<String> extraBootPackages, Set<String> extraPrivilegedArtifacts,
                                                  List<URL> urls) {
    moduleRepository =
        new DefaultModuleRepository(new TestModuleDiscoverer(extraPrivilegedArtifacts,
                                                             new TestContainerModuleDiscoverer(ContainerClassLoaderFactory.class
                                                                 .getClassLoader())));
    testContainerClassLoaderCreator =
        new TestPreFilteredContainerClassLoaderCreator(extraBootPackages, urls.toArray(new URL[0]));
    containerClassLoaderFactory = new ContainerClassLoaderFactory(testContainerClassLoaderCreator);
  }

  /**
   * Creates an {@link ArtifactClassLoader} for the container. The difference between a mule container {@link ArtifactClassLoader}
   * in standalone mode and this one is that it has to be aware that the parent class loader has all the URLs loaded in the
   * launcher app class loader, so it has to create a particular lookup policy to resolve classes as {@code CHILD_FIRST}.
   * <p/>
   * In order to do that a {@link FilteringArtifactClassLoader} is created with an empty lookup policy (meaning that
   * {@code CHILD_FIRST} strategy will be used) for the {@link URL}s that are going to be exposed from the container class loader.
   * This would be the parent class loader for the container, so instead of going directly to the launcher application class
   * loader that has access to the whole classpath, this filtering class loader will resolve only the classes for the {@link URL}s
   * defined to be in the container.
   *
   * @return an {@link ArtifactClassLoader} for the container.
   */
  @Override
  public MuleContainerClassLoaderWrapper createContainerClassLoader() {
    MuleArtifactClassLoader launcherArtifact = createLauncherArtifactClassLoader();
    final List<MuleModule> muleModules = emptyList();
    ClassLoaderFilter filteredClassLoaderLauncher = new ContainerClassLoaderFilterFactory()
        .create(testContainerClassLoaderCreator.getBootPackages(), muleModules);
    final ArtifactClassLoader parentClassLoader =
        new FilteringArtifactClassLoader(launcherArtifact, filteredClassLoaderLauncher, emptyList());
    final ArtifactClassLoader containerClassLoader =
        containerClassLoaderFactory.createContainerClassLoader(parentClassLoader.getClassLoader()).getContainerClassLoader();

    return new TestMuleContainerClassLoaderWrapper(containerClassLoader, testContainerClassLoaderCreator
        .getContainerClassLoaderLookupPolicy(containerClassLoader.getClassLoader()));
  }

  /**
   * Creates the launcher application class loader to delegate from the container class loader.
   *
   * @return an {@link ArtifactClassLoader} for the launcher, parent of the container.
   */
  protected MuleArtifactClassLoader createLauncherArtifactClassLoader() {
    ClassLoader launcherClassLoader = this.getClass().getClassLoader();

    return new MuleArtifactClassLoader("mule", new ArtifactDescriptor("mule"), new URL[0], launcherClassLoader,
                                       new MuleClassLoaderLookupPolicy(emptyMap(), emptySet())) {

      @Override
      public URL findResource(String name) {
        URL url = super.findResource(name);
        if (url == null && getParent() != null) {
          url = getParent().getResource(name);
          // Filter if it is not a resource from the jre
          if (url != null && url.getFile().matches(".*?\\/jre\\/lib\\/\\w+\\.jar\\!.*")) {
            return url;
          } else {
            return null;
          }
        }
        return url;
      }
    };
  }

  @Override
  public LookupStrategy getContainerOnlyLookupStrategy() {
    return new ContainerOnlyLookupStrategy(testContainerClassLoaderCreator.getBuiltPreFilteredContainerClassLoader()
        .getClassLoader());
  }

  @Override
  public ModuleRepository getModuleRepository() {
    return moduleRepository;
  }

  @Override
  public void close() throws Exception {
    testContainerClassLoaderCreator.close();
  }

  private static class TestMuleContainerClassLoaderWrapper implements MuleContainerClassLoaderWrapper {

    private final ArtifactClassLoader containerClassLoader;
    private final ClassLoaderLookupPolicy containerClassLoaderLookupPolicy;

    public TestMuleContainerClassLoaderWrapper(ArtifactClassLoader containerClassLoader,
                                               ClassLoaderLookupPolicy containerClassLoaderLookupPolicy) {
      this.containerClassLoader = containerClassLoader;
      this.containerClassLoaderLookupPolicy = containerClassLoaderLookupPolicy;
    }

    @Override
    public ArtifactClassLoader getContainerClassLoader() {
      return containerClassLoader;
    }

    @Override
    public ClassLoaderLookupPolicy getContainerClassLoaderLookupPolicy() {
      return containerClassLoaderLookupPolicy;
    }
  }
}
