/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.runner.classloader.container;

import static org.mule.runtime.jpms.api.JpmsUtils.createModuleLayerClassLoader;
import static org.mule.runtime.jpms.api.MultiLevelClassLoaderFactory.MULTI_LEVEL_URL_CLASSLOADER_FACTORY;

import static java.util.Collections.emptyEnumeration;
import static java.util.Collections.list;

import static org.apache.commons.collections4.IteratorUtils.asEnumeration;

import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.container.api.MuleContainerClassLoaderWrapper;
import org.mule.runtime.container.internal.ContainerClassLoaderFactory;
import org.mule.runtime.container.internal.DefaultModuleRepository;
import org.mule.runtime.container.internal.JreModuleDiscoverer;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.FilteringArtifactClassLoader;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

/**
 * Default implementation of {@link TestContainerClassLoaderAssembler}.
 */
public class DefaultTestContainerClassLoaderAssembler implements TestContainerClassLoaderAssembler {

  private final DefaultModuleRepository moduleRepository;
  private final List<String> extraBootPackages;
  private final URL[] muleUrls;
  private final URL[] optUrls;

  public DefaultTestContainerClassLoaderAssembler(List<String> extraBootPackages, Set<String> extraPrivilegedArtifacts,
                                                  List<URL> muleUrls, List<URL> optUrls) {
    moduleRepository =
        new DefaultModuleRepository(new TestModuleDiscoverer(extraPrivilegedArtifacts,
                                                             new TestContainerModuleDiscoverer(ContainerClassLoaderFactory.class
                                                                 .getClassLoader())));

    this.extraBootPackages = extraBootPackages;
    this.muleUrls = muleUrls.toArray(new URL[muleUrls.size()]);
    this.optUrls = optUrls.toArray(new URL[optUrls.size()]);
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
    final Set<String> bootPackages = ImmutableSet.<String>builder()
        .addAll(extraBootPackages)
        .addAll(new JreModuleDiscoverer().discover().get(0).getExportedPackages()).build();
    ClassLoader launcherArtifact = createLauncherClassLoader(bootPackages);

    ClassLoader containerSystemClassloader = createModuleLayerClassLoader(optUrls, muleUrls,
                                                                          MULTI_LEVEL_URL_CLASSLOADER_FACTORY,
                                                                          launcherArtifact);

    TestPreFilteredContainerClassLoaderCreator testContainerClassLoaderCreator =
        new TestPreFilteredContainerClassLoaderCreator(containerSystemClassloader, bootPackages);

    return new ContainerClassLoaderFactory(testContainerClassLoaderCreator, cl -> launcherArtifact)
        .createContainerClassLoader(containerSystemClassloader);
  }

  /**
   * Creates the launcher application class loader to delegate from the container class loader.
   *
   * @return an {@link ArtifactClassLoader} for the launcher, parent of the container.
   */
  protected ClassLoader createLauncherClassLoader(Set<String> extraBootPackages) {
    return new TestLauncherClassLoader(this.getClass().getClassLoader(), extraBootPackages);
  }

  @Override
  public ModuleRepository getModuleRepository() {
    return moduleRepository;
  }

  private static final class TestLauncherClassLoader extends ClassLoader {

    static {
      registerAsParallelCapable();
    }

    private final Set<String> extraBootPackages;

    private TestLauncherClassLoader(ClassLoader parent, Set<String> extraBootPackages) {
      super(parent);
      this.extraBootPackages = extraBootPackages;
    }

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
  }

}
