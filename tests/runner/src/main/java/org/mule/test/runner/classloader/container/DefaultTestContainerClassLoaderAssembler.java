/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.runner.classloader.container;

import static org.mule.runtime.api.util.MuleSystemProperties.RESOLVE_MULE_IMPLEMENTATIONS_LOADER_DYNAMICALLY;
import static org.mule.runtime.container.internal.PreFilteredContainerClassLoaderCreator.BOOT_PACKAGES;
import static org.mule.runtime.jpms.api.JpmsUtils.createModuleLayerClassLoader;
import static org.mule.runtime.jpms.api.MultiLevelClassLoaderFactory.MULTI_LEVEL_URL_CLASSLOADER_FACTORY;

import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyEnumeration;
import static java.util.Collections.list;
import static java.util.Collections.singletonList;

import static org.apache.commons.collections4.IteratorUtils.asEnumeration;
import static org.apache.commons.lang3.JavaVersion.JAVA_17;
import static org.apache.commons.lang3.SystemUtils.isJavaVersionAtLeast;

import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.container.api.MuleContainerClassLoaderWrapper;
import org.mule.runtime.container.internal.ContainerClassLoaderFactory;
import org.mule.runtime.container.internal.JreModuleDiscoverer;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.FilteringArtifactClassLoader;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.JavaVersion;

/**
 * Default implementation of {@link TestContainerClassLoaderAssembler}.
 */
public class DefaultTestContainerClassLoaderAssembler implements TestContainerClassLoaderAssembler {

  private ModuleRepository moduleRepository;
  private final List<String> extraBootPackages;
  private final Set<String> extraPrivilegedArtifacts;
  private final URL[] muleApisOptUrls;
  private final URL[] muleApisUrls;
  private final URL[] optUrls;
  private final URL[] muleUrls;

  public DefaultTestContainerClassLoaderAssembler(List<String> extraBootPackages, Set<String> extraPrivilegedArtifacts,
                                                  List<URL> muleApisOptUrls, List<URL> muleApisUrls, List<URL> optUrls,
                                                  List<URL> muleUrls) {
    this.extraBootPackages = extraBootPackages;
    this.extraPrivilegedArtifacts = extraPrivilegedArtifacts;
    this.muleApisOptUrls = muleApisOptUrls.toArray(new URL[muleApisOptUrls.size()]);
    this.muleApisUrls = muleApisUrls.toArray(new URL[muleApisUrls.size()]);
    this.optUrls = optUrls.toArray(new URL[optUrls.size()]);
    this.muleUrls = muleUrls.toArray(new URL[muleUrls.size()]);
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
        .addAll(BOOT_PACKAGES)
        .addAll(new JreModuleDiscoverer().discover().get(0).getExportedPackages()).build();
    ClassLoader launcherArtifact = createLauncherClassLoader(bootPackages);

    ClassLoader muleApisOptClassloader = createModuleLayerClassLoader(muleApisOptUrls, launcherArtifact);

    Class<?> muleApisOptClass = loadClass("org.apache.commons.lang3.StringUtils", muleApisOptClassloader);

    ClassLoader muleApisClassloader =
        createModuleLayerClassLoader(muleApisUrls, muleApisOptClassloader,
                                     singletonList(muleApisOptClass));

    ClassLoader optClassloaderParent = isJavaVersionAtLeast(JAVA_17) ? muleApisOptClassloader : muleApisClassloader;
    ClassLoader optClassloader =
        createModuleLayerClassLoader(optUrls, optClassloaderParent,
                                     singletonList(muleApisOptClass));

    Class<?> muleImplementationsLoaderUtilsClass =
        loadClass("org.mule.runtime.api.util.classloader.MuleImplementationLoaderUtils", muleApisClassloader);

    Class<?> optClass =
        loadClass("org.mule.maven.client.api.MavenClient", optClassloader);

    ClassLoader containerSystemClassloaderParent = isJavaVersionAtLeast(JAVA_17) ? muleApisClassloader : optClassloader;
    ClassLoader containerSystemClassloader = createModuleLayerClassLoader(muleUrls, containerSystemClassloaderParent,
                                                                          asList(muleImplementationsLoaderUtilsClass,
                                                                                 optClass));

    String originalValue = setProperty(RESOLVE_MULE_IMPLEMENTATIONS_LOADER_DYNAMICALLY, "true");
    try {
      muleImplementationsLoaderUtilsClass.getMethod("setMuleImplementationsLoader", ClassLoader.class)
          .invoke(null, containerSystemClassloader);
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      if (originalValue != null) {
        setProperty(RESOLVE_MULE_IMPLEMENTATIONS_LOADER_DYNAMICALLY, originalValue);
      } else {
        clearProperty(RESOLVE_MULE_IMPLEMENTATIONS_LOADER_DYNAMICALLY);
      }
    }

    TestPreFilteredContainerClassLoaderCreator testContainerClassLoaderCreator =
        new TestPreFilteredContainerClassLoaderCreator(containerSystemClassloader, bootPackages, extraPrivilegedArtifacts);
    this.moduleRepository = testContainerClassLoaderCreator.getModuleRepository();

    return new ContainerClassLoaderFactory(testContainerClassLoaderCreator, cl -> launcherArtifact)
        .createContainerClassLoader(containerSystemClassloader);
  }

  private Class<?> loadClass(String name, ClassLoader classLoader) {
    try {
      return classLoader.loadClass(name);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
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
