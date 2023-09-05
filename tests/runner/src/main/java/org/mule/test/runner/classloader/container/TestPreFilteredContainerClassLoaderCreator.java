/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.runner.classloader.container;

import static org.mule.runtime.container.internal.ContainerClassLoaderCreatorUtils.getLookupPolicy;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;

import static java.util.stream.Collectors.toList;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.container.internal.ClasspathModuleDiscoverer;
import org.mule.runtime.container.internal.ContainerModuleDiscoverer;
import org.mule.runtime.container.internal.DefaultModuleRepository;
import org.mule.runtime.container.internal.ModuleDiscoverer;
import org.mule.runtime.container.internal.PreFilteredContainerClassLoaderCreator;
import org.mule.runtime.jpms.api.MuleContainerModule;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

    ModuleDiscoverer moduleDiscoverer = createContainerModuleDiscoverer(containerSystemClassloader);
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

  private static ReflectionAdapterModuleDiscoverer createContainerModuleDiscoverer(ClassLoader containerSystemClassloader) {
    try {
      final Class<?> clsContainerModuleDiscoverer =
          containerSystemClassloader.loadClass(ContainerModuleDiscoverer.class.getName());

      final Object moduleDiscoverer = clsContainerModuleDiscoverer
          .getConstructor()
          .newInstance();

      final Class<?> clsClasspathModuleDiscoverer =
          containerSystemClassloader.loadClass(ClasspathModuleDiscoverer.class.getName());
      final Object classpathModuleDiscoverer = clsClasspathModuleDiscoverer
          .getConstructor(String.class)
          .newInstance(TEST_MODULE_PROPERTIES);

      final Class<?> clsModuleDiscoverer =
          containerSystemClassloader.loadClass(ModuleDiscoverer.class.getName());
      clsContainerModuleDiscoverer.getDeclaredMethod("addModuleDiscoverer", clsModuleDiscoverer)
          .invoke(moduleDiscoverer, classpathModuleDiscoverer);

      return new ReflectionAdapterModuleDiscoverer(containerSystemClassloader, moduleDiscoverer);
    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
        | NoSuchMethodException | SecurityException | ClassNotFoundException e) {
      throw new MuleRuntimeException(e);
    }
  }

  private static final class ReflectionAdapterModuleDiscoverer implements ModuleDiscoverer {

    private final ClassLoader containerSystemClassloader;
    private final Object moduleDiscoverer;
    private final Method methodDiscover;

    public ReflectionAdapterModuleDiscoverer(ClassLoader containerSystemClassloader, Object moduleDiscoverer) {
      this.containerSystemClassloader = containerSystemClassloader;
      this.moduleDiscoverer = moduleDiscoverer;

      try {
        methodDiscover = containerSystemClassloader.loadClass(ModuleDiscoverer.class.getName())
            .getDeclaredMethod("discover");
      } catch (NoSuchMethodException | SecurityException | ClassNotFoundException e) {
        throw new MuleRuntimeException(e);
      }
    }

    @Override
    public List<MuleContainerModule> discover() {
      try {
        return ((List<?>) methodDiscover.invoke(moduleDiscoverer))
            .stream()
            .map(module -> new ReflectionAdapterMuleContainerModule(containerSystemClassloader, module))
            .collect(toList());
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        throw new MuleRuntimeException(e);
      }

    }

  }

  public static final class ReflectionAdapterMuleContainerModule implements MuleContainerModule {

    private final Object adaptedModule;

    private final Method getName;
    private final Method getExportedPackages;
    private final Method getExportedPaths;
    private final Method getPrivilegedExportedPackages;
    private final Method getPrivilegedArtifacts;

    public ReflectionAdapterMuleContainerModule(ClassLoader containerSystemClassloader, Object adaptedModule) {
      this.adaptedModule = adaptedModule;

      try {
        final Class<?> clsMuleContainerModule = containerSystemClassloader.loadClass(MuleContainerModule.class.getName());

        getName = clsMuleContainerModule.getMethod("getName");
        getExportedPackages = clsMuleContainerModule.getMethod("getExportedPackages");
        getExportedPaths = clsMuleContainerModule.getMethod("getExportedPaths");
        getPrivilegedExportedPackages = clsMuleContainerModule.getMethod("getPrivilegedExportedPackages");
        getPrivilegedArtifacts = clsMuleContainerModule.getMethod("getPrivilegedArtifacts");
      } catch (NoSuchMethodException | SecurityException | ClassNotFoundException e) {
        throw new MuleRuntimeException(e);
      }
    }

    @Override
    public String getName() {
      try {
        return (String) getName.invoke(adaptedModule);
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        throw new MuleRuntimeException(e);
      }
    }

    @Override
    public Set<String> getExportedPackages() {
      try {
        return (Set<String>) getExportedPackages.invoke(adaptedModule);
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        throw new MuleRuntimeException(e);
      }
    }

    @Override
    public Set<String> getExportedPaths() {
      try {
        return (Set<String>) getExportedPaths.invoke(adaptedModule);
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        throw new MuleRuntimeException(e);
      }
    }

    @Override
    public Set<String> getPrivilegedExportedPackages() {
      try {
        return (Set<String>) getPrivilegedExportedPackages.invoke(adaptedModule);
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        throw new MuleRuntimeException(e);
      }
    }

    @Override
    public Set<String> getPrivilegedArtifacts() {
      try {
        return (Set<String>) getPrivilegedArtifacts.invoke(adaptedModule);
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        throw new MuleRuntimeException(e);
      }
    }

  }
}
