/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.runner.classloader.container;

import static java.util.Collections.emptyEnumeration;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.list;

import static org.apache.commons.collections4.IteratorUtils.asEnumeration;

import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.container.api.MuleContainerClassLoaderWrapper;
import org.mule.runtime.container.internal.ContainerClassLoaderFactory;
import org.mule.runtime.container.internal.ContainerOnlyLookupStrategy;
import org.mule.runtime.container.internal.DefaultModuleRepository;
import org.mule.runtime.container.internal.JreModuleDiscoverer;
import org.mule.runtime.container.internal.MuleClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.FilteringArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.LookupStrategy;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

/**
 * Default implementation of {@link TestContainerClassLoaderAssembler}.
 */
public class DefaultTestContainerClassLoaderAssembler implements TestContainerClassLoaderAssembler {

  private final DefaultModuleRepository moduleRepository;
  private final TestPreFilteredContainerClassLoaderCreator testContainerClassLoaderCreator;
  private final ContainerClassLoaderFactory containerClassLoaderFactory;
  private final URL[] muleUrls;
  private final URL[] optUrls;

  public DefaultTestContainerClassLoaderAssembler(List<String> extraBootPackages, Set<String> extraPrivilegedArtifacts,
                                                  List<URL> muleUrls, List<URL> optUrls) {
    moduleRepository =
        new DefaultModuleRepository(new TestModuleDiscoverer(extraPrivilegedArtifacts,
                                                             new TestContainerModuleDiscoverer(ContainerClassLoaderFactory.class
                                                                 .getClassLoader())));

    this.muleUrls = muleUrls.toArray(new URL[muleUrls.size()]);
    this.optUrls = optUrls.toArray(new URL[optUrls.size()]);

    testContainerClassLoaderCreator =
        new TestPreFilteredContainerClassLoaderCreator(extraBootPackages,
                                                       this.muleUrls,
                                                       this.optUrls);
    containerClassLoaderFactory =
        new ContainerClassLoaderFactory(testContainerClassLoaderCreator, cl -> createLauncherArtifactClassLoader());
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

    ClassLoader containerOptClassLoader = new URLClassLoader(optUrls, launcherArtifact);
    final ClassLoader containerSystemClassloader = new URLClassLoader(muleUrls, containerOptClassLoader);

    return containerClassLoaderFactory.createContainerClassLoader(containerSystemClassloader);
  }

  /**
   * Creates the launcher application class loader to delegate from the container class loader.
   *
   * @return an {@link ArtifactClassLoader} for the launcher, parent of the container.
   */
  protected MuleArtifactClassLoader createLauncherArtifactClassLoader() {
    ClassLoader launcherClassLoader = this.getClass().getClassLoader();

    return new MuleArtifactClassLoader("launcher", new ArtifactDescriptor("launcher"), new URL[0], launcherClassLoader,
                                       new MuleClassLoaderLookupPolicy(emptyMap(), emptySet())) {

      private final Set<String> extraBootPackages = ImmutableSet.<String>builder()
          .addAll(testContainerClassLoaderCreator.getBootPackages())
          .addAll(new JreModuleDiscoverer().discover().get(0).getExportedPackages()).build();

      @Override
      protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (extraBootPackages.stream().anyMatch(bp -> name.startsWith(bp))) {
          return super.loadClass(name, resolve);
        } else {
          throw new ClassNotFoundException(name);
        }
      }

      @Override
      public URL getResource(String name) {
        if (extraBootPackages.stream().anyMatch(bp -> name.startsWith("META-INF/services/" + bp))) {
          return super.getResource(name);
        } else {
          return null;
        }
      }

      @Override
      public Enumeration<URL> getResources(String name) throws IOException {
        if (extraBootPackages.stream().anyMatch(bp -> name.startsWith("META-INF/services/" + bp))) {
          return super.getResources(name);
        } else {
          return emptyEnumeration();
        }
      }

      @Override
      public Enumeration<URL> findResources(String name) throws IOException {
        final Enumeration<URL> resourceUrls = super.findResources(name);

        return asEnumeration(list(resourceUrls)
            .stream()
            .filter(url -> jreResource(name, url))
            .iterator());
      }

      @Override
      public URL findResource(String name) {
        URL url = super.findResource(name);
        if (jreResource(name, url)) {
          return url;
        } else {
          return null;
        }
      }

      private boolean jreResource(String name, URL url) {
        if (url == null && getParent() != null) {
          url = getParent().getResource(name);
          // Filter if it is not a resource from the jre
          if (url != null
              && url.getFile().matches(".*?\\/jre\\/lib\\/\\w+\\.jar\\!.*")) {
            return true;
          } else {
            return false;
          }
        }
        return true;
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
